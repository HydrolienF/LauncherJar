package fr.formiko.launcherjar;

import java.util.ArrayList;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(args));
        Launcher launcher = new Launcher(list, "HydrolienF", "Kokcinelo");
        // Launcher launcher = new Launcher(list, "00-Evan", "shattered-pixel-dungeon");
        launcher.iniSettings();
        launcher.downloadInGUIIfNeeded(); // GUI will be hide when download will be over
        launcher.launchGameWithRestart();
        launcher.saveSettings();
    }
}
