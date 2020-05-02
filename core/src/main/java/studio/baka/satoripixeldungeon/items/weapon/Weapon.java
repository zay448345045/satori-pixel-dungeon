package studio.baka.satoripixeldungeon.items.weapon;

import studio.baka.satoripixeldungeon.Badges;
import studio.baka.satoripixeldungeon.Dungeon;
import studio.baka.satoripixeldungeon.actors.Char;
import studio.baka.satoripixeldungeon.actors.buffs.MagicImmune;
import studio.baka.satoripixeldungeon.actors.hero.Hero;
import studio.baka.satoripixeldungeon.items.Item;
import studio.baka.satoripixeldungeon.items.KindOfWeapon;
import studio.baka.satoripixeldungeon.items.rings.RingOfFuror;
import studio.baka.satoripixeldungeon.items.weapon.curses.*;
import studio.baka.satoripixeldungeon.items.weapon.enchantments.*;
import studio.baka.satoripixeldungeon.items.weapon.melee.ThreeDirectionsword;
import studio.baka.satoripixeldungeon.messages.Messages;
import studio.baka.satoripixeldungeon.sprites.ItemSprite;
import studio.baka.satoripixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

abstract public class Weapon extends KindOfWeapon {

    public float ACC = 1f;    // Accuracy modifier
    public float DLY = 1f;    // Speed modifier
    public int RCH = 1;    // Reach modifier (only applies to melee hits)

    public enum Augment {
        SPEED(0.7f, 0.6667f),
        DAMAGE(1.5f, 1.6667f),
        NONE(1.0f, 1.0000f);

        private final float damageFactor;
        private final float delayFactor;

        Augment(float dmg, float dly) {
            damageFactor = dmg;
            delayFactor = dly;
        }

        public int damageFactor(int dmg) {
            return Math.round(dmg * damageFactor);
        }

        public float delayFactor(float dly) {
            return dly * delayFactor;
        }
    }

    public Augment augment = Augment.NONE;

    private static final int USES_TO_ID = 20;
    private int usesLeftToID = USES_TO_ID;
    private float availableUsesToID = USES_TO_ID / 2f;

    public Enchantment enchantment;
    public boolean curseInfusionBonus = false;

    @Override
    public int proc(Char attacker, Char defender, int damage) {

        if (enchantment != null && attacker.buff(MagicImmune.class) == null) {
            damage = enchantment.proc(this, attacker, defender, damage);
        }

        if (!levelKnown && attacker == Dungeon.hero && availableUsesToID >= 1) {
            availableUsesToID--;
            usesLeftToID--;
            if (usesLeftToID <= 0) {
                identify();
                GLog.p(Messages.get(Weapon.class, "identify"));
                Badges.validateItemLevelAquired(this);
            }
        }

        return damage;
    }

    public void onHeroGainExp(float levelPercent, Hero hero) {
        if (!levelKnown && isEquipped(hero) && availableUsesToID <= USES_TO_ID / 2f) {
            //gains enough uses to ID over 0.5 levels
            availableUsesToID = Math.min(USES_TO_ID / 2f, availableUsesToID + levelPercent * USES_TO_ID);
        }
    }

    private static final String USES_LEFT_TO_ID = "uses_left_to_id";
    private static final String AVAILABLE_USES = "available_uses";
    private static final String ENCHANTMENT = "enchantment";
    private static final String CURSE_INFUSION_BONUS = "curse_infusion_bonus";
    private static final String AUGMENT = "augment";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(USES_LEFT_TO_ID, usesLeftToID);
        bundle.put(AVAILABLE_USES, availableUsesToID);
        bundle.put(ENCHANTMENT, enchantment);
        bundle.put(CURSE_INFUSION_BONUS, curseInfusionBonus);
        bundle.put(AUGMENT, augment);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        usesLeftToID = bundle.getInt(USES_LEFT_TO_ID);
        availableUsesToID = bundle.getInt(AVAILABLE_USES);
        enchantment = (Enchantment) bundle.get(ENCHANTMENT);
        curseInfusionBonus = bundle.getBoolean(CURSE_INFUSION_BONUS);

        //pre-0.7.2 saves
        if (bundle.contains("unfamiliarity")) {
            usesLeftToID = bundle.getInt("unfamiliarity");
            availableUsesToID = USES_TO_ID / 2f;
        }

