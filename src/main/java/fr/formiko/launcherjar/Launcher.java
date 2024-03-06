package fr.formiko.launcherjar;

import fr.formiko.usual.Os;
import fr.formiko.usual.Progression;
import fr.formiko.usual.ReadFile;
import fr.formiko.usual.color;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public class Launcher {
    private Process pr;
    private String userName;
    private String projectName;
    private boolean gui;
    private List<String> args;
    private Progression progression;
    private String currentAppVersion;
    private String lastAppVersion;
    private boolean justGetVersion = false;
    private String mainFolder;

    public Launcher(List<String> args, String userName, String projectName, boolean gui) {
        this.args = args;
        this.userName = userName;
        this.projectName = projectName;
        this.gui = gui;
        color.iniColor();
    }
    public Launcher(List<String> args, String userName, String projectName) { this(args, userName, projectName, true); }

    public Launcher(List<String> args) {
        pr = null;
        this.args = args;
        color.iniColor();
        try {
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("settings.json");
            Reader reader = new BufferedReader(new InputStreamReader(is));
            JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
            userName = (String) parser.get("userName");
            if (userName == null) {
                erreur.erreur("can't read userName from " + reader);
            }
            projectName = (String) parser.get("projectName");
            if (projectName == null) {
                erreur.erreur("can't read projectName from " + reader);
            }
            gui = parser.containsKey("gui") ? (boolean) parser.get("gui") : true;
        } catch (Exception e) {
            erreur.erreur("can't read data from launcher settings, catch " + e);
        }
        erreur.info("Create a Launcher for " + projectName);
    }


    public boolean iniSettings() {
        currentAppVersion = getCurrentAppVersion();
        lastAppVersion = getLastAppVersion();
        erreur.info(currentAppVersion + " " + lastAppVersion);
        return true;
    }
    public boolean downloadInGUIIfNeeded() {
        if (lastAppVersion == null) {
            erreur.alerte("Can't find last version:" + lastAppVersion);
        } else if (lastAppVersion.equals(currentAppVersion)) {
            erreur.info("No need to download, User have " + currentAppVersion + " that is last version");
        } else {
            erreur.alerte("Download game from " + currentAppVersion + " to " + lastAppVersion);
            downloadGame(lastAppVersion);
            currentAppVersion = lastAppVersion;
        }
        return true;
    }

    private String getDownloadURL(String version) {
        return "https://github.com/" + userName + "/" + projectName + "/releases/download/" + version + "/" + projectName + ".jar";
    }

    /**
     * {@summary Download the game at given version.}<br>
     * If needed it will download JRE
     * 
     * @param version the version to download game at
     * @return true if it work
     */
    public boolean downloadGame(String version) {
        erreur.info("download " + projectName + " " + version + " from " + getDownloadURL(version) + " to " + getJarPath());
        getProgression().iniLauncher();
        File fi = new File(getFolderGameJar());
        fi.mkdirs();
        boolean itWork = fichier.download(getDownloadURL(version), getJarPath(), true);
        getProgression().setDownloadingValue(100);
        return itWork;
    }


    /**
     * {@summary launch the game with selected version.}<br>
     * Version can be set before call launchGame() with setVersion(String version).
     * 
     * @return true if we need to do launch() again
     */
    public boolean launchGame() {
        // set up the command and parameter
        String jvmConfig = getJVMConfig();
        String[] javaArgs = null;
        if (jvmConfig != null) {
            javaArgs = jvmConfig.split("\n")[0].split(" ");
        }
        if (javaArgs == null || javaArgs.length == 0) {
            javaArgs = new String[0];
        }
        List<String> args;
        if (justGetVersion) {
            args = new ArrayList<>();
            args.add("--version");
        } else {
            if (this.args != null && !this.args.isEmpty() && this.args.get(0) != null && this.args.get(0).length() > 0) {
                args = this.args;
            } else {
                args = new ArrayList<>();
            }
            args.add("-launchFromLauncher");
        }
        try {
            String[] cmd = new String[3 + args.size() + javaArgs.length];
            int k = 0;
            cmd[k++] = getJavaCommand();
            for (String arg : javaArgs) {
                if (arg != null) {
                    cmd[k++] = arg;
                }
            }
            cmd[k++] = "-jar";
            cmd[k++] = getJarPath();
            for (String arg : args) {
                if (arg != null) {
                    cmd[k++] = arg;
                }
            }

            erreur.info("command launch: ");
            for (String s : cmd) {
                erreur.print(s + " ");
            }
            erreur.println();
            // create runtime to execute external command
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd));

            if (Os.getOs().isMac()) {
                pb.directory(new File(System.getProperty("user.home")));
            }

            File parentLog = new File(getPathToTemporaryFolder());
            parentLog.mkdirs();
            if (Main.logToFile && parentLog.exists()) {
                File fout = new File(getPathToTemporaryFolder() + "log.txt");
                try {
                    pb.redirectOutput(Redirect.appendTo(fout));
                    erreur.info("All info, error & alerte are redirected to " + fout.getCanonicalPath());
                    erreur.info("Launch with -logToFile=false to have info, error & alerte in console");
                } catch (Exception e) {
                    erreur.alerte("Fail to redirectOutput to log file.");
                    pb.redirectOutput(Redirect.INHERIT);
                }
            } else {
                pb.redirectOutput(Redirect.INHERIT);
            }
            pr = pb.start();

            handleControlC();

        } catch (Exception e) {
            System.out.println("[ERROR] An error ocurre in launcher.");
            e.printStackTrace();
        }
        erreur.info("wait for the end of the Process");
        try {
            pr.waitFor();
        } catch (InterruptedException e) {
            erreur.erreur("Process have been interrupted");
        }
        int exitValue = pr.exitValue();
        erreur.info("exit code " + exitValue);

        switch (exitValue) {
            case 100: {
                return true; // restart game
            }
            default: {
                return false;
            }
        }
    }

    /**
     * {@summary launch the game while it ask to restart.}
     */
    public void launchGameWithRestart() {
        while (launchGame()) {
            erreur.info("Restarting game");
        }
    }

    private String getPathToTemporaryFolder() { return getFolderTemporary(); }
    private String getPathToJarFolder() { return getFolderGameJar(); }
    /**
     * {@summary Give path to projectName.jar.}<br>
     * 
     * @return path to projectName.jar depending of the Os
     */
    public String getJarPath() { return getPathToJarFolder() + projectName + ".jar"; }


    /**
     * {@summary Give path to execute java.}<br>
     * 
     * @return path to our java version depending of the Os
     */
    public String getJavaCommand() {
        String javaCmd = null;
        String pathToJava = getPathToLauncherFilesRuntime() + "bin/java";
        if (Os.getOs().isWindows()) {
            File f = new File(pathToJava + ".exe");
            if (f.exists()) {
                javaCmd = f.toString();
            }
        } else if (Os.getOs().isLinux() || Os.getOs().isMac()) {
            File f = new File(pathToJava);
            if (f.exists()) {
                javaCmd = f.toString();
            }
        }
        if (javaCmd != null && makeExecutable(Paths.get(javaCmd))) {
            return javaCmd;
        } else {
            erreur.alerte("Can't find Java (JRE) at " + pathToJava + ". You need to have a compatible Java version installed");
        }
        return "java";
    }

    /**
     * {@summary Give path to launcher files runtime.}<br>
     * 
     * @return path launcher files depending of the Os
     */
    public String getPathToLauncherFilesRuntime() {
        if (Os.getOs().isMac()) {
            return getPathToLauncherFiles() + "runtime/Contents/Home/";
        } else {
            return getPathToLauncherFiles() + "runtime/";
        }
    }
    /***
     * {@summary Give path to launcher files ressources.}<br>
     * 
     * @return path launcher files depending of the Os
     */
    public String getPathToLauncherFilesApp() { return getPathToLauncherFiles() + "app/"; }
    /**
     * {@summary Give path to launcher files.}<br>
     * 
     * @return path launcher files depending of the Os
     */
    public String getPathToLauncherFiles() {
        if (Os.getOs().isWindows()) {
            return System.getenv("ProgramFiles") + "/" + projectName + "/";
        } else if (Os.getOs().isLinux()) {
            return "/opt/" + projectName.toLowerCase() + "/lib/";
        } else if (Os.getOs().isMac()) {
            return "/Applications/" + projectName.toLowerCase() + ".app/Content/";
        }
        return "";
    }

    /**
     * {@summary Give args to execute java.}<br>
     * Args are download as ressources &#38; can be find in .../app/jvm.config
     * 
     * @return args for the JVM
     */
    public String getJVMConfig() {
        File f = new File(getPathToLauncherFilesApp() + "jvm.config");
        if (f.exists()) {
            return ReadFile.readFile(f);
        }
        return null;
    }

    /**
     * {@summary Try to make a file executable.}
     */
    private boolean makeExecutable(Path path) {
        if (Files.isExecutable(path)) {
            return true;
        }
        fichier.setMaxPermRecursively(new File(path.toString()));
        return Files.isExecutable(path);
    }

    /**
     * {@summary This handler will be called on Control-C pressed.}
     * cf http://vkroz.github.io/posts/20170630-Java-interrupt-hook.html
     */
    private void handleControlC() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            /**
             * {@summary This handler will be called on Control-C pressed.}
             */
            public void run() {
                System.out.println("Closing from launcher");
                if (pr != null) {
                    pr.destroy();
                }
            }
        });
    }
    /**
     * {@summary It launch the game with args --version then get it from the log.}
     * 
     * @return current jar version
     */
    private String getCurrentAppVersion() {
        justGetVersion = true;
        launchGame();
        justGetVersion = false;
        File fout = new File(getPathToTemporaryFolder() + "log.txt");
        String lastLine = "";
        for (String line : ReadFile.readFileList(fout)) {
            if (line.length() > 1 && line.charAt(0) != '[') {
                lastLine = line;
            }
        }
        return lastLine.strip();
    }
    /**
     * {@summary It download the infos off last stable version &#38; return tag_name value.}
     * 
     * @return last aviable jar version
     */
    private String getLastAppVersion() {
        File fi = new File(getPathToTemporaryFolder());
        fi.mkdirs();
        File ftemp = new File(getPathToTemporaryFolder() + "temp.json");
        String url = "https://api.github.com/repos/" + userName + "/" + projectName + "/releases/latest";
        try {
            fichier.download(url, ftemp.getCanonicalPath());
            return getXVersion(Paths.get(ftemp.getCanonicalPath()), "tag_name");
        } catch (IOException e) {
            erreur.erreur("Fail to download lastVersionInfo");
        }
        return null;
    }

    /**
     * {@summary Return the version from path &#38; name of the wanted version.}<br>
     * If it fail, it will return a defaut version.
     * 
     * @param pathToJson       path to the .json file taht containt version
     * @param nameOfTheVersion name of the version
     * @return a version String as 1.49.12
     */
    public String getXVersion(Path pathToJson, String nameOfTheVersion) {
        try {
            Reader reader = Files.newBufferedReader(pathToJson);
            JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
            String version = (String) parser.get(nameOfTheVersion);
            if (version == null) {
                erreur.alerte("can't read " + nameOfTheVersion + " version");
            }
            return version;
        } catch (Exception e) {
            erreur.alerte("can't read " + nameOfTheVersion + " version");
            return "0.0.0";
        }
    }

    private String getMainFolder() {
        if (mainFolder == null) {
            if (Os.getOs().isWindows()) {
                mainFolder = System.getenv("APPDATA");
            } else {
                mainFolder = System.getProperty("user.home");
            }
            mainFolder += "/." + projectName.toLowerCase() + "/";
        }
        return mainFolder;
    }

    private String getFolderGameJar() { return getMainFolder() + "game/"; }
    private String getFolderTemporary() { return getMainFolder() + "temp/"; }

    /**
     * {@summary Getter with lazy initialization.}<br>
     */
    public Progression getProgression() {
        if (progression == null) {
            if (gui) {
                progression = new ProgressionGUI();
            } else {
                progression = new ProgressionCLI();
            }
        }
        return progression;
    }
    /**
     * {@summary Simple CLI view that are update as a progression for downloading files.}<br>
     */
    class ProgressionCLI implements Progression {
        @Override
        public void iniLauncher() { fichier.setProgression(this); }
        @Override
        public void setDownloadingMessage(String message) { System.out.println("Dowload: " + message); }
        @Override
        public void setDownloadingValue(int value) { System.out.println(value + "% done"); }
    }
}
