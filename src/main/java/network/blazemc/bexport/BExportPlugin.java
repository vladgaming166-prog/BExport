package network.blazemc.bexport;

import network.blazemc.bexport.command.BExportCommand;
import network.blazemc.bexport.export.ExportService;
import network.blazemc.bexport.itemsadder.ItemsAdderAccess;
import network.blazemc.bexport.skcrates.SkCratesAccess;
import network.blazemc.bexport.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BExportPlugin extends JavaPlugin {

    private Messages messages;
    private ItemsAdderAccess itemsAdderAccess;
    private SkCratesAccess skCratesAccess;
    private ExportService exportService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messages = new Messages(this);
        this.itemsAdderAccess = new ItemsAdderAccess(this);
        this.skCratesAccess = new SkCratesAccess(this);
        this.exportService = new ExportService(this, messages, itemsAdderAccess, skCratesAccess);

        BExportCommand command = new BExportCommand(this, exportService, messages);
        PluginCommand pluginCommand = getCommand("bexport");
        if (pluginCommand == null) {
            getLogger().severe("plugin.yml is missing the bexport command. Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);

        logEnvironment();
        itemsAdderAccess.logStatus();
        skCratesAccess.inspectAndLog();
        getLogger().info("BExport " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BExport disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        messages.reload();
        itemsAdderAccess.logStatus();
        skCratesAccess.inspectAndLog();
        getLogger().info("Configuration reloaded.");
    }

    public ExportService exportService() {
        return exportService;
    }

    public Messages messages() {
        return messages;
    }

    public SkCratesAccess skCratesAccess() {
        return skCratesAccess;
    }

    private void logEnvironment() {
        String minecraft = getServer().getMinecraftVersion();
        String bukkit = getServer().getBukkitVersion();
        String java = System.getProperty("java.version");
        getLogger().info("Runtime: Minecraft " + minecraft + ", Bukkit " + bukkit + ", Java " + java);
        getLogger().info("Compiled for Paper 1.21.11 (Java 21 bytecode). Compatible with Paper 26.2 / Java 25.");
    }
}
