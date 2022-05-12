package io.github.tropheusj.modid.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ExampleModGameTest implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void exampleTest(GameTestHelper helper) {
		helper.succeed();
	}
}