        augment = bundle.getEnum(AUGMENT, Augment.class);
    }

    @Override
    public void reset() {
        super.reset();
        usesLeftToID = USES_TO_ID;
        availableUsesToID = USES_TO_ID / 2f;
    }

    @Override
    public float accuracyFactor(Char owner) {

        int encumbrance = 0;

        if (owner instanceof Hero) {
            encumbrance = STRReq() - ((Hero) owner).STR();
        }

        if (hasEnchant(Wayward.class, owner))
            encumbrance = Math.max(2, encumbrance + 2);

        float ACC = this.ACC;

        return encumbrance > 0 ? (float) (ACC / Math.pow(1.5, encumbrance)) : ACC;
    }

    @Override
    public float speedFactor(Char owner) {

        int encumbrance = 0;
        if (owner instanceof Hero) {
            encumbrance = STRReq() - ((Hero) owner).STR();
        }

        float DLY = augment.delayFactor(this.DLY);

        DLY *= RingOfFuror.attackDelayMultiplier(owner);

        return (encumbrance > 0 ? (float) (DLY * Math.pow(1.2, encumbrance)) : DLY);
    }

    @Override
    public int reachFactor(Char owner) {
        return (hasEnchant(Projecting.class, owner) && !(this instanceof ThreeDirectionsword)) ? RCH + 1 : RCH;
    }

    public int STRReq() {
        return STRReq(level());
    }

    public abstract int STRReq(int lvl);

    @Override
    public int level() {
        return super.level() + (curseInfusionBonus ? 1 : 0);
    }

    @Override
    public Item upgrade() {
        return upgrade(false);
    }

    public Item upgrade(boolean enchant) {

        if (enchant) {
            if (enchantment == null || hasCurseEnchant()) {
                enchant(Enchantment.random());
            }
        } else {
            if (hasCurseEnchant()) {
                if (Random.Int(3) == 0) enchant(null);
            } else if (level() >= 4 && Random.Float(10) < Math.pow(2, level() - 4)) {
                enchant(null);
            }
        }

        cursed = false;

        return super.upgrade();
    }

    @Override
    public String name() {
        return enchantment != null && (cursedKnown || !enchantment.curse()) ? enchantment.name(super.name()) : super.name();
    }

    @Override
    public Item random() {
        //+0: 75% (3/4)
        //+1: 20% (4/20)
        //+2: 5%  (1/20)
        int n = 0;
        if (Random.Int(4) == 0) {
            n++;
            if (Random.Int(5) == 0) {
                n++;
            }
        }
        level(n);

        //30% chance to be cursed
        //10% chance to be enchanted
        float effectRoll = Random.Float();
        if (effectRoll < 0.3f) {
            enchant(Enchantment.randomCurse());
            cursed = true;
        } else if (effectRoll >= 0.9f) {
            enchant();
        }

        return this;
    }

    public Weapon enchant(Enchantment ench) {
        if (ench == null || !ench.curse()) curseInfusionBonus = false;
        enchantment = ench;
        updateQuickslot();
        return this;
    }

    public Weapon enchant() {

        Class<? extends Enchantment> oldEnchantment = enchantment != null ? enchantment.getClass() : null;
        @SuppressWarnings("unchecked") Enchantment ench = Enchantment.random(oldEnchantment);

        return enchant(ench);
    }

    public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
        return enchantment != null && enchantment.getClass() == type && owner.buff(MagicImmune.class) == null;
    }

    //these are not used to process specific enchant effects, so magic immune doesn't affect them
    public boolean hasGoodEnchant() {
        return enchantment != null && !enchantment.curse();
    }

    public boolean hasCurseEnchant() {
        return enchantment != null && enchantment.curse();
    }

    @Override
    public ItemSprite.Glowing glowing() {
        return enchantment != null && (cursedKnown || !enchantment.curse()) ? enchantment.glowing() : null;
    }

    public static abstract class Enchantment implements Bundlable {

        private static final Class<?>[] common = new Class<?>[]{
                Blazing.class, Chilling.class, Kinetic.class, Shocking.class};

        private static final Class<?>[] uncommon = new Class<?>[]{
                Blocking.class, Blooming.class, Elastic.class,
                Lucky.class, Projecting.class, Unstable.class};

        private static final Class<?>[] rare = new Class<?>[]{
                Corrupting.class, Grim.class, Vampiric.class};

        private static final float[] typeChances = new float[]{
                50, //12.5% each
                40, //6.67% each
                10  //3.33% each
        };

        private static final Class<?>[] curses = new Class<?>[]{
                Annoying.class, Displacing.class, Exhausting.class, Fragile.class,
                Sacrificial.class, Wayward.class, Polarized.class, Friendly.class
        };


        public abstract int proc(Weapon weapon, Char attacker, Char defender, int damage);

        public String name() {
            if (!curse())
                return name(Messages.get(this, "enchant"));
            else
                return name(Messages.get(Item.class, "curse"));
        }

        public String name(String weaponName) {
            return Messages.get(this, "name", weaponName);
        }

        public String desc() {
            return Messages.get(this, "desc");
        }

        public boolean curse() {
            return false;
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
        }

        @Override
        public void storeInBundle(Bundle bundle) {
        }

        public abstract ItemSprite.Glowing glowing();

        @SuppressWarnings("unchecked")
        public static Enchantment random(Class<? extends Enchantment>... toIgnore) {
            switch (Random.chances(typeChances)) {
                case 0:
                default:
                    return randomCommon(toIgnore);
                case 1:
                    return randomUncommon(toIgnore);
                case 2:
                    return randomRare(toIgnore);
            }
        }

        @SuppressWarnings("unchecked")
        public static Enchantment randomCommon(Class<? extends Enchantment>... toIgnore) {
            ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(common));
            enchants.removeAll(Arrays.asList(toIgnore));
            if (enchants.isEmpty()) {
                return random();
            } else {
                return (Enchantment) Reflection.newInstance(Random.element(enchants));
            }
        }

        @SuppressWarnings("unchecked")
        public static Enchantment randomUncommon(Class<? extends Enchantment>... toIgnore) {
            ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(uncommon));
            enchants.removeAll(Arrays.asList(toIgnore));
            if (enchants.isEmpty()) {
                return random();
            } else {
                return (Enchantment) Reflection.newInstance(Random.element(enchants));
            }
        }

        @SuppressWarnings("unchecked")
        public static Enchantment randomRare(Class<? extends Enchantment>... toIgnore) {
            ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(rare));
            enchants.removeAll(Arrays.asList(toIgnore));
            if (enchants.isEmpty()) {
                return random();
            } else {
                return (Enchantment) Reflection.newInstance(Random.element(enchants));
            }
        }

        @SuppressWarnings("unchecked")
        public static Enchantment randomCurse(Class<? extends Enchantment>... toIgnore) {
            ArrayList<Class<?>> enchants = new ArrayList<>(Arrays.asList(curses));
            enchants.removeAll(Arrays.asList(toIgnore));
            if (enchants.isEmpty()) {
                return random();
            } else {
                return (Enchantment) Reflection.newInstance(Random.element(enchants));
            }
        }

    }
}
