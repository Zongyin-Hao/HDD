
public class Test2 extends Thread {
	
	public void clear() {
		System.out.println("Test2 clear");
		Main.x = 0;
	}
	
	public void set(int n) {
		synchronized (Main.lock2) {
			System.out.println("Test2 set " + n);
			Main.y = n;
		}
	}
	
	public void add(int n) {
		synchronized (Main.lock2) {		
			System.out.println("Test2 add " + n);
			Main.y += n;
		}
	}
	
	public void sub(int n) {
		synchronized (Main.lock2) {
			System.out.println("Test2 sub " + n);
			Main.y -= n;
		}
	}
	
	public void mul(int n) {
		synchronized (Main.lock2) {
			System.out.println("Test2 mul " + n);
			Main.y *= n;
		}
	}
	
	public void div(int n) {
		synchronized (Main.lock2) {
			System.out.println("Test2 div " + n);
			Main.y /= n;
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
		
		set(2);
		add(40);
		for (int i = 1; i <= 3; i++) {
			sub(i);
		}
		mul(40);
		for (int i = 1; i <= 3; i++) {
			div(i);
		}
		synchronized (Main.lock) {
			Main.printY();
		}
	}	
	
}
