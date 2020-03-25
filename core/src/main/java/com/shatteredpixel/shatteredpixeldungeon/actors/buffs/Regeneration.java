/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2019 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;

import java.lang.annotation.Target;

public class Regeneration extends Buff {
	
	{
		//unlike other buffs, this one acts after the hero and takes priority against other effects
		//healing is much more useful if you get some of it off before taking damage
		actPriority = HERO_PRIO - 1;
	}
	
	private static final float REGENERATION_DELAY = 10;
	private static float REGENERATION_DELAY_FIX = REGENERATION_DELAY;
	
	@Override
	public boolean act() {
		if (target.isAlive()) {

			if (target.HP < regencap() && !((Hero)target).isStarving()) {
				LockedFloor lock = target.buff(LockedFloor.class);
				if (target.HP > 0 && (lock == null || lock.regenOn())) {
					target.HP += 1;
					if (target.HP == regencap()) {
						((Hero) target).resting = false;
					}
				}
			}

			ChaliceOfBlood.chaliceRegen regenBuff = Dungeon.hero.buff( ChaliceOfBlood.chaliceRegen.class);

			if (target == Dungeon.hero && Dungeon.hero.heroClass == HeroClass.MAHOU_SHOUJO) REGENERATION_DELAY_FIX = 5;

			if (regenBuff != null)
				if (regenBuff.isCursed())
					spend( REGENERATION_DELAY_FIX * 1.5f );
				else
					spend( REGENERATION_DELAY_FIX - regenBuff.itemLevel()*0.9f );
			else
				spend( REGENERATION_DELAY_FIX );
			
		} else {
			
			diactivate();
			
		}
		
		return true;
	}

	public float getregendelay(){
		ChaliceOfBlood.chaliceRegen regenBuff = Dungeon.hero.buff( ChaliceOfBlood.chaliceRegen.class);

		if (target == Dungeon.hero && Dungeon.hero.heroClass == HeroClass.MAHOU_SHOUJO) REGENERATION_DELAY_FIX = 5;

		if (regenBuff != null)
			if (regenBuff.isCursed())
				return  (REGENERATION_DELAY_FIX * 1.5f) ;
			else
				return  ( REGENERATION_DELAY_FIX - regenBuff.itemLevel()*0.9f );
		else
			return  REGENERATION_DELAY_FIX ;
	}
	
	public int regencap(){
		return target.HT;
	}
}