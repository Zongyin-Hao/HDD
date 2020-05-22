
public class Station1 extends Thread {
	
	private HuangNiu1 huangniu1;
	
	public Station1(HuangNiu1 huangniu1) {
		this.huangniu1 = huangniu1;
	}
	
	@Override
	public void run() {
		while (true) {
			synchronized (Main.lock) {
				if (Main.tick > 0) {
					System.out.println("Station1 sell the " + Main.tick + " ticket");
					Main.tick--;
				}
				else {
					break;
				}
			}
			huangniu1.grabTicket();
			huangniu1.printTicket();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				 e.printStackTrace(); 
			}
		}
	}
	
}
