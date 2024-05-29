package canaryprism.audio;

public interface AudioPlayer extends AutoCloseable {
    void play();

    void stop();

    void setVolume(float volume);
}
