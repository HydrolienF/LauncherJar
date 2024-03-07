package fr.formiko.launcherjar;

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
        this((String) map.get("version"), (long) map.get("playedTime"), (long) map.get("lastTimePlayed"),
                (long) map.get("firstTimePlayed"));
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
