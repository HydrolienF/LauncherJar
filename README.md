# launcherjar

Currently it need to have some file on the project:
- README.md
- LICENSE.md
- CHANGELOG.md
- .mvn/jvm.config
- tools/icon.png
- tools/icon.ico
- tools/icon.icns

Icon are png for Linux, ico for Windows & icns for Mac. You can provide the 3 files icons or just some.

Check [a simple project using LauncherJar](https://github.com/HydrolienF/Infanlaboro) if you need help.

Example use auto version tool based on branch name that should be 0.1, 0.2, 0.3, 1.1, 1.2 etc

Your app need to return it's version when launching with --version or -version

You need to give Read and write permissions in Workflow permissions for the github action that use that at https://github.com/You/ProjectName/settings/actions

## [Javadoc](https://formiko.fr/LauncherJar/javadoc/)
