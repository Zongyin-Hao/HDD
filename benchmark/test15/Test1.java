
public class Test1 extends Thread {
	
	public void clear() {
		System.out.println("Test1 clear");
			Main.y = 0;
	}
	
	public void set(int n) {
		synchronized (Main.lock1) {
			System.out.println("Test1 set " + n);
			Main.x = n;
		}
	}
	
	public void add(int n) {
		synchronized (Main.lock1) {		
			System.out.println("Test1 add " + n);
			Main.x += n;
		}
	}
	
	public void sub(int n) {
		synchronized (Main.lock1) {
			System.out.println("Test1 sub " + n);
			Main.x -= n;
		}
	}
	
	public void mul(int n) {
		synchronized (Main.lock1) {
			System.out.println("Test1 mul " + n);
			Main.x *= n;
		}
	}
	
	public void div(int n) {
		synchronized (Main.lock1) {
			System.out.println("Test1 div " + n);
			Main.x /= n;
		}
	}
	
	@Override
	public void run() {
		clear();
		
		try {  
            sleep(100); 
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }
		
		set(1);
		for (int i = 1; i <= 3; i++) {
			add(i);
		}
		sub(4);
		for (int i = 1; i <= 3; i++) {
			mul(i);
		}
		div(4);
		synchronized (Main.lock) {
			Main.printX();
		}
	}	
	
}
