package staticanalysis.petrinet;

import java.util.List;
import java.util.ArrayList;

public class Transition {
	protected int id;
	protected List<Place> preSet;
	protected List<Place> postSet;
	protected String signature; //语句的唯一标识
	protected String message; //调试信息

	public Transition(int id, String signature, String message) {
		this.id = id;
		preSet = new ArrayList<>();
		postSet = new ArrayList<>();
		this.signature = signature;
		this.message = message;
	}

	public int getId() {
		return id;
	}

	public void addPreSet(Place p) {
		preSet.add(p);
	}

	public List<Place> getPreSet() {
		return preSet;
	}

	public void addPostSet(Place p) {
		postSet.add(p);
	}

	public List<Place> getPostSet() {
		return postSet;
	}
	
	public String getSignature() {
		return signature;
	}
	
	public String getMessage() {
		return message;
	}
	
}
