
public class Test1 extends Thread {

	@Override
	public void run() {
		/*
		try {
            sleep(100);
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }
        */
	
		synchronized (Main.lock1) {
			Main.x = 1;
		}
	}

}
