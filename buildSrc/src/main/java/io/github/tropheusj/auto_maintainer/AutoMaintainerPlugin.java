package io.github.tropheusj.auto_maintainer;

import io.github.tropheusj.auto_maintainer.tasks.FinalizeUpdateTask;
import io.github.tropheusj.auto_maintainer.tasks.TryUpdateTask;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class AutoMaintainerPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		System.out.println("plugin init");
		StaticTest.i++;
		System.out.println(StaticTest.i);
		Config config = project.getExtensions().create("autoMaintainer", Config.class);

		Task update = project.task("tryUpdate", t -> new TryUpdateTask(t, config));
		Task finalize = project.task("finalizeUpdate", t -> new FinalizeUpdateTask(t, config));

		Util.createGameTestTask(project);
		Task gametest = project.getTasks().getByName("runGametest");
	}
}
