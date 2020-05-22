
public class Test1 extends Thread {
	
	@Override
	public void run() {
		Main.x = 1;
		System.out.println(Main.x);
	}
	
}
