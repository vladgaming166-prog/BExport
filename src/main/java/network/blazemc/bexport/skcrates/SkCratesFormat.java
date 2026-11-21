package network.blazemc.bexport.skcrates;

import java.util.List;

/**
 * What BExport actually observed in an installed SK Crates data folder, or UNKNOWN if the plugin is absent.
 * Keys are never invented: {@link #itemNodeKey()} is only set when a live crate file contained that node.
 */
public record SkCratesFormat(
    boolean pluginPresent,
    String pluginName,
    String pluginVersion,
    Kind kind,
    String itemNodeKey,
    String base64Key,
    String sampleFile,
    List<String> notes
) {
    public enum Kind {
        NOT_INSTALLED,
        BUKKIT_ITEMSTACK,
        BASE64_KEY,
        UNKNOWN
    }

    public static SkCratesFormat notInstalled() {
        return new SkCratesFormat(
            false,
            null,
            null,
            Kind.NOT_INSTALLED,
            null,
            null,
            null,
            List.of(
                "SK Crates was not found among loaded plugins.",
                "Public SK Crates documentation (BuiltByBit resource 123298) states the plugin is built against the Spigot API and that rewards are added in-game with /crates edit.",
                "BExport therefore does not invent a Base64 config key. Use /crates edit while holding the ItemsAdder item, or paste the bukkit-itemstack node if your crate YAML already stores org.bukkit.inventory.ItemStack."
            )
        );
    }

    public String label() {
        if (!pluginPresent) {
            return "not-installed";
        }
        return kind.name().toLowerCase().replace('_', '-');
    }
}
