
public class Test extends Thread {

	@Override
	public void run() {
		for (int i = 10; i < 20; i++) {
			Main.addList(i);
			Main.sum += i;
		}
		Main.setList(0, -2);
		System.out.println(Main.getX(0));
	}

}
