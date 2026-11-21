package network.blazemc.bexport.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ItemInspector {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public Map<String, Object> describe(ItemStack item) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("material", item.getType().name());
        info.put("amount", item.getAmount());
        info.put("max-stack-size", item.getMaxStackSize());
        info.put("translation-key", item.getType().getItemTranslationKey());

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            info.put("has-meta", false);
            return info;
        }
        info.put("has-meta", true);

        if (meta.hasDisplayName() && meta.displayName() != null) {
            info.put("display-name", MINI_MESSAGE.serialize(meta.displayName()));
            info.put("display-name-plain", PLAIN.serialize(meta.displayName()));
        }
        if (meta.hasItemName() && meta.itemName() != null) {
            info.put("item-name", MINI_MESSAGE.serialize(meta.itemName()));
        }
        if (meta.hasLore() && meta.lore() != null) {
            List<String> lore = new ArrayList<>();
            for (Component line : meta.lore()) {
                lore.add(MINI_MESSAGE.serialize(line));
            }
            info.put("lore", lore);
        }

        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            enchantments.put(entry.getKey().getKey().toString(), entry.getValue());
        }
        if (!enchantments.isEmpty()) {
            info.put("enchantments", enchantments);
        }

        List<String> flags = new ArrayList<>();
        for (ItemFlag flag : meta.getItemFlags()) {
            flags.add(flag.name());
        }
        if (!flags.isEmpty()) {
            info.put("item-flags", flags);
        }
        info.put("unbreakable", meta.isUnbreakable());
        if (meta.hasEnchantmentGlintOverride()) {
            info.put("enchantment-glint-override", meta.getEnchantmentGlintOverride());
        }
        if (meta.hasRarity()) {
            info.put("rarity", meta.getRarity().name());
        }
        if (meta.hasItemModel() && meta.getItemModel() != null) {
            info.put("item-model", meta.getItemModel().toString());
        }
        if (meta.hasEquippable() && meta.getEquippable() != null && meta.getEquippable().getSlot() != null) {
            info.put("equippable-slot", meta.getEquippable().getSlot().name());
        }
        putCustomModelData(meta, info);

        if (meta instanceof Damageable damageable) {
            info.put("damage", damageable.getDamage());
            if (damageable.hasMaxDamage()) {
                info.put("max-damage", damageable.getMaxDamage());
            }
        }

        if (meta.hasAttributeModifiers() && meta.getAttributeModifiers() != null) {
            List<Map<String, Object>> modifiers = new ArrayList<>();
            for (Map.Entry<Attribute, AttributeModifier> entry : meta.getAttributeModifiers().entries()) {
                AttributeModifier modifier = entry.getValue();
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attribute", entry.getKey().getKey().toString());
                row.put("key", modifier.getKey().toString());
                row.put("amount", modifier.getAmount());
                row.put("operation", modifier.getOperation().name());
                if (modifier.getSlotGroup() != null) {
                    row.put("slot-group", modifier.getSlotGroup().toString());
                }
                modifiers.add(row);
            }
            if (!modifiers.isEmpty()) {
                info.put("attribute-modifiers", modifiers);
            }
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        List<Map<String, Object>> pdc = new ArrayList<>();
        for (org.bukkit.NamespacedKey key : container.getKeys()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", key.toString());
            Object value = readPdc(container, key);
            if (value != null) {
                row.put("value", value);
            }
            pdc.add(row);
        }
        if (!pdc.isEmpty()) {
            info.put("persistent-data", pdc);
        }
        return info;
    }

    private static void putCustomModelData(ItemMeta meta, Map<String, Object> info) {
        try {
            if (!meta.hasCustomModelDataComponent()) {
                return;
            }
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            Map<String, Object> cmd = new LinkedHashMap<>();
            if (!component.getFloats().isEmpty()) {
                cmd.put("floats", List.copyOf(component.getFloats()));
            }
            if (!component.getFlags().isEmpty()) {
                cmd.put("flags", List.copyOf(component.getFlags()));
            }
            if (!component.getStrings().isEmpty()) {
                cmd.put("strings", List.copyOf(component.getStrings()));
            }
            if (!cmd.isEmpty()) {
                info.put("custom-model-data", cmd);
            }
        } catch (NoSuchMethodError | Exception ignored) {
            try {
                if (meta.hasCustomModelData()) {
                    info.put("custom-model-data-legacy", meta.getCustomModelData());
                }
            } catch (NoSuchMethodError ignoredLegacy) {
                // Paper 26.x may only expose the component form.
            }
        }
    }

    private static Object readPdc(PersistentDataContainer container, org.bukkit.NamespacedKey key) {
        if (container.has(key, PersistentDataType.STRING)) {
            return container.get(key, PersistentDataType.STRING);
        }
        if (container.has(key, PersistentDataType.INTEGER)) {
            return container.get(key, PersistentDataType.INTEGER);
        }
        if (container.has(key, PersistentDataType.LONG)) {
            return container.get(key, PersistentDataType.LONG);
        }
        if (container.has(key, PersistentDataType.DOUBLE)) {
            return container.get(key, PersistentDataType.DOUBLE);
        }
        if (container.has(key, PersistentDataType.FLOAT)) {
            return container.get(key, PersistentDataType.FLOAT);
        }
        if (container.has(key, PersistentDataType.BYTE)) {
            return container.get(key, PersistentDataType.BYTE);
        }
        if (container.has(key, PersistentDataType.BOOLEAN)) {
            return container.get(key, PersistentDataType.BOOLEAN);
        }
        if (container.has(key, PersistentDataType.BYTE_ARRAY)) {
            return "byte[" + container.get(key, PersistentDataType.BYTE_ARRAY).length + "]";
        }
        return "(complex)";
    }
}
