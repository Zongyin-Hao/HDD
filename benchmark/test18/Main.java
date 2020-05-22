

public class Main {
	
	public static void main(String[] args) {
		
		Person1 p1 = new Person1("damao", 1, true, 30);
		Person2 p2 = new Person2("ermao", 2, true, 24);
		Person3 p3 = new Person3("sanmao", 3, false, 18);
		Person4 p4 = new Person4("simao", 4, false, 16);
		Person5 p5 = new Person5("wumao", 5, true, 10);
		Person6 p6 = new Person6("liumao", 6, false, 4);
		
		p1.start();
		p2.start();
		p3.start();
		p4.start();
		p5.start();
		p6.start();
		
		try {
			p1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			p2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			p3.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			p4.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			p5.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			p6.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
