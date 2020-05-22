

public class Person6 extends Thread {
	
	private String name;
	private Integer id;
	private boolean male;
	private Integer age;
	
	public Person6(String name, Integer id, boolean male, Integer age) {
		this.name = name;
		this.id = id;
		this.male = male;
		this.age = age;
		
		DormRoom.entryDormRoom4(id);
		System.out.println(this + " entry dorm room 4");
	}
	
	@Override
	public void run() {
		DormRoom.leaveDormRoom4(id);
		DormRoom.entryDormRoom2(id);
		System.out.println("person " + id + ": dorm room 4 to 2");
		
		DormRoom.leaveDormRoom2(id);
		DormRoom.entryDormRoom3(id);
		System.out.println("person " + id + ": dorm room 2 to 3");
		
		DormRoom.leaveDormRoom3(id);
		DormRoom.entryDormRoom1(id);
		System.out.println("person " + id + ": dorm room 3 to 1");
		
		DormRoom.showDormRoom4();
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
