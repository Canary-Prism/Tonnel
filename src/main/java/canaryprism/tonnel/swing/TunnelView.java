package canaryprism.tonnel.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

import canaryprism.timing.Conductor;
import canaryprism.tonnel.swing.ConstantSprite.Background;
import canaryprism.tonnel.swing.ConstantSprite.CloudThing;
import canaryprism.tonnel.swing.ConstantSprite.Score;
import canaryprism.tonnel.swing.ConstantSprite.TunnelSprite;
import canaryprism.tonnel.swing.LitSprite.Car;
import canaryprism.tonnel.swing.LitSprite.Driver;
import canaryprism.tonnel.swing.LitSprite.DriverHands;

public class TunnelView extends JPanel {

    private final SpriteSet<ConstantSprite> sprite_set;

    private final SpriteSet<LitSprite> lit_sprite_set;
    private final SpriteSet<LitSprite> shadow_sprite_set;

    private final Conductor conductor = new Conductor(1_000, 60);

    public TunnelView(String sprite_path) {
        super(true);
        this.sprite_set = new SpriteSet<>(ConstantSprite.class, sprite_path);

        this.lit_sprite_set = new SpriteSet<>(LitSprite.class, sprite_path + "/lit");
        this.shadow_sprite_set = new SpriteSet<>(LitSprite.class, sprite_path + "/shadow");

        conductor.submit((e) -> {
            advanceFrame();
            repaint();
        });
    }

    private volatile double background_scroll = 0;
    private volatile Background background = Background.sea;
    private volatile Driver normal_driver = Driver.normal0;
    private volatile Driver driver = Driver.normal0;
    private volatile int flinch_timer = 0;

    private volatile CloudThing anger = CloudThing.one;
    private volatile int anger_timer = 0;
    
    private volatile Background next_background = Background.sea;

    private final int handmovementtimer = 60;
    private final double handmovement = 0.04;
    private volatile double driver_hand_movement;
    private volatile boolean hand_movement_mode = true;
    private volatile int hand_movement_timer = handmovementtimer / 2;

    private final int driverx = 15, drivery = 40;

    private final double[] tunnel_offsets = { 48, 32, 16, 0 };
    private volatile int tunnel_index = 0;

    private volatile double tunnel_offset = 0;

    private volatile boolean in_tunnel = false;

    private volatile int tunnel_timer = 0;
    private volatile TunnelMode tunnel_mode = TunnelMode.none;

    private final int[] stickx_list = { 90, 90, 105, 110, 115, 120, 125, 130, 130, 140, 140, 145, 150 };
    private final int[] sticky_list = { 120, 118, 105, 100, 100, 100, 105, 105, 105, 115, 115, 115, 120 };

    private volatile boolean stick_hit = false;

    private volatile int cowbell_hit_timer = 0;
    private volatile double cowbell_offset = 0;

    private volatile int stick_index = 0;

    private volatile int stickx = 150;
    private volatile int sticky = 120;



    private enum TunnelMode {
        none, enter, exit, in
    }

    private volatile int score;
    private volatile int highscore;


    private final Object viewlock = new Object();

    private final Color background_color = new Color(0x18b0a0);

    private final double scorey_start = 0;
    private final double scorey_end = 32;
    private volatile double scorey = scorey_start;

    private volatile double car_rotate;
    private volatile int car_rotate_timer = -1;

    private volatile double score_scroll = 0;
    private volatile int score_scroll_amount = -1;

    private volatile int blink_index = 0;
    private volatile int blink_timer = 0;
    private void blink() {
        synchronized (viewlock) {
            blink_timer--;
            if (blink_timer <= 0) {
                normal_driver = switch (blink_index) {
                    case 0 -> Driver.normal0;
                    case 1 -> Driver.normal1;
                    case 2 -> Driver.normal2;
                    case 3 -> Driver.normal1;
                    case 4 -> Driver.normal0;
                    default -> Driver.normal0;
                };
                
                blink_timer = switch (normal_driver) {
                    case normal0 -> (int)(Math.random() * 60) + 30;
                    case normal1 -> 5;
                    case normal2 -> 5;
                    default -> throw new IllegalStateException();
                };
                blink_index++;
                if (blink_index == 5) {
                    blink_index = 0;
                }
            }
        }
    }

