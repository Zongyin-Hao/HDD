
public class Main {
	
	public static int sum = 0;
	public static int[] array = new int [3];

	public static void main(String[] args) {
		array[0] = 0;
		array[1] = 1;
		array[2] = 2;
		sum = 3;
		Test t1 = new Test();
		t1.start();
		for (int i = 0; i < 3; i++) {
			if (i != 0) {
				System.out.print(" ");
			}
			System.out.print(array[i]);
		}
		System.out.println();
		System.out.println(sum);
	}

}
