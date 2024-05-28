package canaryprism.tonnel.scoring;

import canaryprism.timing.Conductor;
import canaryprism.tonnel.Tunnel;

public class PlayerInputHandler {
    private final Conductor start_barely_conductor;
    private final Conductor start_hit_conductor;
    private final Conductor end_hit_conductor;
    private final Conductor end_barely_conductor;
    private final long barely_start, hit_start, hit_end, barely_end;

    private final Tunnel tunnel;

    public PlayerInputHandler(Tunnel tunnel, long millis, long beats, long barely_range, long hit_range) {
        this.tunnel = tunnel;

        this.start_barely_conductor = new Conductor(millis, beats);
        this.start_hit_conductor = new Conductor(millis, beats);
        this.end_hit_conductor = new Conductor(millis, beats);
        this.end_barely_conductor = new Conductor(millis, beats);

        start_barely_conductor.submit((e) -> {
            synchronized (ticklock) {
                current_score = Scoring.barely;
                // tunnel.playerInput();
            }
        });
        start_hit_conductor.submit((e) -> {
            synchronized (ticklock) {
                current_score = Scoring.hit;
            }
        });
        end_hit_conductor.submit((e) -> {
            synchronized (ticklock) {
                current_score = Scoring.barely;
            }
        });
        end_barely_conductor.submit((e) -> {
            synchronized (ticklock) {
                if (!has_tapped && !has_missed) {
                    missHit();
                    has_missed = true;
                }
                current_score = Scoring.miss;
                has_tapped = false;
                // tunnel.playerInput();
            }
        });

        this.barely_start = Math.floorDiv(barely_range, 2);
        this.hit_start = Math.floorDiv(hit_range, 2);
        this.hit_end = Math.ceilDiv(hit_range, 2);
        this.barely_end = Math.ceilDiv(barely_range, 2);
    }

    // private volatile boolean onbeat = true;
    private volatile Scoring current_score = Scoring.miss;

    private final Object ticklock = new Object();

    private volatile boolean has_tapped = false, has_missed = false;

    private volatile boolean running = false;

    private void missHit() {
        synchronized (ticklock) {
            tunnel.fail();
        }
    }

    public void start(long initial_delay) {
        start_barely_conductor.start(initial_delay - barely_start);
        start_hit_conductor.start(initial_delay - hit_start);
        end_hit_conductor.start(initial_delay + hit_end);
        end_barely_conductor.start(initial_delay + barely_end);
        running = true;
    }

    public void playerInput() {
        if (!running) {
            return;
        }
        synchronized (ticklock) {
            var score = this.current_score;
            if (has_tapped)
                return;

            has_tapped = true;

            switch (score) {
                case hit -> {
                    has_missed = false;
                }
                case barely -> {
                    has_missed = false;
                    tunnel.barely();
                }
                case miss -> {
                    tunnel.fail();
                }
            }
        }
    }

    public void stop() {
        start_barely_conductor.stop();
        start_hit_conductor.stop();
        end_hit_conductor.stop();
        end_barely_conductor.stop();
    }
}