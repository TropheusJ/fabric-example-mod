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
- A root gradle.properties exists
- All registered dependencies have correct version keys in the root
gradle.properties file

# Process
- AutoMaintainer starts by checking for a new Minecraft version. If one is not
found, nothing happens, and it exits.
- Next, all registered dependencies are checked for new versions. Depending
on the requirement of each dependency, it will either be updated, be disabled,
or cause the update to fail.
- Next, Game Tests are run. If it fails for whatever reason, code is pushed (NYI)
to GitHub, and AutoMaintainer is disabled, waiting for a human to fix it.
- (NYI) Otherwise, code is pushed, AutoMaintainer is left enabled, and the new version
of the mod is published to mod host sites (Modrinth, Curseforge, Github Releases)
- (NYI) Whenever code is pushed, it is pushed to a branch corresponding to the
Minecraft version's target version. Ex. for 22w18a that's 1.19, and for
1.18.2 it's also 1.18.2.
