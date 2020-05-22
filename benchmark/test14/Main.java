
public class Main {
	
	public static Object lock = new Object();
	
	public static void main(String[] args) {
		Bank bank = new Bank();
		PersonA pA = new PersonA(bank);
		PersonB pB = new PersonB(bank);
		pA.start();
		pB.start();
	}	
	
}
