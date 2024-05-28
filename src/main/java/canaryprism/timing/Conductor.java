package canaryprism.timing;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class Conductor {
    private Timer timer = new Timer();

    public Conductor(long millis, long beats) {
        var gcd = gcd(millis, beats);
        millis /= gcd;
        beats /= gcd;

        this.small_loop = beats;
        this.long_delay = millis;
        this.short_delay = Math.round(((double) millis) / beats);

        // delay_sequence = new long[(int) beats];

        // var avg_delay = millis / beats;

        // for (int i = 0; i < beats - 1; i++) {
        // delay_sequence[i] = avg_delay;
        // }
        // delay_sequence[(int) beats - 1] = millis - avg_delay * (beats - 1);

        // System.out.println(Arrays.toString(delay_sequence));
    }

    private long small_loop;
    private long long_delay;
    private long short_delay;

    private ArrayList<ConductorTimerTask> tasks = new ArrayList<>();
    private volatile boolean running = false;

    public void start(long initial_delay) {
        if (running) {
            return;
        }
        running = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < tasks.size(); i++) {
                    tasks.set(i, (ConductorTimerTask) tasks.get(i).clone());
                }

                for (int i = 0; i < tasks.size(); i++) {
                    var task = tasks.get(i);
                    timer.scheduleAtFixedRate(task, 0, short_delay);
                }
            }
        }, initial_delay, long_delay);
        // for (int i = 0; i < tasks.size(); i++) {
        // var task = tasks.get(i);
        // timer.scheduleAtFixedRate(task, 0, short_delay);
        // }
    }

    public void submit(Consumer<ConductorInfo> r) {
        if (running) {
            throw new IllegalStateException("Cannot submit tasks while the conductor is running");
        }
        tasks.add(new ConductorTimerTask(r, 0));
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        timer.cancel();
        timer = new Timer();
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    // private long[] delay_sequence;

    class ConductorTimerTask extends TimerTask {
        Consumer<ConductorInfo> r;
        int beat;
        int loop = 0;

        ConductorTimerTask(Consumer<ConductorInfo> r, int beat) {
            this.r = r;
            this.beat = beat;
        }

        @Override
        public void run() {
            if (loop == small_loop) {
                cancel();
                return;
            }
            r.accept(new ConductorInfo(beat, timer));
            beat++;
            loop++;
        }

        @Override
        public Object clone() {
            return new ConductorTimerTask(r, beat);
        }
    }

    public record ConductorInfo(int beat, Timer timer) {
    }
}
