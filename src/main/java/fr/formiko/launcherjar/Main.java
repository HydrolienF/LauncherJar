package fr.formiko.launcherjar;

import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static boolean logToFile;
    public static void main(String[] args) {
        // launchGame = true;
        logToFile = true;
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(args));
        Launcher launcher = new Launcher(list);
        launcher.iniSettings();
        launcher.downloadInGUIIfNeeded(); // GUI will be hide when download will be over
        launcher.launchGameWithRestart();
    }
}