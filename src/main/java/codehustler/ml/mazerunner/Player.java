package codehustler.ml.mazerunner;

import java.util.Comparator;

/**
 * 
 * basic player interface, used for human and ai players
 *
 */
public interface Player extends Comparable<Player> {
	
	enum Result {COMPLETED, DNF}
	/**
	 * @return the score of the player
	 */
	double getScore();
	void setScore(double score);

	Result getResult();
	void setResult(Result result);
	
	
	/**
	 * @param inputs the observations made by the player
	 */
	void setInputs(double[] inputs);
	
	boolean isTurnLeft();
	
	boolean isTurnRight();
	
	void save();
	void restore(String id);
	
	default int compareTo(Player o) {
		return Comparator.comparingDouble(Player::getScore).compare(this, o);
	}
}
