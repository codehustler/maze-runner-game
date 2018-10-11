package codehustler.ml.mazerunner.ui;

import static codehustler.ml.mazerunner.util.NeedfulThings.vector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;

import codehustler.ml.mazerunner.util.MapOptimizer;
import lombok.Data;

@Data
public class GameMap {
	private Vector startPosition;
	private List<List<Tile>> tiles = new ArrayList<>();
	private List<Tile> wallTiles = new ArrayList<>();
	private Set<Wall> walls = new HashSet<>();
	
	private final RunnerGame game;

	public GameMap(String mapPath, RunnerGame game) throws Exception {
		this.game = game;
		loadMap(Paths.get(ClassLoader.getSystemResource(mapPath).toURI()));
		this.walls = MapOptimizer.optimizeMap(tiles, wallTiles);
	}

	

	public void render(Graphics2D g) {
//		tiles.stream().flatMap(List::stream).forEach(t->t.render(g));
		renderBackground(g);
		renderWalls(g);
	}

	private void renderBackground(Graphics2D g) {
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, tiles.get(0).size() * Tile.TILE_SIZE, tiles.size() * Tile.TILE_SIZE);
	}

	private void renderWalls(Graphics2D g) {
//		System.out.println("wall count: " + walls.size());
		g.setColor(Color.BLACK);
		walls.forEach(v -> {
			g.fillRect((int) v.getA().get(0), (int) v.getA().get(1), (int) v.getSize().get(0),
					(int) v.getSize().get(1));
		});
//		g.setColor(Color.RED);
//		walls.forEach(v -> {
//			g.drawRect((int)v.getA().get(0), (int)v.getA().get(1), (int)v.getSize().get(0), (int)v.getSize().get(1));
//		});
	}

	private void loadMap(Path mapPath) throws IOException {
		AtomicInteger x = new AtomicInteger();
		AtomicInteger y = new AtomicInteger();

		Files.lines(mapPath).forEach(l -> {
			x.set(0);
			List<Tile> tileRow = new ArrayList<>();
			tiles.add(tileRow);

			l.chars()//
					.map(c -> Integer.valueOf(new String(new char[] { (char) c })))//
					.forEach(i -> {
						if (i == 9) {
							startPosition = BasicVector.fromArray(new double[] { x.get(), y.get() });
							tileRow.add(new Tile(vector(x.get(), y.get()), 0, game));
						} else if (i == 2) {// START/FINISH
							Tile t = new Tile(vector(x.get(), y.get()), 0, game);
							t.setProperty("START_FINISH", true);
							tileRow.add(t);
						} else if (i == 3) { // CHECKPOINTS
							tileRow.add(new Tile(vector(x.get(), y.get()), 0, game));
						} else {
							Tile t = new Tile(vector(x.get(), y.get()), i, game);
							if (i == 1) {
								wallTiles.add(t);
							}
							tileRow.add(t);
						}
						x.incrementAndGet();
					});
			y.incrementAndGet();
		});
	}

	public Tile getTileByAddress(Vector address) {
		return tiles.get((int) address.get(1)).get((int) address.get(0));
	}


	public Tile getTileUnderPosition(Vector position) {
		Vector address = positionToAddress(position);
		return getTileByAddress(address);
	}

	private Vector positionToAddress(Vector position) {
		Vector tilePosition = position.divide(Tile.TILE_SIZE);
		tilePosition.update((i, v) -> ((int) v));
		return tilePosition;
	}

	public void clearVisitorCache(MazeRunner runner) {
		tiles.parallelStream().flatMap(List::stream).parallel().forEach(t -> t.clearVisitorCache(runner));
	}

	public void clearVisitorCache() {
		tiles.parallelStream().flatMap(List::stream).parallel().forEach(Tile::clearVisitorCache);
	}
}
