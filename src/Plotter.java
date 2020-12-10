import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.nxt.ADSensorPort;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.util.Delay;


public class Plotter {
	public static NXTRegulatedMotor penMotor = Motor.A;
	public static NXTRegulatedMotor y1Motor = Motor.B;
	public static NXTRegulatedMotor y2Motor = Motor.C;
	public static NXTRegulatedMotor xMotor = Motor.D;
	public static TouchSensor yTouch1 = new TouchSensor((ADSensorPort) SensorPort.S2);
	public static TouchSensor yTouch2 = new TouchSensor((ADSensorPort) SensorPort.S3);
	public static TouchSensor xTouch = new TouchSensor((ADSensorPort) SensorPort.S4);
	public static int INIT_SPEED = 200;
	public static int DRAW_SPEED = 400; 
	public static int PEN_INIT_SPEED = 200;
	public static int PEN_DRAW_SPEED = 700; 
		
	private boolean isThePC() {
		File file = new File("thePC.txt");
		return file.exists();		
	}
	
	private enum HPGLCommand {
		PU, 
		PD,
		UNKNOWN;
		
		public static HPGLCommand convertTo(final String value) {
			if (value.equals("PU")) return PU;
			if (value.equals("PD")) return PD;
			return UNKNOWN;
		}		
	}	
	
	static Thread mainThread = Thread.currentThread(); 

	class ScaleCalc {
		private boolean rotated = false;
		private float scale = 1.0f;
		
		private ScaleCalc(int width, int height, int minX, int maxX, int minY, int maxY) {
			super();
			
			int rangeX = maxX - minX;
			int rangeY = maxY - minY;
			float mechanismRatio = (float) height / (float) width;
			float imageRatio = (float) rangeY / (float) rangeX;
			
			if ((mechanismRatio >= 1.0f && imageRatio >= 1.0f) || (mechanismRatio <= 1.0f && imageRatio <= 1.0f)) {
				rotated = false;
				if (mechanismRatio / imageRatio >= 1.0f) {
					scale = (float) width / (float) rangeX; 
				} else {
					scale = (float) height / (float) rangeY;
				}
			} else {
				rotated = true;
				if (mechanismRatio / imageRatio <= 1.0f) {
					scale = (float) width / (float) rangeY; 
				} else {
					scale = (float) height / (float) rangeX;
				}				
			}
		}

		public final int getX(int x, int y) {
			if (rotated) {
				return round(y * scale);
			} else {
				return round(x * scale);
			}			
		}

		public final int getY(int x, int y) {
			if (rotated) {
				return round(x * scale);
			} else {
				return round(y * scale);
			}			
		}		
	}
	
	class HPGL {
		private HPGLCommand command = HPGLCommand.UNKNOWN;
		private int x = 0;
		private int y = 0;
		
		public final HPGLCommand getCommand() {
			return command;
		}

		public final int getX() {
			return x;
		}

		public final int getY() {
			return y;
		}

		boolean parse(String line) {
			command = HPGLCommand.convertTo(line.substring(0, 2));
			if (command == HPGLCommand.UNKNOWN) return false;
			int pos1 = line.indexOf(",", 2);
			if (pos1 == -1) return false;
			int pos2 = line.indexOf(";", pos1 + 1);
			if (pos2 == -1) return false;

			try {
				x = Integer.parseInt(line.substring(2, pos1));
			} catch (Exception e) {
				return false;
			}
			
			try {
				y = Integer.parseInt(line.substring(pos1 + 1, pos2));
			} catch (Exception e) {
				return false;
			}			
			
			return true;
		}
	}
	
	private boolean runOnPC = isThePC();
	private boolean currentPenDown = true;
	private int currentX = 0;
	private int currentY = 0;	
	private float rangeUsageX = 1.00f;
	private float rangeUsageY = 1.00f;
	private float rangeUsagePen = 1.00f;
	private MotorProps motorPropsX, motorPropsY, motorPropsPen;
	private Mechanism mechanism = null;
			
	public final int getWidth() {
		if (mechanism == null) return 0;
		return mechanism.getWidth();
	}