    private void advanceFrame() {
        synchronized (viewlock) {

            advance_stick: if (stick_hit) {
                if (stick_index == stickx_list.length) {
                    stick_hit = false;
                    break advance_stick;
                }
                stickx = stickx_list[stick_index];
                sticky = sticky_list[stick_index];
                stick_index++;
            }

            if (cowbell_hit_timer > 0) {
                cowbell_hit_timer--;
                cowbell_offset = Math.sin(cowbell_hit_timer * 1.5) * cowbell_hit_timer / 4;
            }


            if (car_rotate_timer >= 0) {
                if (car_rotate_timer > 0)
                    car_rotate_timer--;
                car_rotate = Math.sin(car_rotate_timer * .7) * car_rotate_timer * 0.002;


                if (car_rotate_timer == 0) {
                    driver = Driver.fail;

                    anger_timer--;
                    if (anger_timer <= 0) {
                        anger = CloudThing.values()[(anger.ordinal() + 1) % CloudThing.values().length];
                        anger_timer = (int)(Math.random() * 3) + 10;
                    }
                }

                return;
            }

            background_scroll -= 1.3;
            if (background_scroll < 0) {
                background_scroll = getSprite(background).getWidth(null);
            }

            if (hand_movement_mode) {
                driver_hand_movement += handmovement;
            } else {
                driver_hand_movement -= handmovement;
            }
            hand_movement_timer--;

            if (hand_movement_timer == 0) {
                hand_movement_timer = handmovementtimer;
                hand_movement_mode = !hand_movement_mode;
            }

            switch (tunnel_mode) {
                case none -> {}
                case enter -> {
                    tunnel_timer--;
                    tunnel_offset -= 30;

                    if (tunnel_timer == 0) {
                        tunnel_mode = TunnelMode.in;
                    }
                }

                case in -> {
                    tunnel_timer--;
                    if (tunnel_timer <= 0) {
                        tunnel_index++;
                        tunnel_timer = 2;
                    }
                    if (tunnel_index == tunnel_offsets.length) {
                        tunnel_index = 0;
                    }
                    tunnel_offset = tunnel_offsets[tunnel_index];
                }

                case exit -> {
                    tunnel_timer--;
                    tunnel_offset -= 30;

                    if (tunnel_timer == 0) {
                        tunnel_mode = TunnelMode.none;
                    }
                }
            }

            blink();

            if (flinch_timer > 0) {
                driver = Driver.flinch;
                flinch_timer--;
            } else {
                driver = normal_driver;
            }

            if (score_scroll == score_scroll_amount) {
                scorey = scorey_end;
            } else {
                if (score_scroll < score_scroll_amount) {
                    score_scroll++;
                }
                scorey = scorey_start + (scorey_end - scorey_start) * (score_scroll / score_scroll_amount);
            }

        }
    }

    public void enterTunnel() {
        synchronized (viewlock) {
            in_tunnel = true;
            tunnel_mode = TunnelMode.enter;
            tunnel_timer = 10;
            tunnel_offset = 256;
        }
    }

    public void exitTunnel() {
        synchronized (viewlock) {
            in_tunnel = false;
            tunnel_mode = TunnelMode.exit;
            tunnel_timer = 10;
            tunnel_offset = 0;
            if (next_background != background) {
                background = next_background;
            }
        }
    }

    public void startScoreScroll(int duration) {
        synchronized (viewlock) {
            score_scroll_amount = duration;
            score_scroll = 0;
        }
    }

    public void queueNextBackground() {
        synchronized (viewlock) {
            next_background = Background.values()[(next_background.ordinal() + 1) % Background.values().length];
        }
    }

    public void hitCowbell() {
        synchronized (viewlock) {
            stick_hit = true;
            stick_index = 0;
            cowbell_hit_timer = 10;
        }
    }

    public void setScore(int score) {
        synchronized (viewlock) {
            this.score = score;
            if (score > highscore) {
                highscore = score;
            }
        }
    }

