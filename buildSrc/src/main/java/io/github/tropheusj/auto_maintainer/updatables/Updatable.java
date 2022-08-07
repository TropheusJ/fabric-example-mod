package io.github.tropheusj.auto_maintainer.updatables;

import io.github.tropheusj.auto_maintainer.Config;

import org.gradle.api.Project;

import java.util.Properties;

/**
 * Represents an automatically updatable dependency.
 */
public interface Updatable {
	/**
	 * Initialize this dependency with values from the project.
	 * @param project the entire Gradle project
	 * @param properties a Properties representing the project's root gradle.properties file
	 * @param config the current Config, can be used to check versions of other dependencies.
	 */
	void initialize(Project project, Properties properties, Config config);

	/**
	 * Check if this dependency has an update available.
	 */
	boolean hasUpdate();

	/**
	 * Sometimes, a dependency does not need an update to work on a new version.
	 * @return true if the current version can be used safely.
	 */
	default boolean currentVersionFine() {
		return false;
	}

	/**
	 * Actually update this dependency.
	 * @param project the entire Gradle project
	 * @param properties a Properties representing the project's root gradle.properties file
	 */
	void update(Project project, Properties properties);

	/**
	 * The current version of this dependency.
	 */
	String currentVersion();

	/**
	 * The version this dependency will be updated to. If up-to-date, equal to currentVersion.
	 */
	String updateVersion();

	/**
	 * The UpdateType of this mod; determines what happens if unavailable at time of update.
	 */
	default UpdateRequirement updateType() {
		return UpdateRequirement.REQUIRED_FOR_UPDATE;
	}

	void disable(Project project, Properties properties);
}
