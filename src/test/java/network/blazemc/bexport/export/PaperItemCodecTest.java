package network.blazemc.bexport.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class PaperItemCodecTest {

    private ServerMock server;
    private PaperItemCodec codec;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        codec = new PaperItemCodec();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void roundTripPreservesCustomWeapon() {
        ItemStack original = customWeapon();
        String base64 = codec.encode(original);
        assertFalse(base64.isBlank());
        ItemStack restored = codec.decode(base64);
        assertEquals(Material.NETHERITE_SWORD, restored.getType());
        assertEquals(1, restored.getAmount());
        ItemMeta meta = restored.getItemMeta();
        assertNotNull(meta);
        assertEquals("ruby:legendary_sword", meta.getPersistentDataContainer()
            .get(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING));
        assertTrue(meta.hasEnchant(Enchantment.SHARPNESS));
        PaperItemCodec.ValidationResult validation = codec.validate(original, base64);
        assertTrue(validation.success(), validation.reason());
    }

    @Test
    void roundTripPreservesCustomArmor() {
        ItemStack original = customArmor(Material.NETHERITE_CHESTPLATE, "ruby:ruby_chestplate", "Ruby Chestplate");
        String base64 = codec.encode(original);
        ItemStack restored = codec.decode(base64);
        assertEquals(Material.NETHERITE_CHESTPLATE, restored.getType());
        assertEquals("ruby:ruby_chestplate", restored.getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING));
        assertTrue(codec.validate(original, base64).success());
    }

    @Test
    void armorSetBytesRoundTripKeepsSlotOrder() {
        ItemStack helmet = customArmor(Material.NETHERITE_HELMET, "ruby:ruby_helmet", "Ruby Helmet");
        ItemStack chest = customArmor(Material.NETHERITE_CHESTPLATE, "ruby:ruby_chestplate", "Ruby Chestplate");
        ItemStack legs = customArmor(Material.NETHERITE_LEGGINGS, "ruby:ruby_leggings", "Ruby Leggings");
        ItemStack boots = customArmor(Material.NETHERITE_BOOTS, "ruby:ruby_boots", "Ruby Boots");
        String encoded = codec.encodeSet(new ItemStack[]{helmet, chest, legs, boots});
        ItemStack[] restored = codec.decodeSet(encoded);
        assertEquals(4, restored.length);
        assertEquals(Material.NETHERITE_HELMET, restored[0].getType());
        assertEquals(Material.NETHERITE_CHESTPLATE, restored[1].getType());
        assertEquals(Material.NETHERITE_LEGGINGS, restored[2].getType());
        assertEquals(Material.NETHERITE_BOOTS, restored[3].getType());
        assertEquals("ruby:ruby_helmet", restored[0].getItemMeta().getPersistentDataContainer()
            .get(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING));
    }

    @Test
    void foodAndBlockItemsRoundTrip() {
        ItemStack food = named(Material.COOKED_BEEF, "ruby:ruby_steak", "Ruby Steak");
        ItemStack block = named(Material.PAPER, "ruby:ruby_ore", "Ruby Ore");
        assertTrue(codec.validate(food, codec.encode(food)).success());
        assertTrue(codec.validate(block, codec.encode(block)).success());
    }

    private static ItemStack customWeapon() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Legendary Ruby Sword"));
        meta.lore(List.of(Component.text("Forged for crate rewards")));
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(3);
        }
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, "ruby:legendary_sword");
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "namespace"), PersistentDataType.STRING, "ruby");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack customArmor(Material material, String namespacedId, String name) {
        ItemStack item = named(material, namespacedId, name);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack named(Material material, String namespacedId, String name) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, namespacedId);
        item.setItemMeta(meta);
        return item;
    }
}
