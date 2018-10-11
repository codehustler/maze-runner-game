package codehustler.ml.mazerunner.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

public class GameScreen extends JPanel {

	private static final long serialVersionUID = 1L;

	private final RunnerGame game;

	public GameScreen(RunnerGame game) throws Exception {
		this.game = game;
		this.setBackground(Color.BLACK);
	}

	@Override
	public void paint(Graphics g_) {
		Graphics2D g = (Graphics2D) g_;
		clear(g);
		renderMap(g);
		renderRunners(g);
	}

	private void renderRunners(Graphics2D g) {
		game.getRunners().forEach(m -> m.render(g));
	}

	private void renderMap(Graphics2D g) {
		game.getMap().render(g);
	}

	private void clear(Graphics2D g) {
		g.setColor(Color.RED);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
	}
}
