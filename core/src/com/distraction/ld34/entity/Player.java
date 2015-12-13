package com.distraction.ld34.entity;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.distraction.ld34.farm.Crop;
import com.distraction.ld34.farm.Patch;
import com.distraction.ld34.farm.Seed;
import com.distraction.ld34.tile.MapObject;
import com.distraction.ld34.tile.TileMap;
import com.distraction.ld34.util.Res;

public class Player extends MapObject {
	
	private Patch[][] farm;
	
	private List<Crop> crops;
	private List<Seed.Type> seeds;
	private int money = 50;
	
	private Action action;
	private int actionRow;
	private int actionCol;
	private float actionTime;
	private float actionTimeRequired;
	private float[] actionSpeedMultipliers = {1, 1, 1, 1};
	
	private Seed.Type nextSeedType;
	
	private TextureRegion pixel;
	
	private Patch selectedPatch;
	
	private int row;
	private int col;
	
	public enum Action {
		TILL(3),
		WATER(3),
		SEED(2),
		HARVEST(1);
		public float timeRequired;
		Action(float timeRequired) {
			this.timeRequired = timeRequired;
		}
	};
	
	public Player(TileMap tileMap) {
		super(tileMap);
		Texture tex = Res.i().getTexture("player");
		TextureRegion[] reg = new TextureRegion[1];
		reg[0] = new TextureRegion(tex, 0, 0, 32, 32);
		setAnimation(reg, 0);
		cwidth = 20;
		cheight = 20;
		
		moveSpeed = 100;
		crops = new ArrayList<Crop>();
		seeds = new ArrayList<Seed.Type>();
		
		pixel = new TextureRegion(Res.i().getTexture("pixel"));
	}
	
	public void buySeed(Seed.Type type) {
		if(money >= type.cost) {
			money -= type.cost;
			seeds.add(type);
		}
	}
	
	public void addMoney(int amount) {
		money += amount;
	}
	
	public int getMoney() {
		return money;
	}
	
	public int getNumSeeds() {
		return seeds.size();
	}
	
	public void setFarm(Patch[][] farm) {
		this.farm = farm;
	}
	
	public void actionStarted(Action action, int actionRow, int actionCol) {
		this.action = action;
		this.actionRow = actionRow;
		this.actionCol = actionCol;
		actionTime = 0;
		actionTimeRequired = action.timeRequired * actionSpeedMultipliers[action.ordinal()];
	}
	
	public void upgradeAction(Action action, int level) {
		actionSpeedMultipliers[action.ordinal()] = level == 2 ? 0.5f : 0;
	}
	
	public void actionFinished() {
		switch(action) {
		case TILL:
			farm[actionRow][actionCol].till();
			break;
		case WATER:
			farm[actionRow][actionCol].water();
			break;
		case SEED:
			farm[actionRow][actionCol].seed(new Seed(nextSeedType,
					tileSize * ((int) (x / tileSize) + 0.5f),
					tileSize * ((int) (y / tileSize) + 0.5f),
					32, 32));
			break;
		case HARVEST:
			Crop crop = farm[actionRow][actionCol].harvest();
			if(crop != null) {
				crops.add(crop);
			}
			break;
		}
		action = null;
	}
	
	private void getCurrentTile() {
		row = (int) (tileMap.getNumRows() - (y / tileSize));
		col = (int) (x / tileSize);
		row -= 3;
		col -= 5;
	}
	
	public void till() {
		getCurrentTile();
		if(row < 0 || row >= farm.length || col < 0 || col >= farm[0].length) {
			return;
		}
		if(action == null && farm[row][col].canTill()) {
			actionStarted(Action.TILL, row, col);
		}
	}
	
	public void water() {
		getCurrentTile();
		if(row < 0 || row >= farm.length || col < 0 || col >= farm[0].length) {
			return;
		}
		if(action == null && farm[row][col].canWater()) {
			actionStarted(Action.WATER, row, col);
		}
	}
	
	public void seed() {
		getCurrentTile();
		if(row < 0 || row >= farm.length || col < 0 || col >= farm[0].length) {
			return;
		}
		nextSeedType = (seeds.isEmpty() || farm[row][col].hasSeed() || farm[row][col].getState() == Patch.State.NORMAL) ? null : seeds.remove(0);
		if(action == null && farm[row][col].canSeed() && nextSeedType != null) {
			actionStarted(Action.SEED, row, col);
		}
	}
	
	public void harvest() {
		getCurrentTile();
		if(row < 0 || row >= farm.length || col < 0 || col >= farm[0].length) {
			return;
		}
		if(action == null && farm[row][col].canHarvest()) {
			System.out.println("harvesting");
			actionStarted(Action.HARVEST, row, col);
		}
	}
	
	public void unload() {
		for(Crop crop : crops) {
			addMoney(crop.getValue());
		}
		crops.clear();
	}
	
	public int getNumCrops() {
		return crops.size();
	}
	
	private void highlightPatch() {
		int row = (int) (tileMap.getNumRows() - (y / tileSize));
		int col = (int) (x / tileSize);
		row -= 3;
		col -= 5;
		if(row < 0 || row >= farm.length || col < 0 || col >= farm[0].length) {
			selectedPatch = null;
			return;
		}
		selectedPatch = farm[row][col];
	}
	
	@Override
	public void update(float dt) {
		if(action != null) {
			actionTime += dt;
			if(actionTime >= actionTimeRequired) {
				actionFinished();
			}
		}
		else {
			if(left) {
				dx = -moveSpeed * dt;
			}
			else if(right) {
				dx = moveSpeed * dt;
			}
			else {
				dx = 0;
			}
			if(down) {
				dy = -moveSpeed * dt;
			}
			else if(up) {
				dy = moveSpeed * dt;
			}
			else {
				dy = 0;
			}
			
			checkTileMapCollision();
			x = xtemp;
			y = ytemp;
			
			highlightPatch();
		}
	}
	
	@Override
	public void render(SpriteBatch sb) {
		super.render(sb);
		if(selectedPatch != null) {
			selectedPatch.renderHighlight(sb);
		}
		if(action != null) {
			Color c = sb.getColor();
			sb.setColor(Color.GREEN);
			sb.draw(pixel, x - width / 2, y + height / 2, width * actionTime / actionTimeRequired, 3);
			sb.setColor(Color.BLACK);
			sb.draw(pixel, x - width / 2, y + height / 2, width, 1);
			sb.draw(pixel, x - width / 2, y + height / 2 + 3, width, 1);
			sb.draw(pixel, x - width / 2, y + height / 2, 1, 4);
			sb.draw(pixel, x + width / 2, y + height / 2, 1, 4);
			sb.setColor(c);
		}
	}
	
}
