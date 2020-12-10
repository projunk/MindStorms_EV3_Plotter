
public class Mechanism {
	private int height;
	private int width;
	private int minAlfa;
	private int maxAlfa;
	private int minBeta;
	private int maxBeta;
	private Solution solution;
	
	public class Solution {
		double alfaDouble;
		double betaDouble;
		boolean error;
	}
		
	public Mechanism(int prm_Height, int prm_Width, int prm_minAlfa, int prm_maxAlfa, int prm_minBeta, int prm_maxBeta) {
		height = prm_Height;
		width = prm_Width;
		// motor B, X
		minAlfa = prm_minAlfa;
		maxAlfa = prm_maxAlfa;
		// motor C, Y
		minBeta = prm_minBeta;
		maxBeta = prm_maxBeta;
		solution = new Solution();
	}
	
	private int round(double value) {
		return (int) Math.round((float) (value));
	}
	
	public final int getHeight() {
		return height;
	}

	public final int getWidth() {
		return width;
	}

	public int getMinAlfa() {
		return minAlfa;
	}
	
	public int getMaxAlfa() {
		return maxAlfa;
	}
		
	public int getX(double alfa) {
		return round(((float)(alfa - minAlfa) / (float)(maxAlfa - minAlfa)) * (float) width);
	}
	
	public int getY(double beta) {
		return round(((float)(maxBeta - beta) / (float)(maxBeta - minBeta)) * (float) height);
	}
			
	public void solve(int x, int y) {
		solution.error = false;
		if ((x < 0) || (x > width) || (y < 0) || (y > height)) {
			solution.error = true;
		} else {		
			solution.alfaDouble = minAlfa + round(((float) x / (float) width) * (maxAlfa - minAlfa));
			solution.betaDouble = minBeta + round(((float) y / (float) height) * (maxBeta - minBeta));
		}
	}	
	
	public double getAlfa() {
		return solution.alfaDouble; 
	}
	
	public double getBeta() {
		return solution.betaDouble; 
	}
	
	public boolean errorFound() {
		return solution.error; 
	}	
}
