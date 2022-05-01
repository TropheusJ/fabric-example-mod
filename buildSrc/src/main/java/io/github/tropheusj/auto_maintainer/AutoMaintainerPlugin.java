package io.github.tropheusj.auto_maintainer;

import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;
import io.github.tropheusj.auto_maintainer.updatables.builtin.MinecraftUpdatable;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class AutoMaintainerPlugin implements Plugin<Project> {
	private final List<Updatable> disabled = new ArrayList<>();

	@Override
	public void apply(Project project) {
		Config config = project.getExtensions().create("autoMaintainer", Config.class);
		project.task("tryUpdate", task -> task.doFirst(t -> tryUpdate(t, config)));
	}

	/**
	 * Automatically updates this mod.
	 * Starts by checking for a new Minecraft version. If one is found,
	 * all registered dependencies will be checked for new versions. If all
	 * required dependencies have updates available, the update will proceed.
	 *
	 * // TODO: stage 2
	 * Next, mappings are migrated. Remapped sources will be copied over,
	 * and all dependencies will be updated. If a dependency is required but not
	 * updated, the task will fail.
	 *
	 * // TODO: stage 3
	 * Then, a build is tested. If the build is successful and gametest succeeds,
	 * a build is published to mod hosting sites.
	 * Otherwise, the partial work will be automatically published to a new
	 * git branch. A config file (automaintainer.properties) will be created if it
	 * does not exist, and 'enabled' will be set to false, leaving the branch as-is
	 * until a human can fix it.
	 */
	public void tryUpdate(Task task, Config config) {
		System.out.println("Trying to update...");
		Project project = task.getProject();
		Properties properties = Util.getOrCreateAutoMaintainerProperties(project);
		boolean enabled = Boolean.parseBoolean(properties.getProperty("enabled"));
		if (!enabled) {
			System.out.println("AutoMaintainer is not currently enabled.");
			return;
		}

		Properties gradleProperties = Util.getGradleProperties(project);
		Map<String, Updatable> updatables = config.getUpdatables();
		Updatable mc = updatables.get(MinecraftUpdatable.UPDATABLE_KEY);
		mc.initialize(project, gradleProperties, config);
		if (!mc.hasUpdate()) {
			System.out.println("Current Minecraft version is up-to-date!");
			return;
		}

		System.out.printf("New Minecraft version %s found; proceeding with update.\n", mc.updateVersion());
		for (Entry<String, Updatable> entry : updatables.entrySet()) {
			Updatable updatable = entry.getValue();
			if (updatable != mc)
				handleUpdatable(updatable, entry.getKey(), project, gradleProperties, config);
		}
	}

	public void handleUpdatable(Updatable updatable, String name, Project project, Properties gradleProperties, Config config) {
		updatable.initialize(project, gradleProperties, config);
		if (updatable.hasUpdate()) {
			System.out.printf("updating %s from %s to %s\n", name, updatable.currentVersion(), updatable.updateVersion());
			updatable.update(project, gradleProperties);
		} else {
			UpdateRequirement type = updatable.updateType();
			switch (type) {
				case UPDATE_IF_AVAILABLE ->
						System.out.printf("%s up-to-date @ %s\n", name, updatable.currentVersion());
				case REQUIRED_FOR_UPDATE ->
						throw new RuntimeException(String.format("Mandatory dependency [%s] did not have an update available!", name));
				case DISABLE_IF_UNAVAILABLE -> {
					disabled.add(updatable);
					System.out.printf("Disabled %s; needs update, but no update is available.\n", name);
				}
			}
		}
	}
}
