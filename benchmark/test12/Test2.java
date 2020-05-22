
public class Test2 extends Thread {

	@Override
	public void run() {
		try {
            sleep(100);
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }
        
		synchronized (Main.lock1) {
			Main.x = 2;
		}
	}

}
