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
		System.out.println("finalize update init");
		task.doFirst(t -> finalizeUpdate(t, config));
	}

	public void finalizeUpdate(Task task, Config config) {
		boolean success = checkSuccess(task);
		pushCode(config);
	}

	public boolean checkSuccess(Task task) {
		System.out.println(System.getenv("ACTION_STATUS"));
		return false;
	}

	public void pushCode(Config config) {

	}
}
