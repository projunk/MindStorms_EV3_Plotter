import lejos.util.Delay;

public class XYPlotter {
	static Plotter plotter;
	
	public static int round(double angle) {
		return (int) Math.round((float) angle);
	}
	
	public static void penTest() {
		for (int count = 1; count <= 3; count++) {
			plotter.penDown();
			Delay.msDelay(1000);
			plotter.penUp();
			Delay.msDelay(1000);
		}
	}
	
	public static void drawNXT() {
		int heightChar = 60;
		int widthChar = 30;
		int margin = 5;
		int space = 7;
		int x0 = 10;
		int y0 = 130;
				
		// N
		int x1 = x0 + margin;
		int y1 = y0 - margin;
		int x2 = x1 + heightChar;
		int y2 = y1 - widthChar;
		plotter.drawLine(x1, y1, x2, y1);
		plotter.drawLine(x2, y1, x1, y2);
		plotter.drawLine(x1, y2, x2, y2);
		
		// X
		int y3 = y2 - space;
		int y4 = y3 - widthChar; 
		plotter.drawLine(x1, y3, x2, y4);
		plotter.drawLine(x2, y3, x1, y4);
				
		// T
		int y5 = y4 - space;
		int y6 = y5 - widthChar / 2; 
		int y7 = y6 - 2 * widthChar / 2;		
		plotter.drawLine(x2, y5, x2, y7);
		plotter.drawLine(x2, y6, x1, y6);
		
		// rectangle
		int x3 = x2 + margin;
		int y8 = y7 - margin;		
		plotter.drawRectangle(x0, y0, x3, y8);		
	}
	
	public static void drawNXTs(int prmCnt) {
		for (int i = 1; i <= prmCnt; i++) {
			drawNXT();
		}
	}
	
	public static void runTest() {
//		penTest();
		
//		plotter.drawLine(0, 0, plotter.getWidth(), plotter.getHeight());
//		plotter.drawLine(0, 0, plotter.getWidth(), 0);
//		plotter.drawRectangle(0, 0, plotter.getWidth(), plotter.getHeight());
		drawNXTs(3);
//		plotter.plotHPGL("columbia.plt");
//		plotter.plotHPGL("r15_2d_drawing.plt");		
	}
	
	public static void main(String[] args) {
		plotter = new Plotter();
		plotter.initialize(false, true);
		
		if (args.length == 1) {
			plotter.plotHPGL(args[0]);
		} else {
			runTest();
		}
		
		plotter.finalize();
	}
}
