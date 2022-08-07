package io.github.tropheusj.auto_maintainer.tasks;

import io.github.tropheusj.auto_maintainer.AutoMaintainerProperties;
import io.github.tropheusj.auto_maintainer.Config;

import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.minecraft.Minecraft;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;
import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;
import io.github.tropheusj.auto_maintainer.updatables.builtin.GradlePropertiesBasedUpdatable;

import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Automatically updates this mod.
 * Starts by checking for a new Minecraft version. If one is found,
 * all registered dependencies will be checked for new versions. If all
 * required dependencies have updates available, the update will proceed.
 *
 * Next, all dependencies will be updated. Depending on each dependency's
 * UpdateRequirement, the build will either fail or continue.
 *
 * Then, a build is tested. Once the test finishes, FinalizeUpdateTask is run.
 */
public class TryUpdateTask {
	public TryUpdateTask(Task task, Config config) {
		task.doFirst(t -> tryUpdate(t, config));
	}

	public void tryUpdate(Task task, Config config) {
		System.out.println("Trying to update...");
		Project project = task.getProject();
		AutoMaintainerProperties properties = new AutoMaintainerProperties(project);
		if (!properties.enabled()) {
			System.out.println("AutoMaintainer is not currently enabled.");
			CrossTaskDataHolder.finalize = false;
			return;
		}

		Properties gradleProperties = Util.getGradleProperties(project);
		if (Minecraft.INSTANCE.versions.upToDate()) {
			System.out.printf("Current Minecraft version (%s) is up-to-date!\n", Util.getMcVer());
			CrossTaskDataHolder.finalize = false;
			return;
		}
		String mcVer = Util.getMcVer();
		gradleProperties.setProperty(Minecraft.GRADLE_PROPERTIES_MC_VER_KEY, mcVer);
		System.out.printf("New Minecraft version %s found; proceeding with update.\n", mcVer);
		Map<String, Updatable> updatables = config.getUpdatables();
		updatables.forEach((name, updatable) ->
				handleUpdatable(updatable, name, project, gradleProperties, config));
		List<String> toDisable = new ArrayList<>();
		writeNewProperties(gradleProperties, project, toDisable);
		disableInBuild(project, toDisable);
	}

	/**
	 * Handle an Updatable. Checks if it has an update, and updates it if so.
	 * Otherwise, checks the UpdateRequirement and either fails, continues, or disables.
	 */
	public void handleUpdatable(Updatable updatable, String name, Project project, Properties gradleProperties, Config config) {
		updatable.initialize(project, gradleProperties, config);
		if (updatable.hasUpdate()) {
			System.out.printf("updating %s from %s to %s\n", name, updatable.currentVersion(), updatable.updateVersion());
			updatable.update(project, gradleProperties);
		} else {
			UpdateRequirement type = updatable.updateType();
			switch (type) {
				case UPDATE_IF_AVAILABLE -> upToDate(name, updatable);
				case REQUIRED_FOR_UPDATE -> {
					if (updatable.currentVersionFine()) {
						upToDate(name, updatable);
					} else {
						throw new RuntimeException(String.format("Required dependency [%s] did not have an update available!", name));
					}
				}
				case DISABLE_IF_UNAVAILABLE -> {
					if (updatable.currentVersionFine()) {
						upToDate(name, updatable);
					} else {
						updatable.disable(project, gradleProperties);
						System.out.printf("Disabled %s; needs update, but no update is available.\n", name);
					}
				}
			}
		}
	}

	public void upToDate(String name, Updatable updatable) {
		System.out.printf("%s up-to-date @ %s\n", name, updatable.currentVersion());
	}

	/**
	 * Write the modified properties to the project root gradle.properties file.
	 */
	public void writeNewProperties(Properties properties, Project project, List<String> toDisable) {
		// we need to manually update properties since just storing it would discard all formatting.
		File file = project.file("gradle.properties");
		List<String> newData = new ArrayList<>();
		try (FileReader fileReader = new FileReader(file);
			 BufferedReader reader = new BufferedReader(fileReader)) {
			String line;
			while ((line = reader.readLine()) != null) {
				String newLine = updatePropertiesLine(line, properties, toDisable);
				newData.add(newLine);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (FileWriter writer = new FileWriter(file)) {
			for (String line : newData)
				writer.append(line).append('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Updates a line declaring a version of a dependency.
	 * If the dependency was disabled, the line is commented.
	 * If the dependency was updated, the version is updated.
	 */
	public String updatePropertiesLine(String line, Properties properties, List<String> toDisable) {
		if (!line.contains("=") || line.trim().startsWith("#"))
			return line;
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String[] current = line.split("=");
			String currentKey = current[0].trim();
			String currentValue = current[1].trim();
			String newKey = (String) entry.getKey();
			if (currentKey.equals(newKey)) {
				String newValue = (String) entry.getValue();
				if (newValue.equals(GradlePropertiesBasedUpdatable.DISABLED_MARKER)) {
					toDisable.add(newKey);
					return "# Automatically disabled by AutoMaintainer # " + line;
				}
				return line.replace(currentValue, newValue);
			}
		}
		return line;
	}

	/**
	 * Go through the project root build.gradle file and comment out any dependencies that were disabled.
	 */
	public void disableInBuild(Project project, List<String> toDisable) {
		File build = project.getBuildFile();
		List<String> newData = new ArrayList<>();
		try (FileReader fileReader = new FileReader(build);
			 BufferedReader reader = new BufferedReader(fileReader)) {
			String line;
			while ((line = reader.readLine()) != null) {
				newData.add(updateBuildLine(line, toDisable));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try (FileWriter writer = new FileWriter(build)) {
			for (String line : newData)
				writer.append(line).append('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add a comment to the start of the given line if the dependency it declares was disabled.
	 */
	public String updateBuildLine(String line, List<String> toDisable) {
		for (String disabled : toDisable) {
			if (line.contains(disabled) && !line.trim().startsWith("//")) {
				return "// disabled by AutoMaintainer // " + line;
			}
		}
		return line;
	}
}
