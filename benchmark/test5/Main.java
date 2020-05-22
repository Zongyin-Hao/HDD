
public class Main {

	public static int x = 0;
	
	public static synchronized void setX(int y) {
		x = y;
	}
	
	public static void main(String[] args) {
		Test1 t1 = new Test1();
		Test2 t2 = new Test2();
		t1.start();
		t2.start();
		x = 3;
		System.out.println(x);
	}
	
}
