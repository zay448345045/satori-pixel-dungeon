package studio.baka.satoripixeldungeon.tiles;

import studio.baka.satoripixeldungeon.Assets;
import studio.baka.satoripixeldungeon.Dungeon;
import studio.baka.satoripixeldungeon.levels.Terrain;
import studio.baka.satoripixeldungeon.levels.traps.Trap;
import studio.baka.satoripixeldungeon.plants.Plant;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.ScaleTweener;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

//TODO add in a proper set of vfx for plants growing/withering, grass burning, discovering traps
public class TerrainFeaturesTilemap extends DungeonTilemap {

    private static TerrainFeaturesTilemap instance;

    private final SparseArray<Plant> plants;
    private final SparseArray<Trap> traps;

    public TerrainFeaturesTilemap(SparseArray<Plant> plants, SparseArray<Trap> traps) {
        super(Assets.TERRAIN_FEATURES);

        this.plants = plants;
        this.traps = traps;

        map(Dungeon.level.map, Dungeon.level.width());

        instance = this;
    }

    protected int getTileVisual(int pos, int tile, boolean flat) {
        if (traps.get(pos) != null) {
            Trap trap = traps.get(pos);
            if (!trap.visible)
                return -1;
            else
                return (trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16);
        }

        if (plants.get(pos) != null) {
            return plants.get(pos).image + 7 * 16;
        }

        int stage = (Dungeon.depth - 1) / 5;
        if (Dungeon.depth == 21) stage--;
        if (tile == Terrain.HIGH_GRASS) {
            return 9 + 16 * stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
        } else if (tile == Terrain.FURROWED_GRASS) {
            //TODO
            return 11 + 16 * stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
        } else if (tile == Terrain.GRASS) {
            return 13 + 16 * stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
        } else if (tile == Terrain.EMBERS) {
            return 9 * (16 * 5) + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
        }

        return -1;
    }

    public static Image tile(int pos, int tile) {
        RectF uv = instance.tileset.get(instance.getTileVisual(pos, tile, true));
        if (uv == null) return null;

        Image img = new Image(instance.texture);
        img.frame(uv);
        return img;
    }

    public void growPlant(final int pos) {
        final Image plant = tile(pos, map[pos]);
        if (plant == null) return;

        plant.origin.set(8, 12);
        plant.scale.set(0);
        plant.point(DungeonTilemap.tileToWorld(pos));

        parent.add(plant);

        parent.add(new ScaleTweener(plant, new PointF(1, 1), 0.2f) {
            protected void onComplete() {
                plant.killAndErase();
                killAndErase();
                updateMapCell(pos);
            }
        });
    }

}
