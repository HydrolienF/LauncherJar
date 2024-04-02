package fr.formiko.launcherjar;

import java.util.ArrayList;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(args));
        // Launcher launcher = new Launcher(list, "HydrolienF", "Kokcinelo");
        // Launcher launcher = new Launcher(list, "00-Evan", "shattered-pixel-dungeon");
        // Launcher launcher = new Launcher(list, "LonamiWebs", "Klooni1010");
        // Launcher launcher = new Launcher(list, "MrStahlfelge", "lightblocks");
        // Launcher launcher = new Launcher(list, "Quillraven", "Quilly-s-Adventure");
        // Launcher launcher = new Launcher(list, "Sesu8642", "FeudalTactics");
        // Launcher launcher = new Launcher(list, "HydrolienF", "Infanlaboro");
        Launcher launcher = new Launcher(list, "aehmttw", "Tanks");
        launcher.iniSettings();
        launcher.downloadInGUIIfNeeded(); // GUI will be hide when download will be over
        launcher.launchGameWithRestart();
        launcher.saveSettings();
    }
}
