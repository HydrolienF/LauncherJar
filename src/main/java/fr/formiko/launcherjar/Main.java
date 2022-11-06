package fr.formiko.launcherjar;

import fr.formiko.usual.Usual;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static boolean logToFile;
    private static Launcher launcher;
    // private static boolean launchGame;
    public static void main(String[] args) {
        // launchGame = true;
        Usual.main(args);
        logToFile = true;
        ArrayList<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(args));
        launcher = new Launcher(list);
        launcher.iniSettings();
        launcher.downloadInGUIIfNeeded(); // GUI will be hide when download will be over
        launcher.launchGame();
    }
}