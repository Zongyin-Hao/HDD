
public class Main {
	
	public static int x = 0;
	public static int y = 0;
	public static int z = 0;
	public static Object lock1 = new Object();
	public static Object lock2 = new Object();
	public static Object lock3 = new Object();
	
	public static void setX(int n) {
		x = n;
	}
	
	public static void setY(int n) {
		y = n;
	}
	
	public static void printX() {
		System.out.println(x);
	}
	
	public static void printY() {
		System.out.println(y);
	}
	
	public static void addZ() {
		z++;
	}
	
	public static void main(String[] args) {
		Test t1 = new Test();
		t1.start();
		synchronized (lock1) {
			synchronized (lock2) {
				setX(1);
				printY();
			}
		}
		addZ();
	}	
	
}
