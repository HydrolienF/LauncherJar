# launcherjar

Optionals files are:
- README.md
- LICENSE.md
- CHANGELOG.md
- jvm.config
- icon.png
- icon.ico
- icon.icns

Icon are png for Linux, ico for Windows & icns for Mac. You can provide the 3 files icons or just some.
LauncherJar will create a .ico & a .icns from the .png if the files are missing.

Check [a simple project using LauncherJar](https://github.com/HydrolienF/LauncherJarExample) if you need help.

Your app need to return it's version when launching with `--version`.

If your App return code >=100 it will have effect on launcher :
- 100: App will be restarted

You need to give Read and write permissions in Workflow permissions for the github action that use that at https://github.com/You/ProjectName/settings/actions
