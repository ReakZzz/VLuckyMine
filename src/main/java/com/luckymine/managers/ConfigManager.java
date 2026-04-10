package com.luckymine.managers;

import com.luckymine.LuckyMine;
import com.luckymine.utils.LootEntry;
import com.luckymine.utils.NbtTable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

    private final LuckyMine plugin;

    // ── File handles ─────────────────────────────────────────────────────
    private File              lootFile;
    private FileConfiguration lootConfig;

    // ── Block config ─────────────────────────────────────────────────────
    private final Map<Material, Double>  blockChances = new HashMap<>();
    private final Map<Material, String>  blockNbtMap  = new HashMap<>();

    // ── NBT tables ───────────────────────────────────────────────────────
    private final Map<String, NbtTable> nbtTables = new HashMap<>();

    // ── World blocking ───────────────────────────────────────────────────
    private final Set<String> blockedWorlds = new HashSet<>();

    // ── Messages ─────────────────────────────────────────────────────────
    private final Map<String, String> messages = new HashMap<>();

    // ── Settings ─────────────────────────────────────────────────────────
    private long    despawnTicks     = 200L;
    private String  prefix           = "&6[&eLuckyMine&6] &r";
    private boolean silkTouchAllowed = false;

    // ── Particles ────────────────────────────────────────────────────────
    private boolean  particlesEnabled = true;
    private Particle particleType     = Particle.PORTAL;
    private Sound    particleSound    = Sound.ENTITY_ENDERMAN_TELEPORT;
    private int      particleCount    = 20;
    private double   particleOffsetX  = 0.3;
    private double   particleOffsetY  = 0.5;
    private double   particleOffsetZ  = 0.3;
    private double   particleSpeed    = 0.05;

    public ConfigManager(LuckyMine plugin) {
        this.plugin = plugin;
        initLootFile();
        load();
    }

    // ──────────────────────────────────────────
    //  File initialisation
    // ──────────────────────────────────────────

    private void initLootFile() {
        lootFile = new File(plugin.getDataFolder(), "loot.yml");
        if (!lootFile.exists()) {
            plugin.saveResource("loot.yml", false);
        }
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
    }

    private void reloadLootFile() {
        if (lootFile == null) lootFile = new File(plugin.getDataFolder(), "loot.yml");
        lootConfig = YamlConfiguration.loadConfiguration(lootFile);
    }

    private void saveLootFile() {
        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save loot.yml!", e);
        }
    }

    // ──────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────

    public void reload() {
        blockChances.clear();
        blockNbtMap.clear();
        nbtTables.clear();
        blockedWorlds.clear();
        messages.clear();
        reloadLootFile();
        load();
    }

    // Block config
    public Map<Material, Double> getBlockChances()     { return Collections.unmodifiableMap(blockChances); }
    public double  getChance(Material m)               { return blockChances.getOrDefault(m, -1.0); }
    public String  getNbtKeyForBlock(Material m)       { return blockNbtMap.get(m); }

    // World blocking
    public boolean isWorldBlocked(String w)            { return blockedWorlds.contains(w); }
    public Set<String> getBlockedWorlds()              { return Collections.unmodifiableSet(blockedWorlds); }

    // NBT tables
    public Set<String> getNbtKeys()                    { return Collections.unmodifiableSet(nbtTables.keySet()); }

    public NbtTable getNbtTable(String key) {
        return nbtTables.get(key);
    }

    public NbtTable getRandomNbtTable() {
        if (nbtTables.isEmpty()) return null;
        List<String> keys = new ArrayList<>(nbtTables.keySet());
        return nbtTables.get(keys.get(new Random().nextInt(keys.size())));
    }

    public NbtTable resolveTableForBlock(Material material) {
        String key = blockNbtMap.get(material);
        if (key == null) return null;
        return key.equalsIgnoreCase("random") ? getRandomNbtTable() : getNbtTable(key);
    }

    // Misc settings
    public long    getDespawnTicks()         { return despawnTicks; }
    public String  getPrefix()               { return prefix; }
    public boolean isSilkTouchAllowed()      { return silkTouchAllowed; }

    // Particles
    public boolean  isParticlesEnabled()     { return particlesEnabled; }
    public Particle getParticleType()        { return particleType; }
    public Sound    getParticleSound()       { return particleSound; }
    public int      getParticleCount()       { return particleCount; }
    public double   getParticleOffsetX()     { return particleOffsetX; }
    public double   getParticleOffsetY()     { return particleOffsetY; }
    public double   getParticleOffsetZ()     { return particleOffsetZ; }
    public double   getParticleSpeed()       { return particleSpeed; }

    // ──────────────────────────────────────────
    //  Messages
    // ──────────────────────────────────────────

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMissing message: " + key)
                       .replace("{prefix}", prefix);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return msg;
    }

    // ──────────────────────────────────────────
    //  NBT Persistence  (written to loot.yml)
    // ──────────────────────────────────────────

    /**
     * Save a loot table created via /luckymine createnbt.
     * Defaults to chest structure and 100% item chance.
     * Admins can edit structure / chance in loot.yml afterwards.
     */
    public void saveNbtEntry(String key, List<ItemStack> items) {
        List<LootEntry> entries = new ArrayList<>();
        List<String>    lines   = new ArrayList<>();

        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            LootEntry entry = new LootEntry(item, 0.0);
            entries.add(entry);
            lines.add(entry.toConfigString());
        }

        nbtTables.put(key, new NbtTable(Material.CHEST, entries));

        lootConfig.set("nbt." + key + ".structure", "chest");
        lootConfig.set("nbt." + key + ".items", lines);
        saveLootFile();
    }

    // ──────────────────────────────────────────
    //  Internal loading
    // ──────────────────────────────────────────

    private void load() {
        // ── config.yml ──
        FileConfiguration cfg = plugin.getConfig();
        despawnTicks     = cfg.getLong("chest-despawn-ticks", 200L);
        prefix           = cfg.getString("prefix", "&6[&eLuckyMine&6] &r");
        silkTouchAllowed = cfg.getBoolean("silk-touch", false);
        loadMessages(cfg);
        loadBlockedWorlds(cfg);
        loadParticles(cfg);

        // ── loot.yml ──
        loadNbtTables();
        loadBlocks();
    }

    private void loadMessages(FileConfiguration cfg) {
        ConfigurationSection sec = cfg.getConfigurationSection("messages");
        if (sec == null) {
            plugin.getLogger().warning("No 'messages' section in config.yml — using fallback strings.");
            return;
        }
        for (String key : sec.getKeys(false)) {
            messages.put(key, sec.getString(key, ""));
        }
    }

    private void loadBlockedWorlds(FileConfiguration cfg) {
        blockedWorlds.addAll(cfg.getStringList("blocked-worlds"));
    }

    private void loadParticles(FileConfiguration cfg) {
        ConfigurationSection sec = cfg.getConfigurationSection("particles");
        if (sec == null) return;

        particlesEnabled = sec.getBoolean("enabled", true);
        particleCount    = sec.getInt("count", 20);
        particleOffsetX  = sec.getDouble("offset-x", 0.3);
        particleOffsetY  = sec.getDouble("offset-y", 0.5);
        particleOffsetZ  = sec.getDouble("offset-z", 0.3);
        particleSpeed    = sec.getDouble("speed", 0.05);

        String particleName = sec.getString("type", "PORTAL");
        try {
            particleType = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type '" + particleName + "', defaulting to PORTAL.");
            particleType = Particle.PORTAL;
        }

        String soundName = sec.getString("sound", "ENTITY_ENDERMAN_TELEPORT");
        try {
            particleSound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound '" + soundName + "', defaulting to ENTITY_ENDERMAN_TELEPORT.");
            particleSound = Sound.ENTITY_ENDERMAN_TELEPORT;
        }
    }

    private void loadNbtTables() {
        ConfigurationSection nbtSection = lootConfig.getConfigurationSection("nbt");
        if (nbtSection == null) {
            plugin.getLogger().warning("No 'nbt' section found in loot.yml!");
            return;
        }

        for (String key : nbtSection.getKeys(false)) {
            ConfigurationSection entry = nbtSection.getConfigurationSection(key);
            if (entry == null) continue;

            Material        structure = NbtTable.parseStructure(entry.getString("structure", "chest"));
            List<LootEntry> entries   = new ArrayList<>();

            for (String line : entry.getStringList("items")) {
                LootEntry lootEntry = parseLootEntry(line);
                if (lootEntry != null) entries.add(lootEntry);
            }

            nbtTables.put(key, new NbtTable(structure, entries));
        }
    }

    private void loadBlocks() {
        ConfigurationSection blocksSection = lootConfig.getConfigurationSection("blocks");
        if (blocksSection == null) {
            plugin.getLogger().warning("No 'blocks' section found in loot.yml!");
            return;
        }

        for (String blockKey : blocksSection.getKeys(false)) {
            Material material;
            try {
                material = Material.valueOf(blockKey.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in loot.yml blocks: " + blockKey);
                continue;
            }

            ConfigurationSection entry = blocksSection.getConfigurationSection(blockKey);
            if (entry == null) continue;

            blockChances.put(material, entry.getDouble("chance", 0.0));
            blockNbtMap.put(material, entry.getString("nbt", "random"));
        }
    }

    // ──────────────────────────────────────────
    //  Parsing helpers
    // ──────────────────────────────────────────

    /**
     * Formats supported:
     *   "DIAMOND"           → amount 1,  chance 100.0
     *   "DIAMOND:2"         → amount 2,  chance 100.0
     *   "DIAMOND:2 50.0"    → amount 2,  chance 50.0
     */
    private LootEntry parseLootEntry(String line) {
        if (line == null || line.isBlank()) return null;
        try {
            String[] spaceParts = line.trim().split("\\s+", 2);
            double   chance     = spaceParts.length > 1 ? Double.parseDouble(spaceParts[1]) : 100.0;

            String[] colonParts = spaceParts[0].split(":", 2);
            Material mat    = Material.valueOf(colonParts[0].trim().toUpperCase());
            int      amount = colonParts.length > 1 ? Integer.parseInt(colonParts[1].trim()) : 1;
            amount = Math.max(1, Math.min(64, amount));

            return new LootEntry(new ItemStack(mat, amount), chance);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Could not parse loot entry: '" + line + "'", e);
            return null;
        }
    }

    // ──────────────────────────────────────────
    //  NBT Table mutations  (GUI + createnbt)
    // ──────────────────────────────────────────

    public void createNbtTable(String key, NbtTable table) {
        nbtTables.put(key, table);
        saveNbtTableToFile(key, table);
    }

    public void deleteNbtTable(String key) {
        nbtTables.remove(key);
        lootConfig.set("nbt." + key, null);
        saveLootFile();
    }

    public void updateNbtStructure(String key, Material structure) {
        NbtTable old = nbtTables.get(key);
        if (old == null) return;
        NbtTable updated = new NbtTable(structure, old.getItems());
        nbtTables.put(key, updated);
        saveNbtTableToFile(key, updated);
    }

    public void setItemAmount(String key, int index, int amount) {
        NbtTable table = nbtTables.get(key);
        if (table == null || index < 0 || index >= table.getItems().size()) return;
        List<LootEntry> items = new ArrayList<>(table.getItems());
        LootEntry old2 = items.get(index);
        ItemStack newItem = old2.getItem();
        newItem.setAmount(Math.max(1, Math.min(64, amount)));
        items.set(index, new LootEntry(newItem, old2.getChance()));
        NbtTable updated = new NbtTable(table.getStructure(), items);
        nbtTables.put(key, updated);
        saveNbtTableToFile(key, updated);
    }

    public void setItemChance(String key, int index, double chance) {
        NbtTable table = nbtTables.get(key);
        if (table == null || index < 0 || index >= table.getItems().size()) return;
        List<LootEntry> items = new ArrayList<>(table.getItems());
        LootEntry old = items.get(index);
        items.set(index, new LootEntry(old.getItem(), Math.max(0, Math.min(100, chance))));
        NbtTable updated = new NbtTable(table.getStructure(), items);
        nbtTables.put(key, updated);
        saveNbtTableToFile(key, updated);
    }

    public void removeItemFromTable(String key, int index) {
        NbtTable table = nbtTables.get(key);
        if (table == null || index < 0 || index >= table.getItems().size()) return;
        List<LootEntry> items = new ArrayList<>(table.getItems());
        items.remove(index);
        NbtTable updated = new NbtTable(table.getStructure(), items);
        nbtTables.put(key, updated);
        saveNbtTableToFile(key, updated);
    }

    public void addItemToTable(String key, ItemStack item, double chance) {
        NbtTable table = nbtTables.get(key);
        if (table == null) return;
        List<LootEntry> items = new ArrayList<>(table.getItems());
        items.add(new LootEntry(item, Math.max(0, Math.min(100, chance))));
        NbtTable updated = new NbtTable(table.getStructure(), items);
        nbtTables.put(key, updated);
        saveNbtTableToFile(key, updated);
    }

    private void saveNbtTableToFile(String key, NbtTable table) {
        lootConfig.set("nbt." + key + ".structure", table.getStructure().name().toLowerCase());
        List<String> lines = new ArrayList<>();
        for (LootEntry e : table.getItems()) lines.add(e.toConfigString());
        lootConfig.set("nbt." + key + ".items", lines);
        saveLootFile();
    }

    // ──────────────────────────────────────────
    //  Block mutations  (GUI)
    // ──────────────────────────────────────────

    public void addOrUpdateBlock(Material material, double chance, String nbtKey) {
        blockChances.put(material, Math.max(0, Math.min(100, chance)));
        blockNbtMap.put(material, nbtKey);
        lootConfig.set("blocks." + material.name() + ".chance", blockChances.get(material));
        lootConfig.set("blocks." + material.name() + ".nbt",    nbtKey);
        saveLootFile();
    }

    public void removeBlock(Material material) {
        blockChances.remove(material);
        blockNbtMap.remove(material);
        lootConfig.set("blocks." + material.name(), null);
        saveLootFile();
    }

    public void updateBlockChance(Material material, double chance) {
        if (!blockChances.containsKey(material)) return;
        blockChances.put(material, Math.max(0, Math.min(100, chance)));
        lootConfig.set("blocks." + material.name() + ".chance", blockChances.get(material));
        saveLootFile();
    }

    public void updateBlockNbt(Material material, String nbtKey) {
        if (!blockNbtMap.containsKey(material)) return;
        blockNbtMap.put(material, nbtKey);
        lootConfig.set("blocks." + material.name() + ".nbt", nbtKey);
        saveLootFile();
    }

}