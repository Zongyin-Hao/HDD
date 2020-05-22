
public class Bank {
	
	public static int money = 100;
	
	public void saveMoney(int x) {
		money += x;
	}
	
	public void withDrawMoney(int x) {
		money -= x;
	}
	
	public int getBalance() {
		return money;
	}
	
	public void showBalance() {
		System.out.println("Balance: " + money);
	}
	
}
