
public class Main {
	
	public static int product = 0;
	public static Object lock = new Object();
	
	public static void main(String[] args) {
		Producer producer = new Producer();
		Consumer consumer = new Consumer();
		producer.start();
		consumer.start();
	}
	
}