

public class Person3 extends Thread {
	
	private String name;
	private Integer id;
	private boolean male;
	private Integer age;
	
	public Person3(String name, Integer id, boolean male, Integer age) {
		this.name = name;
		this.id = id;
		this.male = male;
		this.age = age;
		
		DormRoom.entryDormRoom2(id);
		System.out.println(this + " entry dorm room 2");
	}
	
	@Override
	public void run() {
		DormRoom.leaveDormRoom2(id);
		DormRoom.dormRoomSize2--;
		DormRoom.entryDormRoom1(id);
		DormRoom.dormRoomSize1++;
		System.out.println("person " + id + ": dorm room 2 to 1");
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		DormRoom.leaveDormRoom1(id);
		DormRoom.entryDormRoom3(id);
		System.out.println("person " + id + ": dorm room 1 to 3");
		
		DormRoom.leaveDormRoom3(id);
		DormRoom.entryDormRoom4(id);
		System.out.println("person " + id + ": dorm room 3 to 4");
		
		DormRoom.showDormRoom2();
	}
	
	@Override
	public String toString() {
		String s = "name = " + name + " ";
		s += "id = " + id + " ";
		s += "sex = " + (male ? "male" : "female") + " ";
		s += "age = " + age + " ";
		return s;
	}
	
}
