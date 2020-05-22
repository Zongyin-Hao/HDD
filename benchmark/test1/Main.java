
public class Main {
	public static Object lock = new Object();
	public static int x = 0;
	public static int y = 0;
	public static void main(String[] args) throws InterruptedException {
		Test t1 = new Test(1);
		Test t2 = new Test(2);
		Test t3 = new Test(3);
		t1.start();
		t2.start();
		t3.start();
	}
}
