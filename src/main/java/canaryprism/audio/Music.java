package canaryprism.audio;

public record Music(AudioPlayer music, long initialDelay, long time, long beats, long beatDuration) {
    public Music(AudioPlayer music, long initialDelay, long bpm, long beatDuration) {
        this(music, initialDelay, 60_000, bpm, beatDuration);
    }
}
