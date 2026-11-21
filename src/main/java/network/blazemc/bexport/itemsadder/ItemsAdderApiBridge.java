package network.blazemc.bexport.itemsadder;

import dev.lone.itemsadder.api.CustomStack;
import java.util.Optional;
import java.util.Set;
import org.bukkit.inventory.ItemStack;

/**
 * Isolated so {@link NoClassDefFoundError} is thrown only if this class is actually used
 * while ItemsAdder is absent from the classpath.
 */
final class ItemsAdderApiBridge {

    private ItemsAdderApiBridge() {
    }

    static Optional<ItemsAdderIdentity> identify(ItemStack item) {
        CustomStack stack = CustomStack.byItemStack(item);
        if (stack == null) {
            return Optional.empty();
        }
        Integer durability = null;
        Integer maxDurability = null;
        boolean customDurability = false;
        try {
            customDurability = stack.hasCustomDurability();
            if (customDurability) {
                durability = stack.getDurability();
                maxDurability = stack.getMaxDurability();
            }
        } catch (Exception ignored) {
            // ItemsAdder optional durability API
        }
        String displayName = null;
        try {
            displayName = stack.getDisplayName();
        } catch (Exception ignored) {
            // display name is informational
        }
        boolean block = false;
        try {
            block = stack.isBlock();
        } catch (Exception ignored) {
            // block flag is informational
        }
        return Optional.of(new ItemsAdderIdentity(
            stack.getNamespacedID(),
            stack.getNamespace(),
            stack.getId(),
            displayName,
            block,
            customDurability,
            durability,
            maxDurability,
            "itemsadder-api"
        ));
    }

    static Set<String> registeredIds() {
        Set<String> ids = CustomStack.getNamespacedIdsInRegistry();
        return ids == null ? Set.of() : Set.copyOf(ids);
    }
}
