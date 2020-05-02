package studio.baka.satoripixeldungeon.sprites;

import studio.baka.satoripixeldungeon.Dungeon;
import studio.baka.satoripixeldungeon.actors.Char;
import studio.baka.satoripixeldungeon.actors.mobs.npcs.MirrorImage;
import com.watabou.noosa.TextureFilm;

public class MirrorSprite extends MobSprite {

    private static final int FRAME_WIDTH = 12;
    private static final int FRAME_HEIGHT = 15;

    public MirrorSprite() {
        super();

        texture(Dungeon.hero.heroClass.spritesheet());
        updateArmor(0);
        idle();
    }

    @Override
    public void link(Char ch) {
        super.link(ch);
        updateArmor(((MirrorImage) ch).armTier);
    }

    public void updateArmor(int tier) {
        TextureFilm film = new TextureFilm(HeroSprite.tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT);

        idle = new Animation(1, true);
        idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);

        run = new Animation(20, true);
        run.frames(film, 2, 3, 4, 5, 6, 7);

        die = new Animation(20, false);
        die.frames(film, 0);

        attack = new Animation(15, false);
        attack.frames(film, 13, 14, 15, 0);

        idle();
    }
}
