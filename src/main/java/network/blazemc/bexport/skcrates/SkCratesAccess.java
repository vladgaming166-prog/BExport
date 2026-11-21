package network.blazemc.bexport.skcrates;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Inspects a running SK Crates plugin instead of assuming a reward schema.
 * SK Crates is closed-source; BExport only reports keys that exist in its data files.
 */
public final class SkCratesAccess {

    private final JavaPlugin plugin;
    private SkCratesFormat cached = SkCratesFormat.notInstalled();

    public SkCratesAccess(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public SkCratesFormat inspectAndLog() {
        cached = inspect();
        if (!cached.pluginPresent()) {
            plugin.getLogger().info("SK Crates is not installed. Exports include the verified /crates edit workflow, not a guessed Base64 key.");
            return cached;
        }
        plugin.getLogger().info("SK Crates detected: " + cached.pluginName() + " v" + cached.pluginVersion()
            + " format=" + cached.kind()
            + (cached.itemNodeKey() != null ? " item-node=" + cached.itemNodeKey() : "")
            + (cached.base64Key() != null ? " base64-key=" + cached.base64Key() : "")
            + (cached.sampleFile() != null ? " sample=" + cached.sampleFile() : ""));
        for (String note : cached.notes()) {
            plugin.getLogger().info("SK Crates: " + note);
        }
        return cached;
    }

    public SkCratesFormat current() {
        return cached;
    }

    public SkCratesFormat inspect() {
        Optional<Plugin> found = findPlugin();
        if (found.isEmpty()) {
            return SkCratesFormat.notInstalled();
        }
        Plugin sk = found.get();
        File dataFolder = sk.getDataFolder();
        List<String> notes = new ArrayList<>();
        notes.add("Plugin name from plugin.yml: " + sk.getName() + " v" + sk.getPluginMeta().getVersion() + ".");
        notes.add("SK Crates public docs: crate rewards are edited in-game with /crates edit and stored in per-crate YAML files.");

        if (!dataFolder.isDirectory()) {
            notes.add("Data folder does not exist yet, so no reward keys could be read. Use /crates edit to add the exported item.");
            return new SkCratesFormat(true, sk.getName(), sk.getPluginMeta().getVersion(), SkCratesFormat.Kind.UNKNOWN, null, null, null, List.copyOf(notes));
        }

        List<File> yamlFiles = new ArrayList<>();
        collectYaml(dataFolder, yamlFiles, 0);
        String itemNodeKey = null;
        String base64Key = null;
        String sampleFile = null;
        boolean sawItemStack = false;

        for (File file : yamlFiles) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ScanResult scan = new ScanResult();
            walk(yaml, "", scan);
            if (scan.itemNodeKey != null) {
                sawItemStack = true;
                itemNodeKey = scan.itemNodeKey;
                sampleFile = relative(dataFolder, file);
            }
            if (scan.base64Key != null && base64Key == null) {
                base64Key = scan.base64Key;
                if (sampleFile == null) {
                    sampleFile = relative(dataFolder, file);
                }
            }
        }

        SkCratesFormat.Kind kind;
        if (sawItemStack) {
            kind = SkCratesFormat.Kind.BUKKIT_ITEMSTACK;
            notes.add("Observed Bukkit ItemStack nodes in " + sampleFile + " under '" + itemNodeKey + "'.");
            notes.add("BExport writes that same ConfigurationSerializable YAML. Paste the node into the matching crate reward, then run /crates reload.");
        } else if (base64Key != null) {
            kind = SkCratesFormat.Kind.BASE64_KEY;
            notes.add("Observed a Base64-looking item key named '" + base64Key + "' in " + sampleFile + ".");
            notes.add("BExport copies Paper serializeAsBytes() Base64 into that exact key name. No other key names are invented.");
        } else {
            kind = SkCratesFormat.Kind.UNKNOWN;
            notes.add("No ItemStack nodes or Base64 item keys were found in " + yamlFiles.size() + " YAML file(s).");
            notes.add("The verified way to add an ItemsAdder item is still /crates edit while holding the item. BExport will not invent reward keys.");
        }
        return new SkCratesFormat(
            true,
            sk.getName(),
            sk.getPluginMeta().getVersion(),
            kind,
            itemNodeKey,
            base64Key,
            sampleFile,
            List.copyOf(notes)
        );
    }

    public Optional<Plugin> findPlugin() {
        Plugin exact = plugin.getServer().getPluginManager().getPlugin("SKCrates");
        if (exact != null && exact.isEnabled()) {
            return Optional.of(exact);
        }
        for (Plugin candidate : plugin.getServer().getPluginManager().getPlugins()) {
            String compact = candidate.getName().replace(" ", "").replace("-", "").replace("_", "");
            if (compact.equalsIgnoreCase("skcrates") && candidate.isEnabled()) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static void collectYaml(File directory, List<File> out, int depth) {
        if (depth > 4 || directory == null) {
            return;
        }
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectYaml(child, out, depth + 1);
            } else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml") || child.getName().toLowerCase(Locale.ROOT).endsWith(".yaml")) {
                out.add(child);
            }
        }
    }

    private static void walk(ConfigurationSection section, String path, ScanResult scan) {
        for (String key : section.getKeys(false)) {
            String childPath = path.isEmpty() ? key : path + "." + key;
            Object value = section.get(key);
            if (value instanceof ItemStack) {
                if (scan.itemNodeKey == null) {
                    scan.itemNodeKey = key;
                }
            } else if (value instanceof String stringValue) {
                if (looksLikeItemBase64Key(key) && looksLikeBase64(stringValue) && scan.base64Key == null) {
                    scan.base64Key = key;
                }
            } else if (value instanceof ConfigurationSection child) {
                walk(child, childPath, scan);
            } else if (value instanceof List<?> list) {
                for (Object element : list) {
                    if (element instanceof ItemStack && scan.itemNodeKey == null) {
                        scan.itemNodeKey = key;
                    } else if (element instanceof ConfigurationSection nested) {
                        walk(nested, childPath, scan);
                    } else if (element instanceof Map<?, ?> map) {
                        Object discriminator = map.get("==");
                        if (discriminator != null && discriminator.toString().contains("ItemStack") && scan.itemNodeKey == null) {
                            scan.itemNodeKey = key;
                        }
                    }
                }
            }
        }
    }

    private static boolean looksLikeItemBase64Key(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("base64")
            || lower.equals("nbt")
            || lower.equals("bytes")
            || lower.contains("serialized")
            || lower.contains("itemstack")
            || lower.equals("item-data")
            || lower.equals("item_data");
    }

    private static boolean looksLikeBase64(String value) {
        if (value.length() < 32) {
            return false;
        }
        return value.matches("^[A-Za-z0-9+/=\\r\\n]+$");
    }

    private static String relative(File root, File file) {
        try {
            return root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
        } catch (IllegalArgumentException exception) {
            return file.getName();
        }
    }

    private static final class ScanResult {
        private String itemNodeKey;
        private String base64Key;
    }
}
