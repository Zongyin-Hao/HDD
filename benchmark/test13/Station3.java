
public class Station3 extends Thread {
	
	private HuangNiu3 huangniu3;
	
	public Station3(HuangNiu3 huangniu3) {
		this.huangniu3 = huangniu3;
	}
	
	@Override
	public void run() {
		while (true) {
			synchronized (Main.lock) {
				if (Main.tick > 0) {
					System.out.println("Station3 sell the " + Main.tick + " ticket");
					Main.tick--;
				}
				else {
					break;
				}
			}
			huangniu3.grabTicket();
			huangniu3.printTicket();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				 e.printStackTrace(); 
			}
		}
	}
	
}
