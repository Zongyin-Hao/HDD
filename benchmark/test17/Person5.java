

public class Person5 extends Thread {
	
	private String name;
	private Integer id;
	private boolean male;
	private Integer age;
	
	public Person5(String name, Integer id, boolean male, Integer age) {
		this.name = name;
		this.id = id;
		this.male = male;
		this.age = age;
	
		DormRoom.entryDormRoom3(id);
		System.out.println(this + " entry dorm room 3");
	}
	
	@Override
	public void run() {
		DormRoom.leaveDormRoom3(id);
		DormRoom.entryDormRoom4(id);
		System.out.println("person " + id + ": dorm room 3 to 4");
		
		DormRoom.leaveDormRoom4(id);
		DormRoom.entryDormRoom1(id);
		System.out.println("person " + id + ": dorm room 4 to 1");
		
		DormRoom.leaveDormRoom1(id);
		DormRoom.entryDormRoom2(id);
		System.out.println("person " + id + ": dorm room 1 to 2");
		
		DormRoom.showDormRoom3();
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
