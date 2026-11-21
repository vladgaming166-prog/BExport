package network.blazemc.bexport.itemsadder;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads ItemsAdder identity without requiring ItemsAdder at compile/runtime beyond its official API.
 * CustomStack is only referenced from a dedicated method so a missing ItemsAdder jar cannot break class loading.
 */
public final class ItemsAdderAccess {

    public static final String PLUGIN_NAME = "ItemsAdder";

    private final JavaPlugin plugin;

    public ItemsAdderAccess(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        return itemsAdder != null && itemsAdder.isEnabled();
    }

    public void logStatus() {
        if (isAvailable()) {
            Plugin itemsAdder = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            String version = itemsAdder == null ? "unknown" : itemsAdder.getPluginMeta().getVersion();
            plugin.getLogger().info("ItemsAdder detected (" + version + "). Custom item IDs will be read from the official API.");
        } else {
            plugin.getLogger().warning("ItemsAdder is not loaded. Exports still work, but namespaced IDs will only come from leftover PDC keys.");
        }
    }

    public Optional<ItemsAdderIdentity> identify(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        if (isAvailable()) {
            Optional<ItemsAdderIdentity> fromApi = identifyFromApi(item);
            if (fromApi.isPresent()) {
                return fromApi;
            }
        }
        return identifyFromPersistentData(item);
    }

    private Optional<ItemsAdderIdentity> identifyFromApi(ItemStack item) {
        try {
            return ItemsAdderApiBridge.identify(item);
        } catch (NoClassDefFoundError | Exception exception) {
            plugin.getLogger().warning("ItemsAdder API lookup failed: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ItemsAdderIdentity> identifyFromPersistentData(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String namespaced = firstString(container, "itemsadder", "id", "namespaced_id", "namespaced-id", "namespacedid");
        String namespace = firstString(container, "itemsadder", "namespace", "namespace_id");
        String id = firstString(container, "itemsadder", "name", "item_id", "item-id");

        if (namespaced != null && namespaced.contains(":")) {
            return Optional.of(ItemsAdderIdentity.of(namespaced, "persistent-data"));
        }
        if (namespace != null && id != null) {
            return Optional.of(ItemsAdderIdentity.of(namespace + ":" + id, "persistent-data"));
        }
        for (NamespacedKey key : container.getKeys()) {
            if (!"itemsadder".equalsIgnoreCase(key.getNamespace())) {
                continue;
            }
            String value = container.get(key, PersistentDataType.STRING);
            if (value != null && value.contains(":") && key.getKey().toLowerCase(Locale.ROOT).contains("id")) {
                return Optional.of(ItemsAdderIdentity.of(value, "persistent-data:" + key));
            }
        }
        return Optional.empty();
    }

    private static String firstString(PersistentDataContainer container, String namespace, String... keys) {
        for (String key : keys) {
            NamespacedKey namespacedKey = new NamespacedKey(namespace, key);
            String value = container.get(namespacedKey, PersistentDataType.STRING);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public Set<String> registeredIds() {
        if (!isAvailable()) {
            return Set.of();
        }
        try {
            return ItemsAdderApiBridge.registeredIds();
        } catch (NoClassDefFoundError | Exception exception) {
            return Set.of();
        }
    }
}
