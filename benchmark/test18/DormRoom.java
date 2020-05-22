

import java.util.Set;
import java.util.HashSet;

public class DormRoom {
	
	public static Object lock1 = new Object();
	public static Object lock2 = new Object();
	public static Object lock3 = new Object();
	public static Object lock4 = new Object();
	
	public static int dormRoomSize1 = 0;
	public static int dormRoomSize2 = 0;
	public static int dormRoomSize3 = 0;
	public static int dormRoomSize4 = 0;
	
	public static Set<Integer> dormRoom1 = new HashSet<> ();
	public static Set<Integer> dormRoom2 = new HashSet<> ();
	public static Set<Integer> dormRoom3 = new HashSet<> ();
	public static Set<Integer> dormRoom4 = new HashSet<> ();
	
	public static void entryDormRoom1(int id) {
		synchronized (lock1) {
			dormRoomSize1++;
			dormRoom1.add(id);
		}
	}
	
	public static void entryDormRoom2(int id) {
		synchronized (lock2) {
			dormRoomSize2++;
			dormRoom2.add(id);
		}
	}
	
	public static void entryDormRoom3(int id) {
		synchronized (lock3) {
			dormRoomSize3++;
			dormRoom3.add(id);
		}
	}
	
	public static void entryDormRoom4(int id) {
		synchronized (lock4) {
			dormRoomSize4++;
			dormRoom4.add(id);
		}
	}
	
	public static void leaveDormRoom1(int id) {
		synchronized (lock1) {
			if (dormRoom1.contains(id)) {
				dormRoomSize1--;
				dormRoom1.remove(id);
			}
			else {
				System.out.println("Dorm room 1 does not have person " + id);
			}
		}
	}
	
	public static void leaveDormRoom2(int id) {
		synchronized (lock2) {
			if (dormRoom2.contains(id)) {
				dormRoomSize2--;
				dormRoom2.remove(id);
			}
			else {
				System.out.println("Dorm room 2 does not have person " + id);
			}
		}
	}
	
	public static void leaveDormRoom3(int id) {
		synchronized (lock3) {
			if (dormRoom3.contains(id)) {
				dormRoomSize3--;
				dormRoom3.remove(id);
			}
			else {
				System.out.println("Dorm room 3 does not have person " + id);
			}
		}
	}
	
	public static void leaveDormRoom4(int id) {
		synchronized (lock4) {
			if (dormRoom4.contains(id)) {
				dormRoomSize4--;
				dormRoom4.remove(id);
			}
			else {
				System.out.println("Dorm room 4 does not have person " + id);
			}
		}
	}

	public static void showDormRoom1() {
		synchronized (lock1) {
			String person = "";			
			for (Integer p : dormRoom1) {
				person += p + " ";
			}
			System.out.println("Dorm room 1 has " + dormRoomSize1 + " person:" + person);
		}
	}
	
	public static void showDormRoom2() {
		synchronized (lock2) {
			String person = "";			
			for (Integer p : dormRoom2) {
				person += p + " ";
			}
			System.out.println("Dorm room 2 has " + dormRoomSize2 + " person:" + person);
		}
	}
	
	public static void showDormRoom3() {
		synchronized (lock3) {
			String person = "";			
			for (Integer p : dormRoom3) {
				person += p + " ";
			}
			System.out.println("Dorm room 3 has " + dormRoomSize3 + " person:" + person);
		}
	}
	
	public static void showDormRoom4() {
		synchronized (lock4) {
			String person = "";			
			for (Integer p : dormRoom4) {
				person += p + " ";
			}
			System.out.println("Dorm room 4 has " + dormRoomSize4 + " person:" + person);
		}
	}
	
}
