package network.blazemc.bexport.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import network.blazemc.bexport.itemsadder.ItemsAdderIdentity;
import network.blazemc.bexport.skcrates.SkCratesFormat;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public final class ExportWriter {

    public Path writeItem(
        Path exportsDir,
        String name,
        String playerName,
        ItemStack item,
        String base64,
        Map<String, Object> info,
        ItemsAdderIdentity itemsAdder,
        SkCratesFormat skCrates,
        boolean validated
    ) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(header("single item", name));
        yaml.set("name", name);
        yaml.set("type", "SINGLE_ITEM");
        yaml.set("exported-at", Instant.now().toString());
        yaml.set("exported-by", playerName);
        putItemsAdder(yaml, "itemsadder", itemsAdder);
        yaml.set("item-info", info);
        yaml.set("paper-bytes.api", "org.bukkit.inventory.ItemStack#serializeAsBytes()");
        yaml.set("paper-bytes.encoding", "java.util.Base64");
        yaml.set("paper-bytes.validated", validated);
        yaml.set("paper-bytes.sha256", sha256(base64));
        yaml.set("paper-bytes.base64", base64);
        yaml.set("bukkit-itemstack", item.clone());
        putSkCrates(yaml, skCrates, itemsAdder, "paper-bytes.base64");
        Path yamlPath = exportsDir.resolve(name + ".yml");
        atomicWrite(yamlPath, yaml.saveToString());
        atomicWrite(exportsDir.resolve(name + ".txt"), itemTextFile(name, itemsAdder, info, base64, yamlPath));
        atomicWrite(exportsDir.resolve(name + ".skcrates.yml"), skCratesSnippet(name, item, base64, itemsAdder, skCrates, false));
        return yamlPath;
    }

    public Path writeArmor(
        Path exportsDir,
        String name,
        String playerName,
        Map<String, ItemStack> pieces,
        Map<String, String> pieceBase64,
        Map<String, Map<String, Object>> pieceInfo,
        Map<String, ItemsAdderIdentity> pieceIds,
        String setBase64,
        SkCratesFormat skCrates,
        boolean validated
    ) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(header("armor set", name));
        yaml.set("name", name);
        yaml.set("type", "ARMOR_SET");
        yaml.set("exported-at", Instant.now().toString());
        yaml.set("exported-by", playerName);
        yaml.set("paper-bytes-set.api", "org.bukkit.inventory.ItemStack#serializeItemsAsBytes()");
        yaml.set("paper-bytes-set.encoding", "java.util.Base64");
        yaml.set("paper-bytes-set.slot-order", List.of("helmet", "chestplate", "leggings", "boots"));
        yaml.set("paper-bytes-set.validated", validated);
        yaml.set("paper-bytes-set.base64", setBase64);

        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            String prefix = "pieces." + slot;
            ItemStack piece = pieces.get(slot);
            if (piece == null || piece.getType().isAir()) {
                yaml.set(prefix + ".present", false);
                continue;
            }
            yaml.set(prefix + ".present", true);
            putItemsAdder(yaml, prefix + ".itemsadder", pieceIds.get(slot));
            yaml.set(prefix + ".item-info", pieceInfo.get(slot));
            yaml.set(prefix + ".paper-bytes.api", "org.bukkit.inventory.ItemStack#serializeAsBytes()");
            yaml.set(prefix + ".paper-bytes.encoding", "java.util.Base64");
            yaml.set(prefix + ".paper-bytes.validated", validated);
            yaml.set(prefix + ".paper-bytes.sha256", sha256(pieceBase64.get(slot)));
            yaml.set(prefix + ".paper-bytes.base64", pieceBase64.get(slot));
            yaml.set(prefix + ".bukkit-itemstack", piece.clone());
        }
        putSkCrates(yaml, skCrates, firstIdentity(pieceIds), "pieces.<slot>.paper-bytes.base64");

        Path yamlPath = exportsDir.resolve(name + ".yml");
        atomicWrite(yamlPath, yaml.saveToString());
        atomicWrite(exportsDir.resolve(name + ".txt"), armorTextFile(name, pieceBase64, pieceIds, yamlPath));
        atomicWrite(exportsDir.resolve(name + ".skcrates.yml"), armorSkCratesSnippet(name, pieces, pieceBase64, pieceIds, skCrates));
        return yamlPath;
    }

    public void updateIndex(Path indexFile, String name, String type, Path yamlFile, ItemsAdderIdentity itemsAdder, String material) throws IOException {
        YamlConfiguration index = Files.isRegularFile(indexFile)
            ? YamlConfiguration.loadConfiguration(indexFile.toFile())
            : new YamlConfiguration();
        String path = "exports." + name;
        index.set(path + ".type", type);
        index.set(path + ".file", "exports/" + yamlFile.getFileName());
        index.set(path + ".material", material);
        index.set(path + ".exported-at", Instant.now().toString());
        if (itemsAdder != null) {
            index.set(path + ".itemsadder-id", itemsAdder.namespacedId());
        } else {
            index.set(path + ".itemsadder-id", null);
        }
        atomicWrite(indexFile, index.saveToString());
    }

    public List<String> listedExports(Path indexFile) {
        if (!Files.isRegularFile(indexFile)) {
            return List.of();
        }
        YamlConfiguration index = YamlConfiguration.loadConfiguration(indexFile.toFile());
        var section = index.getConfigurationSection("exports");
        if (section == null) {
            return List.of();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    private static void putItemsAdder(YamlConfiguration yaml, String path, ItemsAdderIdentity identity) {
        if (identity == null) {
            yaml.set(path + ".present", false);
            return;
        }
        yaml.set(path + ".present", true);
        yaml.set(path + ".namespaced-id", identity.namespacedId());
        yaml.set(path + ".namespace", identity.namespace());
        yaml.set(path + ".id", identity.id());
        yaml.set(path + ".source", identity.source());
        yaml.set(path + ".block", identity.block());
        if (identity.displayName() != null) {
            yaml.set(path + ".display-name", identity.displayName());
        }
        yaml.set(path + ".custom-durability", identity.customDurability());
        if (identity.durability() != null) {
            yaml.set(path + ".durability", identity.durability());
        }
        if (identity.maxDurability() != null) {
            yaml.set(path + ".max-durability", identity.maxDurability());
        }
    }

    private static void putSkCrates(YamlConfiguration yaml, SkCratesFormat format, ItemsAdderIdentity identity, String base64Pointer) {
        yaml.set("skcrates.plugin-present", format.pluginPresent());
        yaml.set("skcrates.plugin-name", format.pluginName());
        yaml.set("skcrates.plugin-version", format.pluginVersion());
        yaml.set("skcrates.detected-format", format.label());
        yaml.set("skcrates.detected-item-node-key", format.itemNodeKey());
        yaml.set("skcrates.detected-base64-key", format.base64Key());
        yaml.set("skcrates.sample-file", format.sampleFile());
        yaml.set("skcrates.notes", format.notes());
        yaml.set("skcrates.verified-ingame-command", "/crates edit");
        yaml.set("skcrates.paper-base64-pointer", base64Pointer);
        if (identity != null) {
            yaml.set("skcrates.itemsadder-give-command", "iagive %player% " + identity.namespacedId() + " 1");
        }
        yaml.set("skcrates.do-not-invent-keys", true);
    }

    private static String skCratesSnippet(
        String name,
        ItemStack item,
        String base64,
        ItemsAdderIdentity identity,
        SkCratesFormat format,
        boolean armor
    ) {
        StringBuilder out = new StringBuilder();
        out.append("# SK Crates snippet generated by BExport\n");
        out.append("# Export name: ").append(name).append('\n');
        out.append("# This file never invents SK Crates keys.\n");
        out.append("#\n");
        out.append("# Verified SK Crates workflow (public plugin description, BuiltByBit 123298):\n");
        out.append("#   1. Obtain the ItemsAdder item in-game (hold the original, or /iagive).\n");
        out.append("#   2. Run /crates edit\n");
        out.append("#   3. Add the item as a crate reward in the GUI and save.\n");
        out.append("# SK Crates is built against the Spigot API, so it does not use Paper serializeAsBytes().\n");
        out.append("#\n");
        if (format.kind() == SkCratesFormat.Kind.BUKKIT_ITEMSTACK && format.itemNodeKey() != null) {
            out.append("# Detected live SK Crates files store Bukkit ItemStacks under '").append(format.itemNodeKey()).append("'.\n");
            out.append("# Paste the node below into that reward entry, then /crates reload.\n");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(format.itemNodeKey(), item.clone());
            out.append(yaml.saveToString());
        } else if (format.kind() == SkCratesFormat.Kind.BASE64_KEY && format.base64Key() != null) {
            out.append("# Detected live SK Crates files store Base64 item data under '").append(format.base64Key()).append("'.\n");
            out.append("# That key name was read from ").append(format.sampleFile()).append(".\n");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set(format.base64Key(), base64);
            out.append(yaml.saveToString());
        } else {
            out.append("# No SK Crates reward schema was observed on this server.\n");
            out.append("# Do not paste paper-bytes.base64 into SK Crates unless a live crate file already has a Base64 item key.\n");
            out.append("# The Spigot-compatible ItemStack dump is included for servers whose crate YAML already uses:\n");
            out.append("#   ==: org.bukkit.inventory.ItemStack\n");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("bukkit-itemstack", item.clone());
            if (identity != null) {
                yaml.set("itemsadder-namespaced-id", identity.namespacedId());
                yaml.set("itemsadder-give-command", "iagive %player% " + identity.namespacedId() + " 1");
            }
            out.append(yaml.saveToString());
        }
        if (identity != null) {
            out.append("#\n# ItemsAdder restore command (not an SK Crates key, just the official give command):\n");
            out.append("# iagive %player% ").append(identity.namespacedId()).append(" 1\n");
        }
        if (armor) {
            out.append("# This snippet is one armor piece. See the parent armor export for all four slots.\n");
        }
        return out.toString();
    }

    private static String armorSkCratesSnippet(
        String name,
        Map<String, ItemStack> pieces,
        Map<String, String> pieceBase64,
        Map<String, ItemsAdderIdentity> pieceIds,
        SkCratesFormat format
    ) {
        StringBuilder out = new StringBuilder();
        out.append("# SK Crates armor-set snippet generated by BExport\n");
        out.append("# Export name: ").append(name).append('\n');
        out.append("# Add each non-empty piece as its own crate reward with /crates edit.\n");
        out.append("# SK Crates does not document a bundled armor-set reward type.\n\n");
        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            ItemStack piece = pieces.get(slot);
            if (piece == null || piece.getType().isAir()) {
                continue;
            }
            out.append("# --- ").append(slot).append(" ---\n");
            out.append(skCratesSnippet(name + "_" + slot, piece, pieceBase64.get(slot), pieceIds.get(slot), format, true));
            out.append('\n');
        }
        return out.toString();
    }

    private static String itemTextFile(String name, ItemsAdderIdentity identity, Map<String, Object> info, String base64, Path yamlPath) {
        StringBuilder out = new StringBuilder();
        out.append("# BExport paste-ready Base64\n");
        out.append("# name: ").append(name).append('\n');
        out.append("# yaml: ").append(yamlPath.toAbsolutePath()).append('\n');
        if (identity != null) {
            out.append("# itemsadder: ").append(identity.namespacedId()).append('\n');
        }
        if (info.get("material") != null) {
            out.append("# material: ").append(info.get("material")).append('\n');
        }
        if (info.get("display-name-plain") != null) {
            out.append("# display-name: ").append(info.get("display-name-plain")).append('\n');
        }
        out.append("# encoding: Paper ItemStack.serializeAsBytes() + java.util.Base64\n");
        out.append('\n');
        out.append(base64).append('\n');
        return out.toString();
    }

    private static String armorTextFile(String name, Map<String, String> pieceBase64, Map<String, ItemsAdderIdentity> pieceIds, Path yamlPath) {
        StringBuilder out = new StringBuilder();
        out.append("# BExport paste-ready Base64 (armor set)\n");
        out.append("# name: ").append(name).append('\n');
        out.append("# yaml: ").append(yamlPath.toAbsolutePath()).append('\n');
        out.append("# encoding: Paper ItemStack.serializeAsBytes() + java.util.Base64\n");
        out.append('\n');
        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            String base64 = pieceBase64.get(slot);
            out.append("# ").append(slot);
            ItemsAdderIdentity identity = pieceIds.get(slot);
            if (identity != null) {
                out.append(" itemsadder=").append(identity.namespacedId());
            }
            out.append('\n');
            if (base64 == null) {
                out.append("# (empty slot)\n\n");
            } else {
                out.append(base64).append("\n\n");
            }
        }
        return out.toString();
    }

    private static List<String> header(String kind, String name) {
        return List.of(
            "BExport " + kind + " export: " + name,
            "Generated by BExport (BlazeMC Network).",
            "paper-bytes.base64 is Paper ItemStack.serializeAsBytes() encoded with java.util.Base64.",
            "bukkit-itemstack is Bukkit ConfigurationSerializable YAML (YamlConfiguration.getItemStack compatible).",
            "SK Crates does not automatically understand Base64. See skcrates notes and the .skcrates.yml sibling file."
        );
    }

    private static ItemsAdderIdentity firstIdentity(Map<String, ItemsAdderIdentity> pieceIds) {
        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            if (pieceIds.get(slot) != null) {
                return pieceIds.get(slot);
            }
        }
        return null;
    }

    static void atomicWrite(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            return "";
        }
    }
}
