package codehustler.ml.mazerunner;

import codehustler.ml.mazerunner.Player;
import lombok.Getter;
import lombok.Setter;


public abstract class AbstractPlayer implements Player {

	
	@Getter @Setter
	protected double score;
	
	@Setter
	protected double[] inputs; 
	
	@Getter @Setter
	protected boolean turnLeft;
	
	@Getter @Setter
	protected boolean turnRight;
		
}
