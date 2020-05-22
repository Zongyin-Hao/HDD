
public class Station2 extends Thread {
	
	private HuangNiu2 huangniu2;
	
	public Station2(HuangNiu2 huangniu2) {
		this.huangniu2 = huangniu2;
	}
	
	@Override
	public void run() {
		while (true) {
			synchronized (Main.lock) {
				if (Main.tick > 0) {
					System.out.println("Station2 sell the " + Main.tick + " ticket");
					Main.tick--;
				}
				else {
					break;
				}
			}
			huangniu2.grabTicket();
			huangniu2.printTicket();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				 e.printStackTrace(); 
			}
		}
	}
	
}
