package canaryprism.tonnel.swing;

import java.awt.Image;
import java.util.HashMap;

import javax.imageio.ImageIO;

import canaryprism.tonnel.Tunnel;
import canaryprism.tonnel.swing.annotations.NestedSprites;
import canaryprism.tonnel.swing.annotations.SpritePath;

public class SpriteSet<T> {
    private final HashMap<T, Image> sprites = new HashMap<>();

    @SuppressWarnings("unchecked")
    public SpriteSet(Class<T> type, String sprite_path) {

        if (type.isEnum()) {
            try {
                for (var sprite : type.getEnumConstants()) {
                    sprites.put(sprite, ImageIO.read(Tunnel.getResource(sprite_path + "/" + sprite + ".png")));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            var children = type.getDeclaredAnnotation(NestedSprites.class);
            var nested = children.value();

            try {
                for (var child : nested) {
                    var name = child.getDeclaredAnnotation(SpritePath.class);
                    var path = sprite_path;
                    if (name != null && name.value() != null) {
                        path += "/" + name.value();
                    }
                    for (var sprite : child.getEnumConstants()) {
                        sprites.put((T) sprite, ImageIO.read(Tunnel.getResource(path + "/" + sprite + ".png")));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    public Image get(T sprite) {
        return sprites.get(sprite);
    }
}
