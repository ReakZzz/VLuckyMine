package com.luckymine.commands;

import com.luckymine.LuckyMine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class LuckyMineCommand implements CommandExecutor, TabCompleter {

    private final LuckyMine plugin;

    public LuckyMineCommand(LuckyMine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luckymine.admin")) {
            sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                plugin.getConfigManager().getMessage("no-permission")
            ));
            return true;
        }

        // /lm  /luckymine  /lm menu  /luckymine menu  → open GUI
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("player-only")
                ));
                return true;
            }
            plugin.getGuiManager().openMain(p);
            return true;
        }

        // Keep text-only commands for console / scripts
        switch (args[0].toLowerCase()) {

            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("reload-success")
                ));
                sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("reload-stats",
                        "blocks", String.valueOf(plugin.getConfigManager().getBlockChances().size()),
                        "nbt",    String.valueOf(plugin.getConfigManager().getNbtKeys().size()))
                ));
            }

            case "createnbt" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                        plugin.getConfigManager().getMessage("player-only")
                    ));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                        plugin.getConfigManager().getMessage("createnbt-usage")
                    ));
                    return true;
                }
                String key = args[1].toLowerCase().replace(" ", "_");
                if (!key.matches("[a-z0-9_]+")) {
                    sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                        plugin.getConfigManager().getMessage("createnbt-invalid-key")
                    ));
                    return true;
                }
                com.luckymine.managers.NBTManager nbt = plugin.getNbtManager();
                if (nbt.isPendingCreation(player)) nbt.cancelNbtCreation(player);
                nbt.startNbtCreation(player, key);
                player.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("createnbt-activated", "key", key)
                ));
                player.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("createnbt-instructions")
                ));
                player.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                    plugin.getConfigManager().getMessage("createnbt-cancel-hint")
                ));
            }

            case "cancel" -> {
                if (!(sender instanceof Player player)) return true;
                com.luckymine.managers.NBTManager nbt = plugin.getNbtManager();
                if (nbt.isPendingCreation(player)) {
                    nbt.cancelNbtCreation(player);
                    sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                        plugin.getConfigManager().getMessage("cancel-success")
                    ));
                } else {
                    sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(
                        plugin.getConfigManager().getMessage("cancel-not-active")
                    ));
                }
            }

            default -> sendHelp(sender, label);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luckymine.admin")) return List.of();
        if (args.length == 1) {
            return java.util.Arrays.asList("menu", "createnbt", "reload", "cancel").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("createnbt")) {
            return new java.util.ArrayList<>(plugin.getConfigManager().getNbtKeys()).stream()
                .filter(k -> k.startsWith(args[1].toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        String p = plugin.getConfigManager().getPrefix();
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy(p + "&6=== LuckyMine ==="));
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy("&e/" + label + "          &7- Open the GUI menu"));
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy("&e/" + label + " menu     &7- Open the GUI menu"));
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy("&e/" + label + " reload   &7- Reload config"));
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy("&e/" + label + " createnbt <key>  &7- Create NBT table via chest"));
        sender.sendMessage(com.luckymine.gui.GuiUtil.legacy("&e/" + label + " cancel   &7- Cancel createnbt session"));
    }
}