	public final int getHeight() {
		if (mechanism == null) return 0;
		return mechanism.getHeight();
	}
	
	private int round(double value) {
		return (int) Math.round((float) (value));
	}	
			
	public MotorProps getMotorPropsX() {
		return motorPropsX;
	}

	public MotorProps getMotorPropsY() {
		return motorPropsY;
	}

	public MotorProps getMotorPropsPen() {
		return motorPropsPen;
	}
		
	private void setRanges(MotorProps motorProps, float rangeUsage) {
		int rangeLossAngle = round((1.0 - rangeUsage) / 2.0 * motorProps.maxAngle);
		motorProps.minRange = motorProps.minAngle + rangeLossAngle;
		motorProps.maxRange = motorProps.maxAngle - rangeLossAngle;
		motorProps.runOnPC = runOnPC;
	}
	
	private void initRunOnPC() {
		motorPropsX = new MotorProps();
		motorPropsX.minAngle = 0;
		motorPropsX.maxAngle = 1200;
		setRanges(motorPropsX, rangeUsageX);
		motorPropsY = new MotorProps();
		motorPropsY.minAngle = 0;
		motorPropsY.maxAngle = 1200;
		setRanges(motorPropsY, rangeUsageY);		
		motorPropsPen = new MotorProps();
		motorPropsPen.minAngle = -887;
		motorPropsPen.maxAngle = 0;
		setRanges(motorPropsPen, rangeUsagePen);		
	}
	
	private void initMotorY() {
		motorPropsY = new MotorProps();
		y1Motor.setSpeed(INIT_SPEED);
		y2Motor.setSpeed(INIT_SPEED);
		y1Motor.backward();
		y2Motor.backward();
		for (;;) {
			if (yTouch1.isPressed()) {
				y1Motor.stop();				
			}
			if (yTouch2.isPressed()) {
				y2Motor.stop();				
			}			
			if ((yTouch1.isPressed()) && (yTouch2.isPressed())) {
				y1Motor.stop();
				y2Motor.stop();	
				break;
			}
			Delay.msDelay(100);
		}

		Delay.msDelay(500);	
		y2Motor.rotate(90, true);		
		y1Motor.rotate(90, false);
		
		while (y2Motor.isMoving()) {
			Delay.msDelay(100);
		}		
		
		y1Motor.resetTachoCount();		
		y2Motor.resetTachoCount();
		
		Delay.msDelay(500);			
		y1Motor.flt();
		y2Motor.flt();
		motorPropsY.minAngle = 0;
		motorPropsY.maxAngle = 1200;
		setRanges(motorPropsY, rangeUsageY);
		y1Motor.setSpeed(DRAW_SPEED);
		y2Motor.setSpeed(DRAW_SPEED);		
		motorPropsY.show();
	}
			
	private void initMotorX() {		
		motorPropsX = new MotorProps();
		xMotor.setSpeed(INIT_SPEED);
		xMotor.backward();
		for (;;) {
			if (xTouch.isPressed()) {
				xMotor.stop();
				break;
			}
			Delay.msDelay(100);
		}

		Delay.msDelay(500);
		xMotor.rotate(90, false);		
		xMotor.resetTachoCount();
		
		Delay.msDelay(500);		
		xMotor.flt();
		motorPropsX.minAngle = 0;
		motorPropsX.maxAngle = 1200;
		setRanges(motorPropsX, rangeUsageX);
		xMotor.setSpeed(DRAW_SPEED);
		motorPropsX.show();
	}
		
	private void initMotorPen() {
		motorPropsPen = new MotorProps();
		penMotor.setSpeed(PEN_INIT_SPEED);
		penMotor.forward();
		int cnt = 0;
		for (;;) {
			cnt++;
			if ((penMotor.isStalled()) || (cnt > 50)) {
				xMotor.stop();
				break;
			}
			Delay.msDelay(100);
		}		
		
		Delay.msDelay(500);		
		penMotor.rotate(-720, false);
		penMotor.resetTachoCount();
		Delay.msDelay(500);	
		penMotor.flt();		
		motorPropsPen.minAngle = 0;
		motorPropsPen.maxAngle = 720;
		setRanges(motorPropsPen, rangeUsagePen);
		penMotor.setSpeed(PEN_DRAW_SPEED);
		motorPropsPen.show();
	}
		
