package network.blazemc.bexport.export;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import network.blazemc.bexport.itemsadder.ItemsAdderIdentity;
import network.blazemc.bexport.skcrates.SkCratesFormat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

class ExportWriterTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void writesYamlTxtAndSkCratesSnippet() throws Exception {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Legendary Ruby Sword"));
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, "ruby:legendary_sword");
        item.setItemMeta(meta);

        PaperItemCodec codec = new PaperItemCodec();
        String base64 = codec.encode(item);
        ItemInspector inspector = new ItemInspector();
        ExportWriter writer = new ExportWriter();
        ItemsAdderIdentity identity = ItemsAdderIdentity.of("ruby:legendary_sword", "test");

        Path yaml = writer.writeItem(
            tempDir,
            "ruby_sword",
            "Admin",
            item,
            base64,
            inspector.describe(item),
            identity,
            SkCratesFormat.notInstalled(),
            true
        );
        Path txt = tempDir.resolve("ruby_sword.txt");
        Path snippet = tempDir.resolve("ruby_sword.skcrates.yml");
        assertTrue(Files.isRegularFile(yaml));
        assertTrue(Files.isRegularFile(txt));
        assertTrue(Files.isRegularFile(snippet));
        String yamlText = Files.readString(yaml);
        assertTrue(yamlText.contains("paper-bytes"));
        assertTrue(yamlText.contains("base64"));
        assertTrue(yamlText.contains("ruby:legendary_sword"));
        assertTrue(yamlText.contains("serializeAsBytes"));
        assertTrue(yamlText.contains("/crates edit"));
        assertTrue(Files.readString(txt).contains(base64));
        assertTrue(Files.readString(snippet).contains("/crates edit"));
    }

    @Test
    void writesAllFourArmorSlots() throws Exception {
        Map<String, ItemStack> pieces = new LinkedHashMap<>();
        Map<String, String> base64 = new LinkedHashMap<>();
        Map<String, Map<String, Object>> info = new LinkedHashMap<>();
        Map<String, ItemsAdderIdentity> ids = new LinkedHashMap<>();
        PaperItemCodec codec = new PaperItemCodec();
        ItemInspector inspector = new ItemInspector();
        putPiece(pieces, base64, info, ids, codec, inspector, "helmet", Material.NETHERITE_HELMET, "ruby:ruby_helmet");
        putPiece(pieces, base64, info, ids, codec, inspector, "chestplate", Material.NETHERITE_CHESTPLATE, "ruby:ruby_chestplate");
        putPiece(pieces, base64, info, ids, codec, inspector, "leggings", Material.NETHERITE_LEGGINGS, "ruby:ruby_leggings");
        putPiece(pieces, base64, info, ids, codec, inspector, "boots", Material.NETHERITE_BOOTS, "ruby:ruby_boots");

        ExportWriter writer = new ExportWriter();
        Path yaml = writer.writeArmor(
            tempDir,
            "ruby_set",
            "Admin",
            pieces,
            base64,
            info,
            ids,
            codec.encodeSet(new ItemStack[]{pieces.get("helmet"), pieces.get("chestplate"), pieces.get("leggings"), pieces.get("boots")}),
            SkCratesFormat.notInstalled(),
            true
        );
        String text = Files.readString(yaml);
        assertTrue(text.contains("pieces.helmet") || text.contains("helmet:"));
        assertTrue(text.contains("ruby:ruby_helmet"));
        assertTrue(text.contains("ruby:ruby_chestplate"));
        assertTrue(text.contains("ruby:ruby_leggings"));
        assertTrue(text.contains("ruby:ruby_boots"));
        String paste = Files.readString(tempDir.resolve("ruby_set.txt"));
        assertTrue(paste.contains(base64.get("helmet")));
        assertTrue(paste.contains(base64.get("boots")));
    }

    private static void putPiece(
        Map<String, ItemStack> pieces,
        Map<String, String> base64,
        Map<String, Map<String, Object>> info,
        Map<String, ItemsAdderIdentity> ids,
        PaperItemCodec codec,
        ItemInspector inspector,
        String slot,
        Material material,
        String namespacedId
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(namespacedId));
        meta.getPersistentDataContainer().set(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING, namespacedId);
        item.setItemMeta(meta);
        pieces.put(slot, item);
        base64.put(slot, codec.encode(item));
        info.put(slot, inspector.describe(item));
        ids.put(slot, ItemsAdderIdentity.of(namespacedId, "test"));
    }
}
