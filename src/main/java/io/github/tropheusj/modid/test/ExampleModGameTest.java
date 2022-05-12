package io.github.tropheusj.modid.test;

import io.github.tropheusj.modid.ExampleMod;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ExampleModGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void autoMaintainerInfoPrinter(GameTestHelper helper) {
		helper.fail("a");
		WorldVersion version = SharedConstants.getCurrentVersion();
		String name = version.getName();
		String target = version.getReleaseTarget();
		String info = String.format("AUTO MAINTAINER INFO || %s || %s", name, target);
		ExampleMod.LOGGER.info(info);
	}
}
