
public class Test3 extends Thread {

	@Override
	public void run() {
		synchronized (Main.lock2) {
			Main.y = 3;
		}
	}

}
