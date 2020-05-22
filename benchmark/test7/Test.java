
public class Test extends Thread {
	
	@Override
	public void run() {
		synchronized (Main.lock3) {
			synchronized (Main.lock4) {
				Main.printX();
				Main.setY(2);
			}
		}
		synchronized (Main.lock5) {
			Main.addZ();
		}
	}
	
}
