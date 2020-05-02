package studio.baka.satoripixeldungeon.plants;

import studio.baka.satoripixeldungeon.Assets;
import studio.baka.satoripixeldungeon.Challenges;
import studio.baka.satoripixeldungeon.Dungeon;
import studio.baka.satoripixeldungeon.actors.Actor;
import studio.baka.satoripixeldungeon.actors.Char;
import studio.baka.satoripixeldungeon.actors.hero.Hero;
import studio.baka.satoripixeldungeon.actors.hero.HeroSubClass;
import studio.baka.satoripixeldungeon.effects.CellEmitter;
import studio.baka.satoripixeldungeon.effects.particles.LeafParticle;
import studio.baka.satoripixeldungeon.items.Item;
import studio.baka.satoripixeldungeon.levels.Level;
import studio.baka.satoripixeldungeon.levels.Terrain;
import studio.baka.satoripixeldungeon.messages.Messages;
import studio.baka.satoripixeldungeon.scenes.GameScene;
import studio.baka.satoripixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public abstract class Plant implements Bundlable {

    public String plantName = Messages.get(this, "name");

    public int image;
    public int pos;

    public void trigger() {

        Char ch = Actor.findChar(pos);

        if (ch instanceof Hero) {
            ((Hero) ch).interrupt();
        }

        wither();
        activate(ch);
    }

    public abstract void activate(Char ch);

    public void wither() {
        Dungeon.level.uproot(pos);

        if (Dungeon.level.heroFOV[pos]) {
            CellEmitter.get(pos).burst(LeafParticle.GENERAL, 6);
        }

    }

    private static final String POS = "pos";

    @Override
    public void restoreFromBundle(Bundle bundle) {
        pos = bundle.getInt(POS);
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(POS, pos);
    }

    public String desc() {
        return Messages.get(this, "desc");
    }

    public static class Seed extends Item {

        public static final String AC_PLANT = "PLANT";

        private static final float TIME_TO_PLANT = 1f;

        {
            stackable = true;
            defaultAction = AC_THROW;
        }

        protected Class<? extends Plant> plantClass;

        @Override
        public ArrayList<String> actions(Hero hero) {
            ArrayList<String> actions = super.actions(hero);
            actions.add(AC_PLANT);
            return actions;
        }

        @Override
        protected void onThrow(int cell) {
            if (Dungeon.level.map[cell] == Terrain.ALCHEMY
                    || Dungeon.level.pit[cell]
                    || Dungeon.level.traps.get(cell) != null
                    || Dungeon.isChallenged(Challenges.NO_HERBALISM)) {
                super.onThrow(cell);
            } else {
                Dungeon.level.plant(this, cell);
                if (Dungeon.hero.subClass == HeroSubClass.WARDEN) {
                    for (int i : PathFinder.NEIGHBOURS8) {
                        int c = Dungeon.level.map[cell + i];
                        if (c == Terrain.EMPTY || c == Terrain.EMPTY_DECO
                                || c == Terrain.EMBERS || c == Terrain.GRASS) {
                            Level.set(cell + i, Terrain.FURROWED_GRASS);
                            GameScene.updateMap(cell + i);
                            CellEmitter.get(cell + i).burst(LeafParticle.LEVEL_SPECIFIC, 4);
                        }
                    }
                }
            }
        }

        @Override
        public void execute(Hero hero, String action) {

            super.execute(hero, action);

            if (action.equals(AC_PLANT)) {

                hero.spend(TIME_TO_PLANT);
                hero.busy();
                ((Seed) detach(hero.belongings.backpack)).onThrow(hero.pos);

                hero.sprite.operate(hero.pos);

            }
        }

        public Plant couch(int pos, Level level) {
            if (level != null && level.heroFOV != null && level.heroFOV[pos]) {
                Sample.INSTANCE.play(Assets.SND_PLANT);
            }
            Plant plant = Reflection.newInstance(plantClass);
            plant.pos = pos;
            return plant;
        }

        @Override
        public boolean isUpgradable() {
            return false;
        }

        @Override
        public boolean isIdentified() {
            return true;
        }

        @Override
        public int price() {
            return 10 * quantity;
        }

        @Override
        public String desc() {
            return Messages.get(plantClass, "desc");
        }

        @Override
        public String info() {
            return Messages.get(Seed.class, "info", desc());
        }

        public static class PlaceHolder extends Seed {

            {
                image = ItemSpriteSheet.SEED_HOLDER;
            }

            @Override
            public boolean isSimilar(Item item) {
                return item instanceof Plant.Seed;
            }

            @Override
            public String info() {
                return "";
            }
        }
    }
}
