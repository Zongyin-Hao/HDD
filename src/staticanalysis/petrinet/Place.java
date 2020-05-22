package staticanalysis.petrinet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Place {
	private int id;
	private List<Transition> preSet;
	private List<Transition> postSet;
	
	public Place(int id) {
		this.id = id;
		preSet = new ArrayList<>();
		postSet = new ArrayList<>();
	}

	public int getId() {
		return id;
	}

	public void addPreSet(Transition t) {
		preSet.add(t);
	}

	public List<Transition> getPreSet() {
		return preSet;
	}

	public void addPostSet(Transition t) {
		postSet.add(t);
	}

	public List<Transition> getPostSet() {
		return postSet;
	}
	
}
