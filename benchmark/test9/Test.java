
public class Test extends Thread {

	@Override
	public void run() {
		Main.array[0]++;
		Main.sum++;
		Main.array[1]++;
		Main.sum++;
		Main.array[2]++;
		Main.sum++;
	}

}
