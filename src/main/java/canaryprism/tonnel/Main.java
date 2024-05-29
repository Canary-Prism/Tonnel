package canaryprism.tonnel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.JFrame;

import canaryprism.audio.ClipAudioPlayer;
import canaryprism.audio.BufferedAudioPlayer;
import canaryprism.audio.Music;

/**
 * Hello world!
 *
 */
public class Main {

    static boolean hasArg(String[] args, String key) {
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i].equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {

        // var kyuukurarin = new Music(
        //     new BufferedAudioPlayer(Tunnel.getResource("/tonnel_assets/music/きゅうくらりん.wav"), 1), 
        //     2250, 
        //     600_000,
        //     2200,
        //     1000
        // );

        var lockstep = new Music(
            new BufferedAudioPlayer(Tunnel.getResource("/tonnel_assets/music/lockstep1.wav"), 1), 
            550, 
            162, 
            1000
        );

        var intro = new Music(
            new ClipAudioPlayer(Tunnel.getResource("/tonnel_assets/music/tunnel_start.wav"), 1),
            740,
            102,
            8
        );
        var music = new Music(
            new BufferedAudioPlayer(Tunnel.getResource("/tonnel_assets/music/tunnel.wav"), 2),
            (long)(60_000d * 17.5 / 204) - 765,
            102,
            64
        );

        String working_directory;

        // here, we assign the name of the OS, according to Java, to a variable...
        String OS = (System.getProperty("os.name")).toUpperCase();
        if (OS.contains("WIN")) {
            working_directory = System.getenv("AppData");
        } else {
            // in either case, we would start in the user's home directory
            working_directory = System.getProperty("user.home");
            // if we are on a Mac, we are not done, we look for "Application Support"
            working_directory += "/Library/Application Support";
        }

        working_directory += "/Tonnel";
        // we are now free to set the working_directory to the subdirectory that is our
        // folder.
        
        var high_score = getHighScore(working_directory);

        var frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final var intro_delay = (long)(60_000d * 17.5 / 204) - 40;

        var tunnel = new Tunnel(frame, intro, intro_delay, 8, music, "/tonnel_assets/sprites", "/tonnel_assets/audio");
        tunnel.setHighScore(high_score);
        if (hasArg(args, "--auto")) {
            tunnel.setAuto(true);
        }

        var score = tunnel.start().join();



        if (score > high_score) {
            write(working_directory, score);
        }
    }

    private static void write(String path, int score) {
        try {
            Files.writeString(new File(path + "/save").toPath(), Integer.toString(score));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getHighScore(String path) {
        var folder = new File(path);
        if (!folder.exists()) 
            folder.mkdirs();

        var save_file = new File(path + "/save");

        if (!save_file.exists()) {
            try {
                save_file.createNewFile();
                Files.writeString(save_file.toPath(), "0");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            return Integer.parseInt(Files.readString(save_file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
