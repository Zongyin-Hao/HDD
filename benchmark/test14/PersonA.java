
public class PersonA extends Thread {
	
	private Bank bank;
	
	public PersonA(Bank bank) {
		this.bank = bank;
	}
	
	@Override
	public void run() {
		while (true) {
			synchronized (Main.lock) {
				if (bank.getBalance() > 0) {
					bank.withDrawMoney(20);
					System.out.println("A withdraw 20");
					bank.showBalance();
				}
				else {
					break;
				}
				//bank.saveMoney(10);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace(); 
				}
			}
		}
	}
	
}
