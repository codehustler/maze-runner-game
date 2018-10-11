package codehustler.ml.mazerunner.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import codehustler.ml.mazerunner.Player;
import codehustler.ml.mazerunner.PlayerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class RunnerGame extends JFrame {
	public static final Random R = new Random(System.currentTimeMillis());
	
	private static final long serialVersionUID = 1L;
	private static final int SLOW_DELAY = 10;
	
	
	@Getter @Setter
	private int targetLaps = 3;

	private GameScreen gameScreen;
	@Getter
	private GameMap map;

	private int generationCounter = 0;
	
	@Getter
	private List<MazeRunner> runners = Collections.synchronizedList(new ArrayList<>());

	@Setter
	private int selectedDelay = 0;

	@Setter
	private boolean rendergame = true;

	@Getter
	private long timecode = 0;

	@Setter
	private PlayerFactory playerFactory;

	private String[] maps = new String[] { "map_01.map", "map_02.map", "map_03.map" };
	

	@SneakyThrows
	private GameMap pickRandomMap() {
		return new GameMap(maps[2], this);
	}

	public RunnerGame() throws Exception {
		this.setSize(800, 800);
		this.setTitle("AI Runner");
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.addKeyListener(new GameInputProvider(this));

		this.map = pickRandomMap();

		gameScreen = new GameScreen(this);

		this.add(gameScreen, BorderLayout.CENTER);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				RunnerGame.this.setVisible(true);
			}
		});
	}


	private List<MazeRunner> createRunners(Set<Player> players) {
		return players.stream().map(this::createRunner).collect(Collectors.toList());
	}

	private MazeRunner createRunner(Player player) {
		return new MazeRunner(player, this);
	}

	public void run() {
		System.out.println("creating initial population");
		runners.clear();
		runners.addAll(createRunners(playerFactory.createPlayers()));

		while (true) {
			updateGame();
			if (rendergame && selectedDelay > 0) {
				try {
					Thread.sleep(selectedDelay);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	private void resetPopulation() {
		generationCounter++;
		map.clearVisitorCache();
		runners.clear();
		runners.addAll(createRunners(playerFactory.createPlayers()));
	}

	private boolean isPopulationAlive() {
		return this.runners.stream().filter(m -> !m.isGameOver()).count() > 0;
	}

	public void updateGame() {
		if (!isPopulationAlive()) {
			this.map = pickRandomMap();
			resetPopulation();
		}

		runners.parallelStream().forEach(MazeRunner::update);
		this.setTitle(String.format("AI Runner - Generation: %s Population: %s", generationCounter,
				runners.stream().filter(m -> !m.isGameOver()).count()));

		if (rendergame) {
			gameScreen.repaint();
		}

		timecode++;
	}

	public void toggleAnimationSpeed() {
		if (selectedDelay == SLOW_DELAY) {
			selectedDelay = 0;
		} else {
			selectedDelay = SLOW_DELAY;
		}
	}

	public void killCurrentPopulation() {
		runners.forEach(m -> m.setGameOver(true));
	}

	public void toggleRender() {
		rendergame = !rendergame;
	}
}
