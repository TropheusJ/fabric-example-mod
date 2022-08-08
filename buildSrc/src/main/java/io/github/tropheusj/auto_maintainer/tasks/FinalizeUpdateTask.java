package io.github.tropheusj.auto_maintainer.tasks;

import io.github.tropheusj.auto_maintainer.AutoMaintainerProperties;
import io.github.tropheusj.auto_maintainer.BranchCreationMode;
import io.github.tropheusj.auto_maintainer.Config;

import io.github.tropheusj.auto_maintainer.Util;

import io.github.tropheusj.auto_maintainer.minecraft.Minecraft;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

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
		if (!CrossTaskDataHolder.finalize) {
			return;
		}
		boolean success = checkSuccess();
		if (success) {
			tryPush(config, project, true);
		} else {
			disable(project);
			tryPush(config, project, false);
			throw new RuntimeException("Gametests failed, not publishing");
		}
	}

	public void tryPush(Config config, Project project, boolean publish) {
		if (pushCode(config, project)) {
			if (publish) {
//				publishRelease();
			}
			System.out.println("Successfully pushed!");
		} else {
			throw new RuntimeException("Error pushing - a release will not be published.");
		}
	}

	public boolean checkSuccess() {
		return "success".equals(System.getenv("JOB_STATUS"));
	}

	/**
	 * Push the updated code to the repo.
	 * @return true if successful
	 */
	private boolean pushCode(Config config, Project project) {
		File dotGit = project.getProjectDir().toPath().resolve(".git").toFile();
		try (Repository repo = new FileRepositoryBuilder().setGitDir(dotGit).build(); Git git = new Git(repo)) {
			BranchCreationMode mode = config.getBranchCreationMode();
			git.add()
					.addFilepattern(AutoMaintainerProperties.NAME) // should be the only new file
					.call();
			git.commit()
					.setAll(true) // commit all changes
					.setMessage("AutoMaintainer - update to: " + Util.getMcVer())
					.setAuthor("AutoMaintainer", "<>")
					.call();
			PushCommand push = git.push();
			String branchName = null;
			if (shouldBranch(mode)) {
				String format = config.getBranchFormat();
				branchName = format.formatted(Util.getMcVer());
				String currentBranchName = repo.getFullBranch();
				RefSpec refSpec = new RefSpec(currentBranchName + ":" + branchName);
				push.setRefSpecs(refSpec);
			}
			push.call();
			if (branchName != null) {
				updateDefaultBranch(branchName);
			}
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

	private void updateDefaultBranch(String branchName) {
		String[] repo = System.getenv("GH_REPOSITORY").split("/");
		String owner = repo[0];
		String repoName = repo[1];
		String token = System.getenv("GH_TOKEN");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.github.com/repos/%s/%s".formatted(owner, repoName)))
				.setHeader("Authorization", "token " + token)
				.setHeader("Content-Type", "application/json")
				.method("PATCH", BodyPublishers.ofString("""
						{
							"default_branch": "%s"
						}
						""".formatted(branchName)))
				.build();
		try {
			HttpResponse<String> response = Util.HTTP_CLIENT.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				System.out.println("Successfully updated the default branch to: " + branchName);
			} else {
				System.out.println("Error updating the default branch, status code " + response.statusCode());
				System.out.println(response.body());
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void disable(Project project) {
		AutoMaintainerProperties properties = new AutoMaintainerProperties(project);
		properties.disable();
		properties.save();
	}
}
