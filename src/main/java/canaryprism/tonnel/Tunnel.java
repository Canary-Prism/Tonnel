package canaryprism.tonnel;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import javax.swing.JFrame;

import canaryprism.audio.AudioPlayer;
import canaryprism.audio.Music;
import canaryprism.timing.Conductor;
import canaryprism.tonnel.scoring.PlayerInputHandler;
import canaryprism.tonnel.swing.TunnelView;

public class Tunnel {

    private static final long barely_range = 210, hit_range = 120;

    private final Music first, music;
    private final AudioPlayer normal_music, muffled_music;
    private final long first_millis;


    private final Conductor music_loop_conductor;
    private final Conductor conductor, score_conductor, tunnel_noise_conductor;

    private final AudioPlayer cowbell, voice_one, voice_two, tunnel_noise;

    private final String sprite_path;

    private final JFrame frame;
    private volatile TunnelView view;
    private volatile PlayerInputHandler input_handler;

    public Tunnel(JFrame frame, Music first, long first_millis, int first_beats, Music music, String sprite_path, String audio_path) {
        this.frame = frame;

        this.first = first;
        this.first_millis = first_millis;
        this.music = music;

        this.normal_music = music.music();
        this.muffled_music = normal_music.clone();

        normal_music.setVolume(.6f);
        muffled_music.setVolume(.3f);

        this.sprite_path = sprite_path;

        this.cowbell = new AudioPlayer(Tunnel.getResource(audio_path + "/cowbell.wav"), 2);
        this.voice_one = new AudioPlayer(Tunnel.getResource(audio_path + "/one.wav"), 2);
        this.voice_two = new AudioPlayer(Tunnel.getResource(audio_path + "/two.wav"), 2);
        this.tunnel_noise = new AudioPlayer(Tunnel.getResource(audio_path + "/tunnel.wav"), 2);

        tunnel_noise.setVolume(.6f);

        this.tunnel_noise_conductor = new Conductor(2_750, 1);
        tunnel_noise_conductor.submit((e) -> {
            tunnel_noise.play();
        });

        // try {
        //     var ais = AudioSystem.getAudioInputStream(Tunnel.getResource(audio_path + "/tunnel.wav"));
        //     this.tunnel_noise = AudioSystem.getClip();
        //     tunnel_noise.open(ais);
        //     setTunnelNoiseVolume(0);
        //     tunnel_noise.loop(Clip.LOOP_CONTINUOUSLY);
        // } catch (Exception e) {
        //     throw new RuntimeException(e);
        // }

        this.conductor = new Conductor(60_000, music.bpm());
        conductor.submit((e) -> {
            var i = e.beat();

            if (i < first_beats) {
                if ((i & 1) == 0) {
                    voice_one.play();
                } else {
                    voice_two.play();
                }
            } else if (auto) {
                playerInput();
            }
        });

        conductor.submit((e) -> {
            tunnel_timer--;
            if (tunnel_timer == 0) {
                in_tunnel = !in_tunnel;

                if (in_tunnel) {
                    tunnel_timer = (int)(Math.random() * 10) + 2;
                    enterTunnel();
                } else {
                    tunnel_timer = (int)(Math.random() * 40) + 2;
                    exitTunnel();
                }
            }
        });

        this.score_conductor = new Conductor(1_000, 60);
        score_conductor.submit((e) -> {
            score++;
            view.setScore(score);
            background_switch_timer--;
            if (background_switch_timer == 0) {
                view.queueNextBackground();
                background_switch_timer = 2000;
            }
        });

        conductor.submit((e) -> {
            var i = e.beat();

            switch (i) {
                case 0 -> view.startScoreScroll(20);
                case 1 -> score_conductor.start(0);
                case 7 -> input_handler.start(Math.round(60_000d / music.bpm()));
            }
        });

        this.music_loop_conductor = new Conductor(60_000 * music.beatDuration(), music.bpm());
        music_loop_conductor.submit((e) -> {
            if (in_tunnel) {
                muffled_music.play();
            } else {
                normal_music.play();
            }
        });

    }

    
    private volatile int background_switch_timer = 1000;
    
    private volatile boolean in_tunnel = false;
    private volatile int tunnel_timer = (int) (Math.random() * 10) + 10;
    
    private volatile int score = 0;
    private volatile int highscore = 0;

    private volatile boolean auto = false;

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public void setHighScore(int highscore) {
        this.highscore = highscore;
    }

    private void exitTunnel() {
        var frame = muffled_music.getFramePosition();
        normal_music.play(frame);
        muffled_music.stop();
        // tunnel_noise.setVolume(0);
        tunnel_noise.stop();
        tunnel_noise_conductor.stop();
        view.exitTunnel();
    }

    private void enterTunnel() {
        var frame = normal_music.getFramePosition();
        muffled_music.play(frame);
        normal_music.stop();
        tunnel_noise_conductor.start(30);
        // tunnel_noise.setVolume(1);
        view.enterTunnel();
    }

    // private void setTunnelNoiseVolume(float volume) {
    //     var gain = (javax.sound.sampled.FloatControl) tunnel_noise
    //             .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
    //     gain.setValue(20f * (float) Math.log10(volume));
    // }

    private volatile CompletableFuture<Integer> future;

    public CompletableFuture<Integer> start() {
        
        setup();

        // tunnel_noise_conductor.start(0);

        first.music().play();

        // timer.schedule(new TimerTask() {
        //     @Override
        //     public void run() {
        //     }
        // });

        music_loop_conductor.start(first_millis);

        conductor.start(first.initialDelay());

        view.requestFocus();
        view.start();

        this.future = new CompletableFuture<>();

        return future;
    }

    private void setup() {
        input_handler = new PlayerInputHandler(this, 60_000, music.bpm(), barely_range, hit_range);

        view = new TunnelView(sprite_path);
        view.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
            }

            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    playerInput();
                }
            }
        
        });
        view.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                view.requestFocus();
                playerInput();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
            }
        });
        view.setHighScore(highscore);
        frame.add(view);
        frame.pack();
        frame.setVisible(true);
    }
  
    public void playerInput() {
        cowbell.play();
        view.hitCowbell();
        input_handler.playerInput();
    }

    public void barely() {
        view.barely(120);
    }
    public void fail() {
        view.fail();

        conductor.stop();
        music_loop_conductor.stop();
        muffled_music.stop();
        normal_music.stop();
        tunnel_noise.stop();
        tunnel_noise_conductor.stop();
        score_conductor.stop();
        input_handler.stop();

        future.complete(score);
    }

    public static InputStream getResource(String name) {
        return new BufferedInputStream(Tunnel.class.getResourceAsStream(name));
    }
}
