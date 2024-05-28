package canaryprism.tonnel.swing;

import canaryprism.tonnel.swing.ConstantSprite.*;
import canaryprism.tonnel.swing.annotations.NestedSprites;
import canaryprism.tonnel.swing.annotations.SpritePath;

@NestedSprites({ Player.class, CloudThing.class, Background.class, TunnelSprite.class, Score.class })
public sealed interface ConstantSprite {
    public enum Player implements ConstantSprite {
        hand, cowbell, stick
    }

    @SpritePath("anger")
    public enum CloudThing implements ConstantSprite {
        one, two
    }

    @SpritePath("background")
    public enum Background implements ConstantSprite {
        sea, plains, cropstomp, moai, desert, city, stars, sign
    }

    @SpritePath("background")
    public enum TunnelSprite implements ConstantSprite {
        tunnel
    }

    @SpritePath("score")
    public enum Score implements ConstantSprite {
        n1, n2, n3, n4, n5, n6, n7, n8, n9, n0, ndot, nm,
        h1, h2, h3, h4, h5, h6, h7, h8, h9, h0, hdot, hm,
        highscore
    }
}
