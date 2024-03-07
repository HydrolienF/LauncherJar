package fr.formiko.launcherjar;

import fr.formiko.usual.erreur;
import java.util.Map;

public class GameData {
    // (String version, int playedTime, long lastTimePlayed, long firstTimePlayed)
    private String version;
    private long playedTime;
    private long lastTimePlayed;
    private long firstTimePlayed;
    public GameData(String version, long playedTime, long lastTimePlayed, long firstTimePlayed) {
        this.version = version;
        this.playedTime = playedTime;
        this.lastTimePlayed = lastTimePlayed;
        this.firstTimePlayed = firstTimePlayed;
    }
    public GameData(Map<String, Object> map) {
        this((String) map.get("version"), toLong(map.get("playedTime")), toLong(map.get("lastTimePlayed")),
                toLong(map.get("firstTimePlayed")));
    }

    /**
     * @MGMU approved
     */
    public static long toLong(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        erreur.erreur("Not a number: " + o);
        return 0L;
    }

    public String getVersion() { return version; }
    public long getPlayedTime() { return playedTime; }
    public long getLastTimePlayed() { return lastTimePlayed; }
    public long getFirstTimePlayed() { return firstTimePlayed; }
    public void setVersion(String version) { this.version = version; }
    public void setPlayedTime(long playedTime) { this.playedTime = playedTime; }
    public void setLastTimePlayed(long lastTimePlayed) { this.lastTimePlayed = lastTimePlayed; }
    public void setFirstTimePlayed(long firstTimePlayed) { this.firstTimePlayed = firstTimePlayed; }

    public Map<String, Object> toMap() {
        return Map.of("version", version, "playedTime", playedTime, "lastTimePlayed", lastTimePlayed, "firstTimePlayed", firstTimePlayed);
    }
}
