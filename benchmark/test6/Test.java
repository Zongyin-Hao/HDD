
public class Test extends Thread {
	
	@Override
	public void run() {
		synchronized (Main.lock2) {
			synchronized (Main.lock3) {
				Main.printX();
				Main.setY(2);
			}
		}
		Main.addZ();
	}
	
}
