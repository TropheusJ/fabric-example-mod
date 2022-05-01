package io.github.tropheusj.auto_maintainer.updatables.builtin;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import org.gradle.api.Project;

import java.util.Properties;

public abstract class GradlePropertiesBasedUpdatable implements Updatable {
	public static final String DISABLED_MARKER = "%DISABLED%";
	protected String currentVersion;
	protected String newVersion;
	protected String name;
	protected String propertyKey;

	public GradlePropertiesBasedUpdatable(String name, String propertyKey) {
		this.name = name;
		this.propertyKey = propertyKey;
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		currentVersion = properties.getProperty(propertyKey);
		Util.checkNull(currentVersion, name, propertyKey);
	}

	@Override
	public boolean hasUpdate() {
		return newVersion != null && !currentVersion.equals(newVersion);
	}

	@Override
	public void update(Project project, Properties properties) {
		properties.setProperty(propertyKey, newVersion);
	}

	@Override
	public String currentVersion() {
		return currentVersion;
	}

	@Override
	public String updateVersion() {
		return newVersion;
	}

	@Override
	public void disable(Project project, Properties properties) {
		properties.setProperty(propertyKey, DISABLED_MARKER);
	}
}
