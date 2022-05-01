package io.github.tropheusj.auto_maintainer;

import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;
import io.github.tropheusj.auto_maintainer.updatables.builtin.GradlePropertiesBasedUpdatable;
import io.github.tropheusj.auto_maintainer.updatables.builtin.MinecraftUpdatable;

import org.gradle.api.Plugin;
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

public class AutoMaintainerPlugin implements Plugin<Project> {
	private final List<String> toDisable = new ArrayList<>();

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
	 * Next, all dependencies will be updated. Depending on each dependency's
	 * UpdateRequirement, the build will either fail or continue.
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
		handleUpdatable(mc, MinecraftUpdatable.UPDATABLE_KEY, project, gradleProperties, config);
		System.out.printf("New Minecraft version %s found; proceeding with update.\n", mc.updateVersion());
		for (Entry<String, Updatable> entry : updatables.entrySet()) {
			Updatable updatable = entry.getValue();
			if (updatable != mc)
				handleUpdatable(updatable, entry.getKey(), project, gradleProperties, config);
		}
		writeNewProperties(gradleProperties, project);
		disableInBuild(project);
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
					updatable.disable(project, gradleProperties);
					System.out.printf("Disabled %s; needs update, but no update is available.\n", name);
				}
			}
		}
	}

	public void writeNewProperties(Properties properties, Project project) {
		// we need to manually update properties since just storing it would discard all formatting.
		File file = project.file("gradle.properties");
		List<String> newData = new ArrayList<>();
		try (FileReader fileReader = new FileReader(file);
			 BufferedReader reader = new BufferedReader(fileReader)) {
			String line;
			while ((line = reader.readLine()) != null) {
				newData.add(updatePropertiesLine(line, properties));
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

	public String updatePropertiesLine(String line, Properties properties) {
		if (!line.contains("="))
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

	public void disableInBuild(Project project) {
		File build = project.getBuildFile();
		List<String> newData = new ArrayList<>();
		try (FileReader fileReader = new FileReader(build);
			 BufferedReader reader = new BufferedReader(fileReader)) {
			String line;
			while ((line = reader.readLine()) != null) {
				newData.add(updateBuildLine(line));
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

	public String updateBuildLine(String line) {
		for (String disabled : toDisable) {
			if (line.contains(disabled)) {
				return "// disabled by AutoMaintainer // " + line;
			}
		}
		return line;
	}
}
