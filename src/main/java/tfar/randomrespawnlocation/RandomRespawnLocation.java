package tfar.randomrespawnlocation;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RandomRespawnLocation.MODID)
public class RandomRespawnLocation {
    // Directly reference a log4j logger.

    public static final String MODID = "randomrespawnlocation";

    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    public RandomRespawnLocation() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::playerRespawn);
    }

    private static final Random rand = new Random();

    private void playerRespawn(PlayerEvent.PlayerRespawnEvent e) {
        ServerPlayerEntity player = (ServerPlayerEntity) e.getPlayer();
        ServerWorld world = (ServerWorld) player.world;
        BlockPos oldRespawn = player.func_241140_K_();//getRespawnPos
        if (oldRespawn != null) {
            BlockState blockstate = world.getBlockState(oldRespawn);
            Block block = blockstate.getBlock();
            if (block instanceof RespawnAnchorBlock && blockstate.get(RespawnAnchorBlock.CHARGES) > 0 && RespawnAnchorBlock.doesRespawnAnchorWork(world)) {
                Optional<Vector3d> optional = RespawnAnchorBlock.findRespawnPoint(EntityType.PLAYER, world, oldRespawn);
                boolean isRespawnForced = player.func_241142_M_();
                if (!isRespawnForced && optional.isPresent()) {
                    handleRespawnAnchor(player, optional.get(),world);
                }
            } else if (blockstate.isBed(world, oldRespawn, null) && BedBlock.doesBedWork(world)) {
                handleBedRespawn(player, new Vector3d(oldRespawn.getX(), oldRespawn.getY(), oldRespawn.getZ()),world);
            }
            //world spawn
        } else {
            BlockPos newPos = getPos(world.getSpawnPoint(), world);
            player.setPositionAndUpdate(newPos.getX() + 0.5,newPos.getY() + 1.5, newPos.getZ() + 0.5);
        }
    }

    public static void handleRespawnAnchor(ServerPlayerEntity player, Vector3d original,ServerWorld world) {
        RespawnType respawnType = ServerConfig.respawn_anchors.get();
        if (respawnType == RespawnType.NORMAL) {
            return;
        }
        BlockPos newPos = respawnType == RespawnType.IGNORE ? ((ServerWorld) player.world).getSpawnPoint() : getPos(new BlockPos(original), world);
        player.setPositionAndUpdate(newPos.getX() + 0.5, newPos.getY() + 2.6, newPos.getZ() + 0.5);
    }

    public static void handleBedRespawn(ServerPlayerEntity player, Vector3d original,ServerWorld world) {
        RespawnType respawnType = ServerConfig.beds.get();
        if (respawnType == RespawnType.NORMAL) {
            return;
        }
        BlockPos newPos = respawnType == RespawnType.IGNORE ? ((ServerWorld) player.world).getSpawnPoint() : getPos(new BlockPos(original),world);
        player.setPositionAndUpdate(newPos.getX() + 0.5, newPos.getY() + 2.6, newPos.getZ() + 0.5);
    }

    public static BlockPos getPos(BlockPos original, ServerWorld world) {
        int radius = rand.nextInt(ServerConfig.radius.get());
        double angle = rand.nextDouble() * 360;
        int x = (int) (original.getX() + Math.cos(Math.toRadians(angle)) * radius);
        int z = (int) (original.getZ() + Math.sin(Math.toRadians(angle)) * radius);
        Chunk chunk = world.getChunk(x>>4,z>>4);
        int y = world.getDimensionType().getHasCeiling() ? world.getChunkProvider().getChunkGenerator().getGroundHeight() :
                chunk.getTopBlockY(Heightmap.Type.MOTION_BLOCKING, x & 15, z & 15);
        return new BlockPos(x,y, z);
    }

    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair2.getRight();
        SERVER = specPair2.getLeft();
    }

    public static class ServerConfig {

        public static ForgeConfigSpec.IntValue radius;

        public static ForgeConfigSpec.EnumValue<RespawnType> beds;
        public static ForgeConfigSpec.EnumValue<RespawnType> respawn_anchors;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            radius = builder.comment("The radius of the random respawn area").defineInRange("radius", 1000, 1, 30000000);
            beds = builder.
                    comment("Normal doesn't apply random respawn, centered centers the random respawn on the bed, ignore uses worldspawn instead")
                    .defineEnum("beds", RespawnType.NORMAL);
            respawn_anchors = builder.
                    comment("Normal doesn't apply random respawn, centered centers the random respawn on the anchor, ignore uses worldspawn instead")
                    .defineEnum("respawn_anchors", RespawnType.NORMAL);
            builder.pop();
        }
    }

    public enum RespawnType {
        NORMAL, CENTERED, IGNORE
    }
}
