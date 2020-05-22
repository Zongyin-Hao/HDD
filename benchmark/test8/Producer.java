
public class Producer extends Thread {
	
	@Override
	public void run() {
		synchronized (Main.lock) {
			while (Main.product != 0) {
				try {
					Main.lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Producer");
			Main.product = 1;
			Main.lock.notifyAll();
		}
	}
	
}