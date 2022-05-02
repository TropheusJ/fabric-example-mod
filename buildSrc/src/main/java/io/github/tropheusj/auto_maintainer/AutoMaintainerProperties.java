package io.github.tropheusj.auto_maintainer;

import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents the automaintainer.properties file in the root directory, used
 * both to store persistent configuration data, and to pass data between tasks.
 */
public class AutoMaintainerProperties {
	public static final String NAME = "automaintainer.properties";

	private final Project project;
	private final File file;
	private final Properties properties;

	public AutoMaintainerProperties(Project project) {
		this.project = project;
		this.file = project.file(NAME);
		this.properties = new Properties();
		if (!file.exists()) {
			// create and set defaults
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Failed to create automaintainer.properties file!", e);
			}
			setDefaults(properties);
			try (FileOutputStream out = new FileOutputStream(file)) {
				properties.store(out, " Properties controlling the behavior of AutoMaintainer.");
			} catch (IOException e) {
				throw new RuntimeException("Failed to save automaintainer.properties file!", e);
			}
		} else {
			try (FileInputStream in = new FileInputStream(file)) {
				properties.load(in);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load automaintainer.properties file!");
			}
		}
	}

	public void setDefaults(Properties properties) {
		properties.setProperty("enabled", "true");
	}

	public boolean enabled() {
		return Boolean.parseBoolean((String) properties.getOrDefault("enabled", "true"));
	}
}
