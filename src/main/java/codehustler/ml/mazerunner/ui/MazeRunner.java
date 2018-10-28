package codehustler.ml.mazerunner.ui;

import static codehustler.ml.mazerunner.util.NeedfulThings.combine;
import static codehustler.ml.mazerunner.util.NeedfulThings.drawVector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.la4j.Matrix;
import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;

import codehustler.ml.mazerunner.Player;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data 
@EqualsAndHashCode(of="player")
public strictfp class MazeRunner {
	
	public static int[] FIELD_OF_VIEW_ANGLES = new int[] {5, 15, 35, 70, 100};

	public static final int runnerRadius = 10;

	private Vector position = BasicVector.fromArray(new double[] { 0d, 0d });

	private Vector velocity = BasicVector.fromArray(new double[] { 0d, -1d });

	private Matrix rotationMatrixCW = Matrix.zero(2, 2);
	private Matrix rotationMatrixCCW = Matrix.zero(2, 2);

	private GameMap map;
	
	private Player player;
	
	private final RunnerGame game;

	private List<Vector> fieldOfView = new ArrayList<>();
	private Set<Vector> viewportIntersections = Collections.synchronizedSet(new HashSet<>());
	
	boolean gameOver = false;
	
	private double[] observations;
	
	private Tile occupiedTile;

	private long lastStartFinishTimecode;

	private int lapCounter = 0;
	
	public MazeRunner(Player player, RunnerGame game) {
		this.game = game;
		this.map = game.getMap();
		this.player = player;
		this.position = this.map.getStartPosition().multiply(Tile.TILE_SIZE).add(Tile.TILE_SIZE / 2);
		this.lastStartFinishTimecode = game.getTimecode();
		this.init();
	}

	private void init() {
		double angleDeg = 2.2;
		double angle = Math.toRadians(-angleDeg);

		rotationMatrixCW.set(0, 0, Math.cos(angle));
		rotationMatrixCW.set(1, 0, Math.sin(angle));
		rotationMatrixCW.set(0, 1, -Math.sin(angle));
		rotationMatrixCW.set(1, 1, Math.cos(angle));

		angle = Math.toRadians(angleDeg);
		rotationMatrixCCW.set(0, 0, Math.cos(angle));
		rotationMatrixCCW.set(1, 0, Math.sin(angle));
		rotationMatrixCCW.set(0, 1, -Math.sin(angle));
		rotationMatrixCCW.set(1, 1, Math.cos(angle));

		updateFieldOfView();
		observations = new double[fieldOfView.size()];
	}
	
	private void updateFieldOfView() {
		// field of view
		Vector front = velocity.multiply(10000 / velocity.euclideanNorm());
		fieldOfView.clear();
		fieldOfView.add(front);
		IntStream.of(FIELD_OF_VIEW_ANGLES).forEach(n ->{
			fieldOfView.add(rotateVector(front, n));
			fieldOfView.add(rotateVector(front, -n));
		});
	}

	public synchronized void update() {
		if ( gameOver ) {
			return;
		}
		occupiedTile = map.getTileUnderPosition(getPosition());
		
		if ( occupiedTile.getProperty("START_FINISH", Boolean.class, false) ) {
			long delta = game.getTimecode() - lastStartFinishTimecode;
//			System.out.println("lap?? - " + delta);
			
			if ( delta > 800 ) {
				lapCounter++;
//				System.out.println("Lap: " + lapCounter);
				if ( lapCounter == game.getTargetLaps() ) {
//					System.out.println("RACE FINISHED!");
					gameOver = true;
				}
			}
			lastStartFinishTimecode = game.getTimecode();
			map.clearVisitorCache(this);
		}
		
		player.setInputs(observations);
		// recalculate velocity
		if (player.isTurnLeft()) {
			velocity = velocity.multiply(rotationMatrixCCW);
		} else if (player.isTurnRight()) {
			velocity = velocity.multiply(rotationMatrixCW);
		}

		this.position = position.add(velocity);
		this.collisionCheck();
		this.loopCheck();
		this.updateFieldOfView();
		this.calculateViewport();
		
		occupiedTile.addOccupant(this);
	}
	
	private void gameOver() {
		this.gameOver = true;
	}
	
	/**
	 * checks if the runner is stuck in a loop
	 */
	private void loopCheck() {
		Optional<Long> lastVisited = occupiedTile.lastVisited(this);
		
		lastVisited.ifPresent(l->{
			long delta = game.getTimecode() - l;
			if ( delta > 100 && delta < 10_000) {
				player.setScore(player.getScore()-delta);
				gameOver();
			}
		});
	}
	
	private void calculateViewport() {
		AtomicInteger index = new AtomicInteger();
		viewportIntersections.clear();
		fieldOfView.forEach(v->{
			nearestIntersection(combine(position, position.add(v)), index.getAndIncrement()).ifPresent(viewportIntersections::add);	
		});
	}
	

	private Vector rotateVector(Vector v, double angleDeg) {
		double angle = Math.toRadians(-angleDeg);

		Matrix rotationMatrix = Matrix.zero(2, 2);
		rotationMatrix.set(0, 0, Math.cos(angle));
		rotationMatrix.set(1, 0, Math.sin(angle));
		rotationMatrix.set(0, 1, -Math.sin(angle));
		rotationMatrix.set(1, 1, Math.cos(angle));

		return v.multiply(rotationMatrix);
	}

	public synchronized void render(Graphics2D g) {
		// highlight the occupied tile
//		//Tile tile = map.getTileUnderPosition(getPosition());
//		if (tile.getValue() == 0) {
//			g.setColor(Color.GREEN);
//			g.fillRect((int) tile.getAddress().get(0) * Tile.TILE_SIZE, (int) tile.getAddress().get(1) * Tile.TILE_SIZE,
//					Tile.TILE_SIZE, Tile.TILE_SIZE);
//		} else {
//			g.setColor(Color.RED);
//			g.fillRect((int) tile.getAddress().get(0) * Tile.TILE_SIZE, (int) tile.getAddress().get(1) * Tile.TILE_SIZE,
//					Tile.TILE_SIZE, Tile.TILE_SIZE);
//		}

		// draw runner
		int x = (int) getPosition().get(0) - runnerRadius;
		int y = (int) getPosition().get(1) - runnerRadius;

		int width = runnerRadius * 2;
		int height = runnerRadius * 2;
		
		g.setColor(gameOver ? Color.WHITE : Color.YELLOW);
		
		g.fillOval(x, y, width, height);
		drawVector(position, velocity, runnerRadius / velocity.length(), g);
		
		// draw field of view
//		g.setColor(Color.MAGENTA);
//		fieldOfView.forEach(v->{
//			drawVector(position, v, 1, g);	
//		});

		
//		g.setColor(Color.RED);
//		viewportIntersections.forEach(v -> {
//			g.fillRect((int) (v.get(0) - 2), (int) (v.get(1) - 2), 4, 4);
//			g.drawLine((int)position.get(0), (int)position.get(1), (int)v.get(0), (int)v.get(1));
//		});
	}



	private Optional<Vector> nearestIntersection(Vector edge, int observationIndex) {
		AtomicReference<Vector> nearest = new AtomicReference<>();
		AtomicReference<Double> nearestDistance = new AtomicReference<>();

		// mixed meat :/
		//.filter(t->isWallTileVisible(t, 400))
		
		map.getWalls().stream().flatMap(t -> t.getEdges().stream()).forEach(tileEdge -> {
			Optional<Vector> intersection = lineIntersect(edge, tileEdge);
			intersection.ifPresent(v -> {
				double distance = position.subtract(v).euclideanNorm();

				if (nearest.get() == null || distance < nearestDistance.get()) {
					nearest.set(v);
					nearestDistance.set(distance);
					observations[observationIndex] = distance;
				}
			});
		});
		

		return Optional.ofNullable(nearest.get());
	}


	private static Optional<Vector> lineIntersect(Vector a, Vector b) {
		double p1X = a.get(0);
		double p1Y = a.get(1);
		double p2X = a.get(2);
		double p2Y = a.get(3);

		double p3X = b.get(0);
		double p3Y = b.get(1);
		double p4X = b.get(2);
		double p4Y = b.get(3);

		double denom = (p4Y - p3Y) * (p2X - p1X) - (p4X - p3X) * (p2Y - p1Y);
		if (denom == 0.0) { // Lines are parallel.
			return Optional.empty();
		}
		double ua = ((p4X - p3X) * (p1Y - p3Y) - (p4Y - p3Y) * (p1X - p3X)) / denom;
		double ub = ((p2X - p1X) * (p1Y - p3Y) - (p2Y - p1Y) * (p1X - p3X)) / denom;
		if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
			// Get the intersection point.
			return Optional.of(Vector.fromArray(new double[] { p1X + ua * (p2X - p1X), p1Y + ua * (p2Y - p1Y) }));
		}

		return Optional.empty();
	}
	
	private void collisionCheck() {
		Tile tile = map.getTileUnderPosition(position);
		if (tile.getValue() == 1) {
			gameOver();
		}
	}
}
