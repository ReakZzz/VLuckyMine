package com.luckymine.managers;

import com.luckymine.LuckyMine;
import com.luckymine.utils.LootEntry;
import com.luckymine.utils.NbtTable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class NBTManager {

    private final LuckyMine plugin;
    private final Random random = new Random();

    /** Location -> scheduled removal task */
    private final Map<Location, BukkitTask> activeLuckyChests = new HashMap<>();

    /** Players currently in "createnbt" mode: UUID -> pending NBT key */
    private final Map<UUID, String> pendingNbtCreators = new HashMap<>();

    public NBTManager(LuckyMine plugin) {
        this.plugin = plugin;
    }

    // ──────────────────────────────────────────
    //  Lucky Container Spawning
    // ──────────────────────────────────────────

    /**
     * Spawn a lucky container at the exact location the block was mined.
     * Called 1 tick after the break so the block is guaranteed AIR.
     *
     * @param location    where the ore was
     * @param table       NbtTable — defines container type + loot entries
     * @param player      the miner — container will face toward them
     */
    public void spawnLuckyChest(Location location, NbtTable table, Player player) {
        if (location.getBlock().getType() != Material.AIR) return;

        final Location finalLocation = location.clone();
        final Block    finalBlock    = finalLocation.getBlock();

        // ── 1. Place the container block ──
        finalBlock.setType(table.getStructure());

        // ── 2. Set facing toward the player ──
        applyFacing(finalBlock, player.getLocation());

        // ── 3. Fill inventory — roll each item's individual chance ──
        BlockState state = finalBlock.getState();
        if (state instanceof Container container) {
            Inventory inv = container.getInventory();
            inv.clear();

            List<ItemStack> winners = new ArrayList<>();
            for (LootEntry entry : table.getItems()) {
                if (random.nextDouble() * 100.0 <= entry.getChance()) {
                    winners.add(entry.getItem());
                }
            }

            // Scatter winners into random slots
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) slots.add(i);
            Collections.shuffle(slots);
            for (int i = 0; i < winners.size() && i < slots.size(); i++) {
                inv.setItem(slots.get(i), winners.get(i));
            }
        }

        // ── 4. Effects ──
        spawnEffects(finalLocation);

        // ── 5. Schedule despawn ──
        long ticks = plugin.getConfigManager().getDespawnTicks();
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> removeLuckyChest(finalLocation), ticks);

        activeLuckyChests.put(finalLocation, task);
    }

    /** Remove a lucky container and cancel its despawn task. Also sets the block to AIR. */
    public void removeLuckyChest(Location location) {
        removeLuckyChest(location, true);
    }

    /**
     * Remove a lucky container and cancel its despawn task.
     * @param removeBlock if false, skip the setType(AIR) call
     *                    (use when the block is already being broken by a BlockBreakEvent)
     */
    public void removeLuckyChest(Location location, boolean removeBlock) {
        BukkitTask task = activeLuckyChests.remove(location);
        if (task != null) task.cancel();

        if (removeBlock) {
            Block block = location.getBlock();
            if (NbtTable.VALID_STRUCTURES.contains(block.getType())) {
                block.setType(Material.AIR);
            }
        }
    }

    public boolean isLuckyChest(Location location) {
        return activeLuckyChests.containsKey(location);
    }

    public void despawnAllChests() {
        new ArrayList<>(activeLuckyChests.keySet()).forEach(this::removeLuckyChest);
        activeLuckyChests.clear();
    }

    // ──────────────────────────────────────────
    //  Facing Logic
    // ──────────────────────────────────────────

    /**
     * Make the container face toward the player.
     * Compares the horizontal offset between the block and the player
     * and picks the dominant cardinal axis (N/S/E/W).
     * Hoppers also support DOWN but we keep it horizontal for consistency.
     */
    private void applyFacing(Block block, Location playerLoc) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Directional directional)) return;

        double dx = playerLoc.getX() - block.getX();
        double dz = playerLoc.getZ() - block.getZ();

        BlockFace face;
        if (Math.abs(dx) >= Math.abs(dz)) {
            face = dx > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else {
            face = dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }

        // Hoppers don't support all faces — guard with getFaces()
        if (directional.getFaces().contains(face)) {
            directional.setFacing(face);
            block.setBlockData(directional);
        }
    }

    // ──────────────────────────────────────────
    //  CreateNBT Session Handling
    // ──────────────────────────────────────────

    public void startNbtCreation(Player player, String nbtKey) {
        pendingNbtCreators.put(player.getUniqueId(), nbtKey);
    }

    public boolean isPendingCreation(Player player) {
        return pendingNbtCreators.containsKey(player.getUniqueId());
    }

    public String getPendingNbtKey(Player player) {
        return pendingNbtCreators.get(player.getUniqueId());
    }

    public void cancelNbtCreation(Player player) {
        pendingNbtCreators.remove(player.getUniqueId());
    }

    // ──────────────────────────────────────────
    //  Effects
    // ──────────────────────────────────────────

    private void spawnEffects(Location location) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isParticlesEnabled() || location.getWorld() == null) return;

        Location center = location.clone().add(0.5, 0.5, 0.5);

        location.getWorld().spawnParticle(
            config.getParticleType(), center,
            config.getParticleCount(),
            config.getParticleOffsetX(), config.getParticleOffsetY(), config.getParticleOffsetZ(),
            config.getParticleSpeed()
        );
        location.getWorld().playSound(center, config.getParticleSound(), 1.0f, 1.0f);
    }
}
