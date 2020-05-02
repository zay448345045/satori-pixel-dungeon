package com.watabou.noosa;

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.utils.Rect;
import com.watabou.utils.RectF;

import java.nio.FloatBuffer;
import java.util.Arrays;

public class Tilemap extends Visual {

	protected SmartTexture texture;
	protected TextureFilm tileset;

	protected int[] data;
	protected int mapWidth;
	protected int mapHeight;
	protected int size;

	private final float cellW;
	private final float cellH;

	protected float[] vertices;
	protected FloatBuffer quads;
	protected Vertexbuffer buffer;

	private final Rect updated;
	private boolean fullUpdate;
	private Rect updating;
	private int topLeftUpdating;
	private int bottomRightUpdating;

	public Tilemap( Object tx, TextureFilm tileset ) {

		super( 0, 0, 0, 0 );

		this.texture = TextureCache.get( tx );
		this.tileset = tileset;

		RectF r = tileset.get( 0 );
		cellW = tileset.width( r );
		cellH = tileset.height( r );

		vertices = new float[16];

		updated = new Rect();
	}

	public void map( int[] data, int cols ) {

		this.data = data;

		mapWidth = cols;
		mapHeight = data.length / cols;
		size = mapWidth * mapHeight;

		width = cellW * mapWidth;
		height = cellH * mapHeight;

		quads = Quad.createSet( size );

		updateMap();
	}
	
	public Image image(int x, int y){
		if (!needsRender(x + mapWidth*y)){
			return null;
		} else {
			Image img = new Image(texture);
			img.frame(tileset.get(data[x + mapWidth * y]));
			return img;
		}
	}

	//forces a full update, including new buffer
	public synchronized void updateMap(){
		updated.set( 0, 0, mapWidth, mapHeight );
		fullUpdate = true;
	}

	public synchronized void updateMapCell(int cell){
		updated.union( cell % mapWidth, cell / mapWidth );
	}

	private synchronized void moveToUpdating(){
		updating = new Rect(updated);
		updated.setEmpty();
	}

	protected void updateVertices() {

		moveToUpdating();
		
		float x1, y1, x2, y2;
		int pos;
		RectF uv;

		y1 = cellH * updating.top;
		y2 = y1 + cellH;

		for (int i=updating.top; i < updating.bottom; i++) {

			x1 = cellW * updating.left;
			x2 = x1 + cellW;

			pos = i * mapWidth + updating.left;

			for (int j=updating.left; j < updating.right; j++) {

				if (topLeftUpdating == -1)
					topLeftUpdating = pos;

				bottomRightUpdating = pos + 1;

				quads.position(pos*16);
				
				uv = tileset.get(data[pos]);
				
				if (needsRender(pos) && uv != null) {

					vertices[0] = x1;
					vertices[1] = y1;

					vertices[2] = uv.left;
					vertices[3] = uv.top;

					vertices[4] = x2;
					vertices[5] = y1;

					vertices[6] = uv.right;
					vertices[7] = uv.top;

					vertices[8] = x2;
					vertices[9] = y2;

					vertices[10] = uv.right;
					vertices[11] = uv.bottom;

					vertices[12] = x1;
					vertices[13] = y2;

					vertices[14] = uv.left;
					vertices[15] = uv.bottom;

				} else {

					//If we don't need to draw this tile simply set the quad to size 0 at 0, 0.
					// This does result in the quad being drawn, but we are skipping all
					// pixel-filling. This is better than fully skipping rendering as we
					// don't need to manage a buffer of drawable tiles with insertions/deletions.
					Arrays.fill(vertices, 0);
				}

				quads.put(vertices);

				pos++;
				x1 = x2;
				x2 += cellW;

			}

			y1 = y2;
			y2 += cellH;
		}

	}

	//private int camX, camY, camW, camH;
	//private int topLeft, bottomRight, length;

	@Override
	public void draw() {

		super.draw();

		if (!updated.isEmpty()) {
			updateVertices();
			if (buffer == null)
				buffer = new Vertexbuffer(quads);
			else {
				if (fullUpdate) {
					buffer.updateVertices(quads);
					fullUpdate = false;
				} else {
					buffer.updateVertices(quads,
							topLeftUpdating * 16,
							bottomRightUpdating * 16);
				}
			}
			topLeftUpdating = -1;
			updating.setEmpty();
		}
		
		//FIXME temporarily disabled this optimization as it is suspected to cause crashes
		/*Camera c = Camera.main;
		//we treat the position of the tilemap as (0,0) here
		camX = (int)(c.scroll.x/cellW - x/cellW);
		camY = (int)(c.scroll.y/cellH - y/cellH);
		camW = (int)Math.ceil(c.width/cellW);
		camH = (int)Math.ceil(c.height/cellH);

		if (camX >= mapWidth
				|| camY >= mapHeight
				|| camW + camW <= 0
				|| camH + camH <= 0)
			return;

		//determines the top-left visible tile, the bottom-right one, and the buffer length
		//between them, this culls a good number of none-visible tiles while keeping to 1 draw
		topLeft = Math.max(camX, 0)
				+ Math.max(camY*mapWidth, 0);

		bottomRight = Math.min(camX+camW, mapWidth-1)
				+ Math.min((camY+camH)*mapWidth, (mapHeight-1)*mapWidth);

		if (topLeft >= size || bottomRight < 0)
			length = 0;
		else
			length = bottomRight - topLeft + 1;

		if (length <= 0)
			return;*/

		NoosaScript script = script();

		texture.bind();

		script.uModel.valueM4( matrix );
		script.lighting(
				rm, gm, bm, am,
				ra, ga, ba, aa );

		script.camera( camera );

		script.drawQuadSet( buffer, size, 0 );

	}
	
	protected NoosaScript script(){
		return NoosaScriptNoLighting.get();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (buffer != null)
			buffer.delete();
	}

	protected boolean needsRender(int pos){
		return data[pos] >= 0;
	}
}
