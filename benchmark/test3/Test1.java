
public class Test1 extends Thread {
	
	@Override
	public void run() {
		synchronized (Main.lock) {
			Main.x = 1;
			System.out.println(Main.x);
		}
	}
	
}
