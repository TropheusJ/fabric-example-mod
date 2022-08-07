package io.github.tropheusj.auto_maintainer.tasks;

import io.github.tropheusj.auto_maintainer.AutoMaintainerProperties;
import io.github.tropheusj.auto_maintainer.BranchCreationMode;
import io.github.tropheusj.auto_maintainer.Config;

import io.github.tropheusj.auto_maintainer.Util;

import io.github.tropheusj.auto_maintainer.minecraft.Minecraft;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.IOException;

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
	public FinalizeUpdateTask(Task task, Config config, Project project) {
		task.doFirst(t -> finalizeUpdate(t, config, project));
	}

	public void finalizeUpdate(Task task, Config config, Project project) {
		pushCode(config, project);
		boolean success = checkSuccess(task);
		boolean pushed = pushCode(config, project);
		if (success) {
			if (pushed) {
//				publishRelease();
				System.out.println("Successfully pushed!");
			} else {
				System.out.println("Error pushing - a release will not be published.");
			}
		} else {
			System.out.println("Unsuccessful update :(");
//			disable();
		}
	}

	public boolean checkSuccess(Task task) {
		System.out.println("action status: " + System.getenv("ACTION_STATUS"));
		return true;
	}

	/**
	 * Push the updated code to the repo.
	 * @return true if successful
	 */
	public boolean pushCode(Config config, Project project) {
		try (Repository repo = new FileRepository(project.getRootDir()); Git git = new Git(repo)) {
			BranchCreationMode mode = config.getBranchCreationMode();
			git.add()
					.addFilepattern(AutoMaintainerProperties.NAME) // should be the only new file
					.call();
			git.commit()
					.setAll(true) // commit all changes
					.setMessage("Automaintainer - update to: " + Util.getMcVer())
					.setAuthor("Automaintainer", "<>")
					.call();
			PushCommand push = git.push();
			if (shouldBranch(mode)) {
				String format = config.getBranchFormat();
				String branchName = format.formatted(Util.getMcVer());
				RefSpec refSpec = new RefSpec("refs/heads/" + branchName);
				push.setRefSpecs(refSpec);
			}
			push.call();
			return true;
		} catch (Exception e) {
			System.out.println("Error pushing updated code!");
			e.printStackTrace();
			return false;
		}
	}

	private boolean shouldBranch(BranchCreationMode mode) {
		String newVersion = Util.getMcVer();
		boolean release = Minecraft.RELEASE.matcher(newVersion).matches(); // 1.19 and 1.19.1 = true, 22w14a = false
		boolean major = newVersion.split("\\.").length == 2; // 1.19 = 2, 1.19.1 = 3
		return switch (mode) {
			case ALL -> true;
			case PATCH -> release;
			case MAJOR -> major;
		};

	}
}
