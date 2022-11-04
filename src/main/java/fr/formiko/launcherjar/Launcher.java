package fr.formiko.launcherjar;

import fr.formiko.usual.Folder;
import fr.formiko.usual.Os;
import fr.formiko.usual.Progression;
import fr.formiko.usual.ReadFile;
import fr.formiko.usual.erreur;
import fr.formiko.usual.fichier;
import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Launcher {
    private Process pr;
    private String userName = "HydrolienF";
    private String projectName = "Formiko";
    private String projetLauncherName = projectName + "Launcher";
    private List<String> args;
    private Progression progression;

    public Launcher(List<String> args) {
        pr = null;
        this.args = args;
    }


    public boolean iniSettingsIfNeeded() {
        // TODO
        return true;
    }
    public boolean downloadInGUIIfNeeded() {
        // TODO
        return true;
    }

    private String getDownloadURL(String version) {
        return "https://github.com/" + userName + "/" + projectName + "/releases/download/" + version + "/" + projectName + ".jar";
    }


    /**
     * {@summary launch the game with selected version.}<br>
     * Version can be set before call launchGame() with setVersion(String version).
     * 
     * @return true if we need to do launch() again
     * @lastEditedVersion 0.1
     */
    public boolean launchGame() {
        // set up the command and parameter
        String jvmConfig = getJVMConfig();
        String javaArgs[] = null;
        if (jvmConfig != null) {
            javaArgs = jvmConfig.split("\n")[0].split(" ");
        }
        if (javaArgs == null || javaArgs.length == 0) {
            javaArgs = new String[0];
        }
        args.add("-launchFromLauncher");
        try {
            String[] cmd = new String[3 + args.size() + javaArgs.length - 1];
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

            erreur.info("commande launch: ");// @a
            for (String s : cmd) {
                erreur.print(s + " ");// @a
            }
            erreur.println();// @a
            // create runtime to execute external command
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd));
            // .inheritIO();
            // if (Main.launchWithBash && Os.getOs().isMac()) {
            // String t[] = new String[4];
            // t[0] = "/bin/bash";
            // t[1] = "-l";
            // t[2] = "-c";
            // t[3] = "\"";
            // boolean first = true;
            // for (String s : cmd) {
            // if (!first) {
            // t[3] += " ";
            // first = false;
            // }
            // t[3] += s;
            // }
            // t[3] += "\"";
            // erreur.info("commande launch on mac: ");// @a
            // for (String s : t) {
            // erreur.print(s + " ");
            // }
            // erreur.println();
            // pb = new ProcessBuilder(Arrays.asList(t));
            // }

            if (Os.getOs().isMac()) {
                pb.directory(new File(System.getProperty("user.home")));
            }

            File parentLog = new File(Folder.getFolder().getFolderTemporary());
            parentLog.mkdirs();
            if (Main.logToFile && parentLog.exists()) {
                File fout = new File(Folder.getFolder().getFolderTemporary() + "log.txt");
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
        switch (pr.exitValue()) {
        // case 2: {
        // userWantToDownloadNextVersion = true;
        // return true;
        // }
        // default: {
        // erreur.info("exit code " + pr.exitValue());
        // return false;
        // }
        }
        return false; // don't restart launcher
    }


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
        } else if (Os.getOs().isLinux()) {
            File f = new File(pathToJava);
            if (f.exists()) {
                javaCmd = f.toString();
            }
        } else if (Os.getOs().isMac()) {
            File f = new File(pathToJava);
            if (f.exists()) {
                javaCmd = f.toString();
            }
        }
        if (javaCmd != null && makeExecutable(Paths.get(javaCmd))) {
            return javaCmd;
        } else {
            erreur.alerte("Can't execute " + javaCmd);
        }
        return javaCmd;
    }

    /**
     * {@summary Give path to launcher files runtime.}<br>
     * 
     * @return path launcher files depending of the Os
     */
    public String getPathToLauncherFilesRuntime() {
        if (Os.getOs().isWindows()) {
            return System.getenv("ProgramFiles") + "/" + projetLauncherName + "/runtime/";
        } else if (Os.getOs().isLinux()) {
            return "/opt/" + projetLauncherName + "/lib/runtime/";
        } else if (Os.getOs().isMac()) {
            return "/Applications/" + projetLauncherName + ".app/Contents/runtime/Contents/Home/";
        }
        return "";
    }
    /**
     * {@summary Give path to launcher files ressources.}<br>
     * 
     * @return path launcher files depending of the Os
     * @lastEditedVersion 1.0
     */
    public String getPathToLauncherFilesApp() {
        if (Os.getOs().isWindows()) {
            return System.getenv("ProgramFiles") + "/" + projetLauncherName + "/app/";
        } else if (Os.getOs().isLinux()) {
            return "/opt/" + projetLauncherName + "/lib/app/";
        } else if (Os.getOs().isMac()) {
            return "/Applications/" + projetLauncherName + ".app/Content/app/";
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
     * {@summary Give path to Formiko.jar.}<br>
     * 
     * @return path to Formiko.jar depending of the Os
     */
    // TODO point to downloaded .jar
    public String getJarPath() { return "C:/Users/lili5/git/LauncherJar/Kokcinelo.jar"; }

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
     * {@summary Getter with lazy initialization.}<br>
     * 
     * @lastEditedVersion 1.0
     */
    public Progression getProgression() {
        if (progression == null) {
            progression = new ProgressionCLI();
        }
        return progression;
    }
    class ProgressionCLI implements Progression {
        @Override
        public void iniLauncher() { fichier.setProgression(this); }
        @Override
        public void setDownloadingMessage(String message) { System.out.println("Dowload: " + message); }
        @Override
        public void setDownloadingValue(int value) { System.out.println(value + "% done"); }
    }
}
