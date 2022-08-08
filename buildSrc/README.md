# AutoMaintainer
### A gradle plugin to keep your Minecraft mods automatically up to date.

# Use
AutoMaintainer is designed to be run in a specific environment, and was designed
around [my example mod](https://github.com/TropheusJ/fabric-example-mod).
If any of these assumptions are not correct, bad things will happen.
- AutoMaintaner is being run from a GitHub Actions environment and all tasks
are correctly set up in the workflow
- The mod has GameTests set up
- The mod is for Fabric, Quilt is not yet supported
- The mod is one project. Architectury is not supported, might happen eventually.
- A root gradle.properties exists and is used for dependency version management
- all dependencies are registered to AutoMaintainer
- A personal access token is provided to AutoMaintainer with the correct
privileges (don't worry, it doesn't need much)

# Setup
More will be added here later.
- Add AutoMaintainer to your project.
- Specify your dependencies in the `autoMaintainer` block in your build.gradle
  - configure as desired here as well
- Set up your workflow
  - see try_update.yml for an example
  - Must run `gradlew tryUpdate`, `gradlew runGametest`,
  and `gradlew finalizeUpdate` in that order
  - Requires some environment variables: `job.status`, `github.repository`,
  and `secrets.AUTOMAINTAINER_TOKEN`
    - `AUTOMAINTAINER_TOKEN` must be a repository secret containing a personal
    access token. The only permission this token needs is `repo/public_repo`.
    It's used for changing the default branch whatever a newer one is created.
    This is needed because scheduled workflows seem to only work on the
    default branch.
- Set up your game tests. AutoMaintainer will use the status of your tests to
determine if a new release should be made. **If all tests pass, the update
will be published.**
  - TODO gametest tutorial

# Process
- AutoMaintainer starts by checking for a new Minecraft version. If one is not
found, nothing happens, and it exits.
- Next, all registered dependencies are checked for new versions. Depending
on the requirement of each dependency, it will either be updated, be disabled,
or cause the update to fail.
- Next, Game Tests are run. If it fails for whatever reason, code is pushed
to GitHub, and AutoMaintainer is disabled, waiting for a human to fix it.
- Otherwise, code is pushed, AutoMaintainer is left enabled, and the new version
of the mod is published (NYI) to mod host sites (Modrinth, Curseforge, Github Releases)
- The target branch of pushed code is configurable. It can make a new branch for
only major versions, only patch versions, or all versions. The default branch will
be updated when a new branch is made.
