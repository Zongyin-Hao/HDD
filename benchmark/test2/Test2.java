
public class Test2 extends Thread {

	@Override
	public void run() {
		Main.x = 2;
		System.out.println(Main.x);
	}

}
