
public class Test extends Thread {
	private int id;
	
	public Test(int id) {
		this.id = id;
	}
	
	@Override
	public void run() {
		synchronized (Main.lock) {
			for (int i = 0; i < 2; i++) {
				if (i % 2 == 0) {
					Main.x = id;
				}
				else {
					System.out.println(Main.x);
				}
			}
		}
		Main.y = 1;
	}

}
