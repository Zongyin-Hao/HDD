
import java.util.List;
import java.util.ArrayList;

public class Main {

	public static int sum = 0;
	public static List<Integer> list = new ArrayList<> ();

	public static synchronized void addList(int x) {
		list.add(x);
	}
	
	public static synchronized void setList(int id, int x) {
		list.set(id, x);
	}
	
	public static synchronized int getX(int id) {
		return list.get(id);
	}

	public static void main(String[] args) {
		Test t1 = new Test();
		t1.start();
		for (int i = 0; i < 10; i++) {
			addList(i);
		}
		setList(0, -1);
		System.out.println(getX(0));
		System.out.println(sum);
	}

}