	public void initialize(boolean debug, boolean waitForPen) {
		if (runOnPC) {
			initRunOnPC();
		} else {
			initMotorPen();
			penUp();			
			initMotorX();
			initMotorY();						
		}
				
		mechanism = new Mechanism(170, 170, motorPropsX.minRange, motorPropsX.maxRange, motorPropsY.minRange, motorPropsY.maxRange);
		home();
				
		if (waitForPen) {
			Sound.beepSequenceUp();			
			debugString("Insert Pen", true);
			Delay.msDelay(2000);
		}
	}
	
	public void finalize() {
		home();
		if (!runOnPC) {
			y1Motor.flt();
			y2Motor.flt();
			xMotor.flt();
			penMotor.flt();			
			Sound.beepSequenceUp();
			debugString("Ready", false);
		}
	}
	
	public void penUp() {
		if (!runOnPC) {
			if (currentPenDown) {
				currentPenDown = false;
				penMotor.flt();
				penMotor.rotateTo(motorPropsPen.minRange);
			}
		}
	}
	
	public void penDown() {
		if (!runOnPC) {
			if (!currentPenDown) {
				currentPenDown = true;
				penMotor.flt();
				penMotor.rotateTo(motorPropsPen.maxRange);
				penMotor.flt();
				Delay.msDelay(100);
			}
		}
	}

	public void debugString(String st, boolean waitForEnter) {
		// the ';' delimiter is used to split over multiple lines
		if (runOnPC) {
			String st2 = st;	
			int pos; 
			while ((pos = st2.indexOf(';')) != -1) {
				System.out.print(st2.substring(0, pos) + "\t");
				st2 = st2.substring(pos + 1);
			}
			System.out.println(st2);			
		} else {
			LCD.clear();
			LCD.refresh();
		
			String st2 = st;	
			int pos, y = 0; 
			while ((pos = st2.indexOf(';')) != -1) {
				LCD.drawString(st2.substring(0, pos), 0, y);
				st2 = st2.substring(pos + 1);
				y++;
			}
			LCD.drawString(st2, 0, y);
			LCD.refresh();
		
			if (waitForEnter) { 
				Button.waitForAnyPress();
				LCD.clear();
				LCD.refresh();
			}
		}
	}
		
	private void gotoXY(int x, int y) {		
		mechanism.solve(x, y);
		if (mechanism.errorFound()) {
			debugString("Out of range:" + Integer.toString(x) + "," + Integer.toString(y), true);
			penUp();
			return;
		}
		
		double alfa = mechanism.getAlfa();
		double beta = mechanism.getBeta();		
									
		if (!runOnPC) {
			xMotor.rotateTo(round(alfa), true);
			y1Motor.rotateTo(round(beta), true);
			y2Motor.rotateTo(round(beta), true);
						
			while ((y1Motor.isMoving()) || (y2Motor.isMoving()) || (xMotor.isMoving())) {
				Delay.msDelay(1);
			}
			y1Motor.flt();
			y2Motor.flt();
			xMotor.flt();			
		}
		
		// update current coordinates
		currentX = x;
		currentY = y;
	}
		
	private void interpolateLinear(int x1, int y1, int x2, int y2) {
		int stepSizeX = 1;
		int stepSizeY = 1;
		if ((Math.abs(x2 - x1) < stepSizeX) && (Math.abs(y2 - y1) < stepSizeY)) {
			return;
		}
		if ((Math.abs(x2 - x1) < stepSizeX) || (Math.abs(y2 - y1) < stepSizeY)) {
			gotoXY(x2, y2);
			return;
		}
		
		if (x1 < x2) {
			float a = (float)(y1 - y2) / (float)(x1 - x2);
			float b = y1 - a * x1;
			for (int x = x1 + 1; x <= x2; x = x + stepSizeX) {
				int y = round(a * x + b);
				gotoXY(x, y);
			}
		} else {
			if (x1 > x2) {
				float a = (float)(y1 - y2) / (float)(x1 - x2);
				float b = y1 - a * x1;
				for (int x = x1 - 1; x >= x2; x = x - stepSizeX) {
					int y = round(a * x + b);
					gotoXY(x, y);
				}				
			} else {
				// x1 == x2
				if (y1 < y2) {
					for (int y = y1 + 1; y <= y2; y = y + stepSizeY) {
						gotoXY(x1, y);
					}
				} else {
					if (y1 > y2) {
						for (int y = y1 - 1; y >= y2; y = y - stepSizeY) {
							gotoXY(x1, y);
						}
					} else {
						// x1 == x2; y1 == y2
					}
				}
			}
		}		
	}

	
	public void home() {
		penUp();
		gotoXY(0, 0);
	}
	
