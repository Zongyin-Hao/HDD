
public class Main {

	public static int tick = 6;
	public static Object lock = new Object();

	public static void main(String[] args) {
		HuangNiu1 huangniu1 = new HuangNiu1();
		HuangNiu2 huangniu2 = new HuangNiu2();
		HuangNiu3 huangniu3 = new HuangNiu3();
		Station1 station1 = new Station1(huangniu1);
		Station2 station2 = new Station2(huangniu2);
		Station3 station3 = new Station3(huangniu3);
		
		station1.start();
		station2.start();
		station3.start();
	}
	
}
