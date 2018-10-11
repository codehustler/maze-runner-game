package codehustler.ml.mazerunner.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor 
public class GameInputProvider implements KeyListener {
	
	private boolean turnLeft = false;
	private boolean turnRight = false;
	
	@NonNull
	private RunnerGame game;
	

	@Override
	public void keyPressed(KeyEvent e) {
		if ( e.getKeyCode() == KeyEvent.VK_LEFT) {
			turnLeft = true;
		} else if ( e.getKeyCode() == KeyEvent.VK_RIGHT) {
			turnRight = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		turnLeft = false;
		turnRight = false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println("keyTyped: " + e.getKeyChar());
		if ( e.getKeyChar() == '\n' ) {
			System.out.println("enter!!");
			game.toggleAnimationSpeed();
		} else if ( e.getKeyChar() == 'k' ) {
			game.killCurrentPopulation();
		} else if ( e.getKeyChar() == 'r' ) {
			game.toggleRender();
		} 
	}
}