	public void moveXY(int x, int y) {
		debugString("moveXY;" + Integer.toString(x) + "," + Integer.toString(y), false);
		penUp();
		gotoXY(x, y);
	}
	
	public void drawXY(int x, int y) {
		debugString("drawXY;" + Integer.toString(x) + "," + Integer.toString(y), false);		
		penDown();
		interpolateLinear(currentX, currentY, x, y);
	}

	public void drawLine(int x1, int y1, int x2, int y2) {
//		debugString(Integer.toString(x1) + "," + Integer.toString(y1) + ";" + Integer.toString(x2) + ","+ Integer.toString(y2));		
		if ((x1 != currentX) || (y1 != currentY)) moveXY(x1, y1);
		drawXY(x2, y2);
	}
	
	public void drawRectangle(int x1, int y1, int x2, int y2) {
		moveXY(x1, y1);
		drawXY(x2, y1);
		drawXY(x2, y2);
		drawXY(x1, y2);
		drawXY(x1, y1);				
	}
	
	@SuppressWarnings("deprecation")
	public void plotHPGL(String prmFileName) {
		try{
			debugString("Processing", false);
			FileInputStream fstream = new FileInputStream(new File(prmFileName));
			DataInputStream in = new DataInputStream(fstream);
			String strLine;
			HPGL hpgl = new HPGL();
			
			// get min/max coordinates to calculate autoscaling
			// compare action below does not work if Integer.MIN_VALUE is used; perhaps because of different size of "int" and "Integer" on the brick
			int minX = 100000;
			int minY = 100000;			
			int maxX = -100000;
			int maxY = -100000;
			while ((strLine = in.readLine()) != null) {
				if (hpgl.parse(strLine)) {
					switch (hpgl.getCommand()) {					
						case PU:
						case PD:
							if (hpgl.getX() < minX) {
								minX = hpgl.getX();
							}
							if (hpgl.getY() < minY) {
								minY = hpgl.getY();
							}
							if (hpgl.getX() > maxX) {
								maxX = hpgl.getX();
							}
							if (hpgl.getY() > maxY) {
								maxY = hpgl.getY();
							}							
							break;
					}
				}
			}
			in.close();
			fstream.close();
			debugString("minX=" + Integer.toString(minX) + ";maxX=" + Integer.toString(maxX) + ";minY=" + Integer.toString(minY) + ";maxY="+ Integer.toString(maxY), false);
			ScaleCalc scaleCalc= new ScaleCalc(mechanism.getWidth(), mechanism.getHeight(), minX, maxX, minY, maxY);
			
			// plot			
			fstream = new FileInputStream(new File(prmFileName));			
			in = new DataInputStream(fstream);
			while ((strLine = in.readLine()) != null) {				
				if (hpgl.parse(strLine)) {
					switch (hpgl.getCommand()) {
						case PU:
							moveXY(scaleCalc.getX(hpgl.getX(), hpgl.getY()), scaleCalc.getY(hpgl.getX(), hpgl.getY()));
							break;
						case PD:
							drawXY(scaleCalc.getX(hpgl.getX(), hpgl.getY()), scaleCalc.getY(hpgl.getX(), hpgl.getY()));
							break;
					}
				}
			}
			
			in.close();
			fstream.close();
		} catch (Exception e) {
			debugString(e.getMessage(), true);
		}
	}
}

