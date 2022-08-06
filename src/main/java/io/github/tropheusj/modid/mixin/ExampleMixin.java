package io.github.tropheusj.modid.mixin;

import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ExampleMixin {
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void modid$init(CallbackInfo ci) {
		System.out.println("example!");
	}
}
