
public class Test2 extends Thread {

	@Override
	public void run() {
		synchronized (Main.lock) {
			Main.x = 2;
			System.out.println(Main.x);
		}
	}

}