    public void setHighScore(int highscore) {
        synchronized (viewlock) {
            this.highscore = highscore;
        }
    }

    public void barely(int duration) {
        synchronized (viewlock) {
            driver = Driver.flinch;
            flinch_timer = duration;
        }
    }

    public void fail() {
        synchronized (viewlock) {
            driver = Driver.anger;
            car_rotate_timer = 60;
        }
    }

    @Override
    protected void paintComponent(java.awt.Graphics g1) {
        super.paintComponent(g1);

        var g = (java.awt.Graphics2D) g1;


        g.setColor(background_color);
        g.fillRect(0, 0, getWidth(), getHeight());

        {
            var scale = Math.min(getWidth() / 200d, getHeight() / 256d);
            double screenx = getWidth() / 2 - (256 * scale / 2);
            double screeny = getHeight() / 2 - (256 * scale / 2);
            double screenw = 256 * scale;
            double screenh = 256 * scale;

            g.clipRect(round(screenx), round(screeny), round(screenw), round(screenh));
        }


        synchronized (viewlock) {
            var scale = Math.min(getWidth() / 200d, getHeight() / 256d);
            double screenx = getWidth() / 2 - (256 * scale / 2);
            double screeny = getHeight() / 2 - (256 * scale / 2);

            var background_sprite = getSprite(background);

            {
                double x = screenx - background_sprite.getWidth(null) * scale + background_scroll * scale;

                g.drawImage(background_sprite, round(x), round(screeny), round(background_sprite.getWidth(null) * scale), round(background_sprite.getHeight(null) * scale), null);

                x += background_sprite.getWidth(null) * scale;
    
                g.drawImage(background_sprite, round(x), round(screeny), round(background_sprite.getWidth(null) * scale), round(background_sprite.getHeight(null) * scale), null);
            }

            var tunnel_sprite = getSprite(TunnelSprite.tunnel);

            switch (tunnel_mode) {
                case none -> {}
                case exit -> {
                    g.drawImage(tunnel_sprite, round(screenx + tunnel_offset * scale), round(screeny), round(tunnel_sprite.getWidth(null) * scale), round(tunnel_sprite.getHeight(null) * scale), null);
                }
                case enter -> {
                    g.drawImage(tunnel_sprite, round(screenx + (tunnel_offset) * scale), round(screeny), round(tunnel_sprite.getWidth(null) * scale), round(tunnel_sprite.getHeight(null) * scale), null);
                    g.drawImage(tunnel_sprite, round(screenx + (tunnel_offset + tunnel_sprite.getWidth(null)) * scale), round(screeny), round(tunnel_sprite.getWidth(null) * scale), round(tunnel_sprite.getHeight(null) * scale), null);

                }

                case in -> {
                    g.drawImage(tunnel_sprite, round(screenx + (tunnel_offset - 128) * scale), round(screeny), round(tunnel_sprite.getWidth(null) * scale), round(tunnel_sprite.getHeight(null) * scale), null);
                    g.drawImage(tunnel_sprite, round(screenx + (tunnel_offset - 128 + tunnel_sprite.getWidth(null)) * scale), round(screeny), round(tunnel_sprite.getWidth(null) * scale), round(tunnel_sprite.getHeight(null) * scale), null);
                }
            }

            g.transform(AffineTransform.getTranslateInstance(getWidth(), getHeight()));
            g.transform(AffineTransform.getRotateInstance(car_rotate));
            g.transform(AffineTransform.getTranslateInstance(-getWidth(), -getHeight()));


            var car_body_sprite = getSprite(Car.body);

            g.drawImage(car_body_sprite, round(screenx), round(screeny), round(car_body_sprite.getWidth(null) * scale), round(car_body_sprite.getHeight(null) * scale), null);


            var lefthand_sprite = getSprite(DriverHands.lefthand);

            g.drawImage(lefthand_sprite, round(screenx + (driverx + 65) * scale), round(screeny + (drivery + 90 + driver_hand_movement) * scale), round(lefthand_sprite.getWidth(null) * scale), round(lefthand_sprite.getHeight(null) * scale), null);


            var driver_sprite = getSprite(driver);

            g.drawImage(driver_sprite, round(screenx + driverx * scale), round(screeny + drivery * scale), round(driver_sprite.getWidth(null) * scale), round(driver_sprite.getHeight(null) * scale), null);

            if (driver == Driver.fail) {
                var anger_sprite = getSprite(anger);

                g.drawImage(anger_sprite, round(screenx + (driverx + 60) * scale), round(screeny + (drivery - 10) * scale), round(anger_sprite.getWidth(null) * scale), round(anger_sprite.getHeight(null) * scale), null);
            }

            var wheel_sprite = getSprite(Car.wheel);

            g.drawImage(wheel_sprite, round(screenx + 160 * scale), round(screeny + 115 * scale), round(wheel_sprite.getWidth(null) * scale), round(wheel_sprite.getHeight(null) * scale), null);

            var righthand_sprite = getSprite(DriverHands.righthand);

            g.drawImage(righthand_sprite, round(screenx + (driverx + 42) * scale), round(screeny + (drivery + 90 - driver_hand_movement) * scale), round(righthand_sprite.getWidth(null) * scale), round(righthand_sprite.getHeight(null) * scale), null);

            var hand_sprite = getSprite(ConstantSprite.Player.hand);

            g.drawImage(hand_sprite, round(screenx + 20 * scale), round(screeny + 150 * scale), round(hand_sprite.getWidth(null) * scale), round(hand_sprite.getHeight(null) * scale), null);

            var cowbell_sprite = getSprite(ConstantSprite.Player.cowbell);

            g.drawImage(cowbell_sprite, round(screenx + (53 + cowbell_offset) * scale), round(screeny + 130 * scale), round(cowbell_sprite.getWidth(null) * scale), round(cowbell_sprite.getHeight(null) * scale), null);

            var stick_sprite = getSprite(ConstantSprite.Player.stick);

            // 150, 120

            // 90, 120
            // 90, 118
            // 105, 105
            // 110, 100
            // 115, 100
            // 120, 100
            // 125, 105
            // 130, 110
            // 140, 115


            

            g.drawImage(stick_sprite, round(screenx + stickx * scale), round(screeny + sticky * scale), round(stick_sprite.getWidth(null) * scale), round(stick_sprite.getHeight(null) * scale), null);

            g.transform(AffineTransform.getTranslateInstance(getWidth(), getHeight()));
            g.transform(AffineTransform.getRotateInstance(-car_rotate));
            g.transform(AffineTransform.getTranslateInstance(-getWidth(), -getHeight()));


            char[] score_chars = (score >= 99999999) ? new char[] { 9, 9, 9, 9, 9, 9, 9, 9 } : String.format("%08d", score).toCharArray();

            {
                int x = 65;
                // final int y = 15;
                final int y = round(scorey - 17);
                for (int i = 0; i < 7; ++i) {
                    var score_sprite = switch (score_chars[i]) {
                        case '0' -> getSprite(Score.n0);
                        case '1' -> getSprite(Score.n1);
                        case '2' -> getSprite(Score.n2);
                        case '3' -> getSprite(Score.n3);
                        case '4' -> getSprite(Score.n4);
                        case '5' -> getSprite(Score.n5);
                        case '6' -> getSprite(Score.n6);
                        case '7' -> getSprite(Score.n7);
                        case '8' -> getSprite(Score.n8);
                        case '9' -> getSprite(Score.n9);
                        default -> getSprite(Score.nm);
                    };
                    g.drawImage(score_sprite, round(screenx + x * scale), round(screeny + y * scale), round(score_sprite.getWidth(null) * scale), round(score_sprite.getHeight(null) * scale), null);
                    x += 12;
                }
                var dot = getSprite(Score.ndot);

                g.drawImage(dot, round(screenx + x * scale), round(screeny + y * scale), round(dot.getWidth(null) * scale), round(dot.getHeight(null) * scale), null);

                x += 5;

                var decimal = switch (score_chars[7]) {
                    case '0' -> getSprite(Score.n0);
                    case '1' -> getSprite(Score.n1);
                    case '2' -> getSprite(Score.n2);
                    case '3' -> getSprite(Score.n3);
                    case '4' -> getSprite(Score.n4);
                    case '5' -> getSprite(Score.n5);
                    case '6' -> getSprite(Score.n6);
                    case '7' -> getSprite(Score.n7);
                    case '8' -> getSprite(Score.n8);
                    case '9' -> getSprite(Score.n9);
                    default -> getSprite(Score.nm);
                };

                g.drawImage(decimal, round(screenx + x * scale), round(screeny + y * scale), round(decimal.getWidth(null) * scale), round(decimal.getHeight(null) * scale), null);

                x += 15;

                var m = getSprite(Score.nm);

                g.drawImage(m, round(screenx + x * scale), round(screeny + y * scale), round(m.getWidth(null) * scale), round(m.getHeight(null) * scale), null);
            }

            
            char[] highscore_chars = (highscore >= 99999999) ? new char[] { 9, 9, 9, 9, 9, 9, 9, 9 } : String.format("%08d", highscore).toCharArray();
            
            {
                int x = 115;
                final int y = round(scorey - 29);

                var highscore_sprite = getSprite(Score.highscore);
    
                g.drawImage(highscore_sprite, round(screenx + 65 * scale), round(screeny + y * scale), round(highscore_sprite.getWidth(null) * scale), round(highscore_sprite.getHeight(null) * scale), null);


                for (int i = 0; i < 7; ++i) {
                    var score_sprite = switch (highscore_chars[i]) {
                        case '0' -> getSprite(Score.h0);
                        case '1' -> getSprite(Score.h1);
                        case '2' -> getSprite(Score.h2);
                        case '3' -> getSprite(Score.h3);
                        case '4' -> getSprite(Score.h4);
                        case '5' -> getSprite(Score.h5);
                        case '6' -> getSprite(Score.h6);
                        case '7' -> getSprite(Score.h7);
                        case '8' -> getSprite(Score.h8);
                        case '9' -> getSprite(Score.h9);
                        default -> getSprite(Score.hm);
                    };
                    g.drawImage(score_sprite, round(screenx + x * scale), round(screeny + y * scale), round(score_sprite.getWidth(null) * scale), round(score_sprite.getHeight(null) * scale), null);
                    x += 6;
                }
                var dot = getSprite(Score.hdot);

                g.drawImage(dot, round(screenx + x * scale), round(screeny + y * scale), round(dot.getWidth(null) * scale), round(dot.getHeight(null) * scale), null);

                x += 4;

                var decimal = switch (highscore_chars[7]) {
                    case '0' -> getSprite(Score.h0);
                    case '1' -> getSprite(Score.h1);
                    case '2' -> getSprite(Score.h2);
                    case '3' -> getSprite(Score.h3);
                    case '4' -> getSprite(Score.h4);
                    case '5' -> getSprite(Score.h5);
                    case '6' -> getSprite(Score.h6);
                    case '7' -> getSprite(Score.h7);
                    case '8' -> getSprite(Score.h8);
                    case '9' -> getSprite(Score.h9);
                    default -> getSprite(Score.hm);
                };

                g.drawImage(decimal, round(screenx + x * scale), round(screeny + y * scale), round(decimal.getWidth(null) * scale), round(decimal.getHeight(null) * scale), null);

                x += 6;

                var m = getSprite(Score.hm);

                g.drawImage(m, round(screenx + x * scale), round(screeny + y * scale), round(m.getWidth(null) * scale), round(m.getHeight(null) * scale), null);
            }
        }
    }

    public void start() {
        conductor.start(0);
    }

    private int round(double d) {
        return (int) Math.round(d);
    }

    private Image getSprite(ConstantSprite sprite) {
        return sprite_set.get(sprite);
    }

    private Image getSprite(LitSprite sprite) {
        if (in_tunnel) {
            return shadow_sprite_set.get(sprite);
        } else {
            return lit_sprite_set.get(sprite);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200 * 3, 256 * 3);
    }
}
