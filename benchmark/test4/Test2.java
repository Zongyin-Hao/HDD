
public class Test2 extends Thread {

	@Override
	public void run() {
		synchronized (Main.lock2) {
			Main.x = 2;
			System.out.println(Main.x);
		}
	}

}
