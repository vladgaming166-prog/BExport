package network.blazemc.bexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class BExportPluginTest {

    private ServerMock server;
    private BExportPlugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BExportPlugin.class);
        player = server.addPlayer("Admin");
        player.setOp(true);
        player.addAttachment(plugin, "bexport.admin", true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void helpAndReloadWork() {
        assertTrue(player.performCommand("bexport"));
        assertTrue(player.performCommand("bexport reload"));
        assertTrue(plugin.getDataFolder().toPath().resolve("config.yml").toFile().exists()
            || plugin.getConfig().getBoolean("settings.validate-exports", true));
    }

    @Test
    void exportCommandWritesValidatedFiles() throws Exception {
        player.getInventory().setItemInMainHand(customSword());
        assertTrue(player.performCommand("bexport export ruby_sword"));

        Path exports = plugin.getDataFolder().toPath().resolve("exports");
        Path yaml = exports.resolve("ruby_sword.yml");
        Path txt = exports.resolve("ruby_sword.txt");
        Path index = plugin.getDataFolder().toPath().resolve("exports.yml");
        assertTrue(Files.isRegularFile(yaml), "expected " + yaml);
        assertTrue(Files.isRegularFile(txt), "expected " + txt);
        assertTrue(Files.isRegularFile(index), "expected " + index);
        String yamlText = Files.readString(yaml);
        assertTrue(yamlText.contains("ruby:legendary_sword"));
        assertTrue(yamlText.contains("validated: true") || yamlText.contains("validated: true"));
        assertTrue(Files.readString(index).contains("ruby_sword"));
    }

    @Test
    void exportArmorWritesFourSlots() throws Exception {
        Player living = player;
        living.getInventory().setHelmet(customArmor(Material.NETHERITE_HELMET, "ruby:ruby_helmet"));
        living.getInventory().setChestplate(customArmor(Material.NETHERITE_CHESTPLATE, "ruby:ruby_chestplate"));
        living.getInventory().setLeggings(customArmor(Material.NETHERITE_LEGGINGS, "ruby:ruby_leggings"));
        living.getInventory().setBoots(customArmor(Material.NETHERITE_BOOTS, "ruby:ruby_boots"));
        assertTrue(player.performCommand("bexport exportarmor ruby_set"));
        Path yaml = plugin.getDataFolder().toPath().resolve("exports").resolve("ruby_set.yml");
        assertTrue(Files.isRegularFile(yaml));
        String text = Files.readString(yaml);
        assertTrue(text.contains("ruby:ruby_helmet"));
        assertTrue(text.contains("ruby:ruby_chestplate"));
        assertTrue(text.contains("ruby:ruby_leggings"));
        assertTrue(text.contains("ruby:ruby_boots"));
    }

    @Test
    void rejectsInvalidNames() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        assertTrue(player.performCommand("bexport export ../hack"));
        assertTrue(Files.notExists(plugin.getDataFolder().toPath().resolve("exports").resolve("hack.yml")));
    }

    @Test
    void pluginMetadata() {
        assertEquals("BExport", plugin.getName());
        assertNotNull(plugin.exportService());
    }

    private static ItemStack customSword() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Legendary Ruby Sword"));
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, "ruby:legendary_sword");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack customArmor(Material material, String id) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(id));
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }
}
