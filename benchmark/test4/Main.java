
public class Main {

	public static int x = 0;
	public static Object lock1 = new Object();
	public static Object lock2 = new Object();
	
	public static void main(String[] args) {
		Test1 t1 = new Test1();
		Test2 t2 = new Test2();
		t1.start();
		t2.start();
	}
	
}
