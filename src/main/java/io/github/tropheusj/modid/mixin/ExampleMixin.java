package io.github.tropheusj.modid.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.ServerLevelData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class ExampleMixin {
	@Inject(method = "<init>", at = @At("TAIL"))
	private void modid$init(MinecraftServer minecraftServer, Executor executor, LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData, ResourceKey resourceKey, LevelStem levelStem, ChunkProgressListener chunkProgressListener, boolean bl, long l, List list, boolean bl2, CallbackInfo ci) {
		System.out.println("example!");
	}
}
