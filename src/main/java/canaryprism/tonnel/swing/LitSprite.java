package canaryprism.tonnel.swing;

import canaryprism.tonnel.swing.LitSprite.*;
import canaryprism.tonnel.swing.annotations.NestedSprites;
import canaryprism.tonnel.swing.annotations.SpritePath;

@NestedSprites({ Driver.class, DriverHands.class, Car.class })
public sealed interface LitSprite {

    @SpritePath("driver")
    public enum Driver implements LitSprite {
        normal0, normal1, normal2, flinch, anger, fail
    }

    @SpritePath("driver")
    public enum DriverHands implements LitSprite {
        lefthand, righthand
    }

    @SpritePath("car")
    public enum Car implements LitSprite {
        body, wheel
    }
}
