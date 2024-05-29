package canaryprism.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class BufferedAudioPlayer implements AudioPlayer {
    private final SourceDataLine[] lines;
    private final ExecutorService ex;
    private volatile int rotation = 0;

    private final byte[] data;

    public BufferedAudioPlayer(InputStream data, int concurrents) {
        var baos = new ByteArrayOutputStream();
        this.lines = new SourceDataLine[concurrents];
        this.ex = Executors.newFixedThreadPool(concurrents);
        try {
            data.transferTo(baos);
            this.data = baos.toByteArray();

            var ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.data));
            var format = ais.getFormat();
            for (int i = 0; i < concurrents; i++) {
                lines[i] = AudioSystem.getSourceDataLine(null);
                lines[i].open(format, 4096);
                lines[i].start();
            }
            ais.close();

            // for (int i = 0; i < concurrents; i++) {
            //     var clip = AudioSystem.getClip();
            //     // System.out.println(clip.getClass());
            //     // clip.open(null, null, concurrents, i);
            //     clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.data)));
            //     lines[i] = clip;
            // }
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVolume(float volume) {
        for (var line : lines) {
            var gain = (javax.sound.sampled.FloatControl) line
                    .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            gain.setValue(20f * (float) Math.log10(volume));
            // System.out.println("volume set");
            // var gain = (javax.sound.sampled.FloatControl) line
            //         .getControl(javax.sound.sampled.FloatControl.Type.VOLUME);
            // gain.setValue(volume);
        }
    }

    private static final int BUFFER_SIZE = 2048;


    @Override
    public void play() {
        var line = lines[rotation];
        if (line.isActive()) {
            System.err.println("Warning: player rotation saturated");
            return;
        }
        ex.submit(() -> {
            line.start();
            var is = new ByteArrayInputStream(data);
            byte[] bufferBytes = new byte[BUFFER_SIZE];
            int readBytes = -1;
            try {
                while ((readBytes = is.read(bufferBytes)) != -1) {
                    line.write(bufferBytes, 0, readBytes);
                    if (Thread.interrupted()) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            line.drain();
            line.stop();
        });
        // clip.setFramePosition(frame);
        rotation++;
        if (rotation == lines.length)
            rotation = 0;
    }

    @Override
    public void stop() {
        for (var clip : lines) {
            clip.stop();
        }
    }

    @Override
    public BufferedAudioPlayer clone() {
        return new BufferedAudioPlayer(new ByteArrayInputStream(this.data), lines.length);
    }

    @Override
    public void close() {
        for (var line : lines) {
            line.close();
        }
        ex.shutdown();
    }
}