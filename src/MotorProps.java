
import lejos.nxt.*;


public class MotorProps {
	public int minAngle;
	public int maxAngle;
	public int minRange;	
	public int maxRange;
	public boolean runOnPC = false;
	
	public void show() {
		if (runOnPC) {
			System.out.println("MinAngle=" + Integer.toString(minAngle));
			System.out.println("MaxAngle=" + Integer.toString(maxAngle));
			System.out.println("MinRange=" + Integer.toString(minRange));
			System.out.println("MinRange=" + Integer.toString(minRange));
		} else {
			LCD.clear();
			LCD.refresh();		
			LCD.drawString("MinAngle=" + Integer.toString(minAngle), 2, 1);
			LCD.drawString("MaxAngle=" + Integer.toString(maxAngle), 2, 2);
			LCD.drawString("MinRange=" + Integer.toString(minRange), 2, 4);		
			LCD.drawString("MaxRange=" + Integer.toString(maxRange), 2, 5);		
//			try {
//				Button.ENTER.waitForPressAndRelease();
//			} catch (InterruptedException e) {			
//			}
		}
	}
}
