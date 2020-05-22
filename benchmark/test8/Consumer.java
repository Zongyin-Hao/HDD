
public class Consumer extends Thread {
	
	@Override
	public void run() {
		synchronized (Main.lock) {
			while (Main.product == 0) {
				try {
					Main.lock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Consumer");
			Main.product = 0;
			Main.lock.notifyAll();
		}
	}
	
}