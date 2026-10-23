package com.spheroidon.oreReplacer;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            Material.GILDED_BLACKSTONE // remove this line if you want to keep gilded blackstone
    );

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("OreReplacer enabled - new chunks will have ores stripped.");
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material replacement = replacementFor(block.getType(), isNether);
                    if (replacement != null) {
                        block.setType(replacement, false); // false = no physics/light recalculation spam
                    }
                }
            }
        }
    }

    private Material replacementFor(Material mat, boolean isNether) {
        String name = mat.name();

        boolean isOre = name.endsWith("_ORE") || EXTRA_ORES.contains(mat);
        if (!isOre) {
            return null;
        }

        if (isNether) {
            if (name.endsWith("BLACKSTONE")) {
                return Material.BLACKSTONE;
            }
            return Material.NETHERRACK;
        }
        if (name.startsWith("DEEPSLATE_")) {
            return Material.DEEPSLATE;
        }
        return Material.STONE;
    }
}
