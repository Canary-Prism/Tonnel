package canaryprism.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class ClipAudioPlayer implements AudioPlayer {
    private final Clip[] clips;
    private volatile int rotation = 0;

    private final byte[] data;

    public ClipAudioPlayer(InputStream data, int concurrents) {
        var baos = new ByteArrayOutputStream();
        this.clips = new Clip[concurrents];
        try {
            data.transferTo(baos);
            this.data = baos.toByteArray();
            for (int i = 0; i < concurrents; i++) {
                var clip = AudioSystem.getClip();
                // System.out.println(clip.getClass());
                // clip.open(null, null, concurrents, i);
                clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.data)));
                clips[i] = clip;
            }
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVolume(float volume) {
        for (var clip : clips) {
            var gain = (javax.sound.sampled.FloatControl) clip
                    .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            gain.setValue(20f * (float) Math.log10(volume));
        }
    }

    public int getFramePosition() {
        if (rotation == 0) {
            return clips[clips.length - 1].getFramePosition();
        }
        return clips[rotation - 1].getFramePosition();
    }

    @Override
    public void play() {
        this.play(0);
    }

    public void play(int frame) {
        var clip = clips[rotation];
        if (clip.isActive()) {
            System.err.println("Warning: player rotation saturated");
            clip.stop();
        }
        clip.setFramePosition(frame);
        clip.start();
        rotation++;
        if (rotation == clips.length)
            rotation = 0;
    }

    @Override
    public void stop() {
        for (var clip : clips) {
            clip.stop();
        }
    }

    @Override
    public ClipAudioPlayer clone() {
        return new ClipAudioPlayer(new ByteArrayInputStream(this.data), clips.length);
    }

    @Override
    public void close() {
        for (var clip : clips) {
            clip.close();
        }
    }
}
