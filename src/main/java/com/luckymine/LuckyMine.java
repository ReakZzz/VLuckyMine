package com.luckymine;

import com.luckymine.commands.LuckyMineCommand;
import com.luckymine.gui.GuiManager;
import com.luckymine.listeners.BlockBreakListener;
import com.luckymine.listeners.ChestInteractListener;
import com.luckymine.listeners.GuiListener;
import com.luckymine.managers.ConfigManager;
import com.luckymine.managers.NBTManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LuckyMine extends JavaPlugin {

    private static LuckyMine instance;
    private ConfigManager configManager;
    private NBTManager    nbtManager;
    private GuiManager    guiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        nbtManager    = new NBTManager(this);
        guiManager    = new GuiManager(this);

        LuckyMineCommand cmd = new LuckyMineCommand(this);
        getCommand("luckymine").setExecutor(cmd);
        getCommand("luckymine").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this),  this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, guiManager), this);

        getLogger().info("\u256C\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563");
        getLogger().info("\u2551   V-LuckyMine v" + getDescription().getVersion() + " Enabled  \u2551");
        getLogger().info("\u255A\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255D");
        getLogger().info("Blocks: " + configManager.getBlockChances().size()
                + " | Loot tables: " + configManager.getNbtKeys().size());
    }

    @Override
    public void onDisable() {
        if (nbtManager != null) nbtManager.despawnAllChests();
        getLogger().info("LuckyMine disabled.");
    }

    public static LuckyMine getInstance() { return instance; }

    public ConfigManager getConfigManager() { return configManager; }
    public NBTManager    getNbtManager()    { return nbtManager; }
    public GuiManager    getGuiManager()    { return guiManager; }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        getLogger().info("LuckyMine reloaded. Blocks: " + configManager.getBlockChances().size()
                + " | Loot tables: " + configManager.getNbtKeys().size());
    }
}
