package fr.formiko.launcherjar;

import fr.formiko.usual.Os;
import fr.formiko.usual.Progression;
import fr.formiko.usual.ReadFile;
import fr.formiko.usual.color;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;
import fr.formiko.utils.FLUFiles;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
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
    private Map<String, Map<String, GameData>> gamesData;
    private Yaml yaml;
    private long timeStarted;

    public Launcher(List<String> args, String userName, String projectName, boolean gui) {
        this.args = args;
        this.userName = userName;
        this.projectName = projectName;
        this.gui = gui;
        yaml = new Yaml();
        color.iniColor();
    }
    public Launcher(List<String> args, String userName, String projectName) { this(args, userName, projectName, true); }

    public Launcher(List<String> args) {
        pr = null;
        this.args = args;
        yaml = new Yaml();
        color.iniColor();
        try {
            initGameToLaunchSettings(this.getClass().getClassLoader().getResourceAsStream("settings.json"));
        } catch (Exception e) {
            erreur.erreur("can't read data from launcher settings in jar, catch " + e);
            try {
                initGameToLaunchSettings(new FileInputStream(getLauncherJarFolder() + "gameToLaunch.json"));
            } catch (Exception e2) {
                erreur.erreur("can't read data from launcher settings on ./launcherjar/, catch " + e2);
            }
        }
        erreur.info("Create a Launcher for " + projectName);
    }

    private void initGameToLaunchSettings(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
            e.printStackTrace();
        }
        erreur.info("Change launcher param for " + projectName);
    }


    public boolean iniSettings() {
        timeStarted = System.currentTimeMillis();
        gamesData = getGameDataMap();
        currentAppVersion = getCurrentAppVersion();
        lastAppVersion = getLastAppVersion();
        erreur.info(currentAppVersion + " " + lastAppVersion);
        return true;
    }
    public boolean saveSettings() {
        try {
            getCurrentGameData().setLastTimePlayed(System.currentTimeMillis());
            getCurrentGameData().setPlayedTime(getCurrentGameData().getPlayedTime() + (System.currentTimeMillis() - timeStarted));
            timeStarted = System.currentTimeMillis();

            File f = getDownloadedGamesDataFile();
            f.getParentFile().mkdirs();

            Map<String, Map<String, Map<String, Object>>> gamesDataToSave = new HashMap<>();
            for (Map.Entry<String, Map<String, GameData>> user : gamesData.entrySet()) {
                Map<String, Map<String, Object>> gamesData = new HashMap<>();
                for (Map.Entry<String, GameData> game : user.getValue().entrySet()) {
                    // gamesData.put(game.getKey(), game.getValue().toMap());
                    gamesData.put(game.getKey(), game.getValue().toMap());
                }
                gamesDataToSave.put(user.getKey(), gamesData);
            }
            String yamlString = yaml.dump(gamesDataToSave);

            Files.write(f.toPath(), yamlString.getBytes());
            erreur.info("Save settings");
            return true;
        } catch (Exception e) {
            erreur.erreur("Can't save settings because " + e);
            e.printStackTrace();
            return false;
        }
    }
    public boolean saveDefaultSettings() {
        getCurrentGameData().setFirstTimePlayed(System.currentTimeMillis());
        return saveSettings();
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
            // getCurrentGameData().setVersion(currentAppVersion);
            // Create a new GameData with the new version
            if (!gamesData.containsKey(userName)) {
                gamesData.put(userName, new HashMap<>());
            }
            gamesData.get(userName).put(projectName,
                    new GameData(currentAppVersion, 0, System.currentTimeMillis(), System.currentTimeMillis()));
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
            case 101: {
                // Launch game from new settings location.
                try {
                    saveSettings();
                    initGameToLaunchSettings(new FileInputStream(getLauncherJarFolder() + "gameToLaunch.json"));
                    Main.setFullRestart(true);
                } catch (Exception e) {
                    erreur.erreur("can't read data from launcher settings, catch " + e);
                }
                return false;
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
     * @return current jar version
     */
    private String getCurrentAppVersion() { return getCurrentGameData() != null ? getCurrentGameData().getVersion() : null; }
    private GameData getCurrentGameData() {
        if (gamesData.containsKey(userName) && gamesData.get(userName).containsKey(projectName)) {
            return gamesData.get(userName).get(projectName);
        } else {
            return null;
        }
    }
    /**
     * {@summary It download the infos off last stable version &#38; return tag_name value.}
     * 
     * @return last aviable jar version
     */
    private String getLastAppVersion() {
        String url = "https://api.github.com/repos/" + userName + "/" + projectName + "/releases/latest";
        String content = FLUFiles.readFileFromWeb(url);
        if (content == null || content.isEmpty()) {
            erreur.erreur("Fail to download lastVersionInfo");
            return null;
        }
        return getXVersion(content, "tag_name");
    }

    /**
     * {@summary Return the version from path &#38; name of the wanted version.}<br>
     * If it fail, it will return a defaut version.
     * 
     * @param content          content of the .json file that containt version
     * @param nameOfTheVersion name of the version
     * @return a version String as 1.49.12
     */
    public String getXVersion(String content, String nameOfTheVersion) {
        try {
            JsonObject parser = (JsonObject) Jsoner.deserialize(content);
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

    private String getAllGamesFolder() { return Os.getOs().isWindows() ? System.getenv("APPDATA") : System.getProperty("user.home"); }

    private String getLauncherJarFolder() { return getAllGamesFolder() + "/.launcherjar/"; }
    private File getLauncherJarFile(String fileName) {
        String fullFileName = getLauncherJarFolder() + fileName;
        File dataFile = new File(fullFileName);
        if (!dataFile.exists()) {
            if (FLUFiles.createFile(fullFileName)) {
                erreur.info("Create " + fileName);
            } else {
                erreur.erreur("Can't create " + fileName);
            }
        }
        return dataFile;
        // saveDefaultSettings();
    }
    private File getDownloadedGamesDataFile() { return getLauncherJarFile("downloadedGames.yml"); }
    private File getAvailableGamesDataFile() { return getLauncherJarFile("availableGames.yml"); }
    private File getGameToLaunchDataFile() { return getLauncherJarFile("gameToLaunch.json"); }
    private Map<String, Map<String, GameData>> getGameDataMap() {
        File downloadedGames = getDownloadedGamesDataFile();
        try (InputStream in = new FileInputStream(downloadedGames)) {
            // Map<String, Map<String, GameData>> listOfGames = yaml.load(in);
            Map<String, Map<String, Map<String, Object>>> listOfGamesIn = yaml.load(in);
            Map<String, Map<String, GameData>> listOfGames = new HashMap<>();
            for (Map.Entry<String, Map<String, Map<String, Object>>> user : listOfGamesIn.entrySet()) {
                Map<String, GameData> gamesData = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> game : user.getValue().entrySet()) {
                    gamesData.put(game.getKey(), new GameData(game.getValue()));
                }
                listOfGames.put(user.getKey(), gamesData);
            }
            return listOfGames;
        } catch (Exception e) {
            erreur.erreur("Can't read " + downloadedGames + " because " + e);
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private String getMainFolder() { return getAllGamesFolder() + "/." + projectName.toLowerCase() + "/"; }

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
