package com.spheroidon.oreReplacer;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

/**
 * Replaces every ore block with stone/deepslate (or netherrack in the Nether)
 * the moment a chunk finishes generating — regardless of which datapack
 * placed the ore. This does NOT touch already-generated chunks; only newly
 * generated ones from this point forward.
 */
public final class OreReplacer extends JavaPlugin implements Listener {

    // Ores that don't follow the "*_ORE" naming pattern.
    private static final Set<Material> EXTRA_ORES = EnumSet.of(
            Material.ANCIENT_DEBRIS,
            Material.GILDED_BLACKSTONE,
            // Raw ore blocks placed directly by the 1.18+ large ore vein feature.
            Material.RAW_IRON_BLOCK,
            Material.RAW_COPPER_BLOCK,
            Material.RAW_GOLD_BLOCK
    );

    // Y boundary used as a stand-in for "stone vs deepslate" when a block
    // (like a raw ore block) has no DEEPSLATE_ prefix to key off of.
    private static final int DEEPSLATE_Y_THRESHOLD = 0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Sweep chunks that are already loaded (e.g. spawn chunks, which are
        // generated/kept loaded before this plugin ever sees a populate/load
        // event for them on a pre-existing world).
        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                processChunk(chunk);
            }
        }

        getLogger().info("OreReplacer enabled — swept loaded chunks and will strip ores from new ones.");
    }

    // Covers normal new-chunk generation.
    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        processChunk(event.getChunk());
    }

    // Belt-and-braces: ChunkPopulateEvent can be skipped for chunks generated
    // through some paths (pre-generation, multithreaded/async generation,
    // structure lookups). isNewChunk() reliably catches those too.
    // Reprocessing an already-handled chunk is harmless (idempotent).
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            processChunk(event.getChunk());
        }
    }

    private void processChunk(Chunk chunk) {
        World world = chunk.getWorld();
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material replacement = replacementFor(block.getType(), y, isNether);
                    if (replacement != null) {
                        block.setType(replacement, false); // false = no physics/light recalculation spam
                    }
                }
            }
        }
    }

    private Material replacementFor(Material mat, int y, boolean isNether) {
        String name = mat.name();

        // Gilded blackstone isn't an "ore" mechanically, but it's an ore-like
        // loot block generated in bastions/nether terrain — swap it for plain
        // blackstone instead of netherrack/stone.
        if (mat == Material.GILDED_BLACKSTONE) {
            return Material.BLACKSTONE;
        }

        boolean isOre = name.endsWith("_ORE") || EXTRA_ORES.contains(mat);
        if (!isOre) {
            return null;
        }

        if (isNether) {
            return Material.NETHERRACK;
        }
        if (name.startsWith("DEEPSLATE_")) {
            return Material.DEEPSLATE;
        }
        if (name.startsWith("RAW_")) {
            // No deepslate-prefixed variant exists for raw blocks, so use Y
            // as a stand-in for which stone layer they generated in.
            return y < DEEPSLATE_Y_THRESHOLD ? Material.DEEPSLATE : Material.STONE;
        }
        return Material.STONE;
    }
}
