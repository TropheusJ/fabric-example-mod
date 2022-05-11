package io.github.tropheusj.auto_maintainer.tasks;

import io.github.tropheusj.auto_maintainer.Config;

import org.gradle.api.Task;

/**
 * Ran after gametests to finish up the update.
 * Behavior depends on if gametest was successful or not.
 *
 * If it succeeded, code is pushed to GitHub, and a release is
 * published to CurseForge and/or Modrinth and/or GitHub Releases.
 *
 * If it failed, code is pushed to GitHub and not released. A
 * config file is generated in the root directory and AutoMaintainer
 * is disabled until a human can fix it.
 */
public class FinalizeUpdateTask {
	public FinalizeUpdateTask(Task task, Config config) {
		// todo
		task.doFirst((t) -> {
			System.out.println("finalized update");
		});
	}
}
