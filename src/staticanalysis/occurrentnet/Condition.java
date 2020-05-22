package staticanalysis.occurrentnet;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import staticanalysis.petrinet.*;

public class Condition {
	
	private Place map;
	private int id;
	private Event preSet;
	private List<Event> postSet;
	private Set<Condition> concurrentSet;
	
	public Condition(Place map, int id) {
		this.map = map;
		this.id = id;
		postSet = new ArrayList<> ();
		concurrentSet = new HashSet<> ();
	}
	
	public Place getMap() {
		return map;
	}
	
	public int getId() {
		return id;
	}
	
	public void setPreSet(Event e) {
		preSet = e;
	}
	
	public Event getPreSet() {
		return preSet;
	}
	
	public void addPostSet(Event e) {
		postSet.add(e);
	}
	
	public List<Event> getPostSet() {
		return postSet;
	}
	
	public void addConcurrentSet(Condition c) {
		concurrentSet.add(c);
	}
	
	public void setConcurrentSet(Set<Condition> set) {
		concurrentSet = set;
	}

	public Set<Condition> getConcurrentSet() {
		return concurrentSet;
	}
	
	public boolean isConcurrentWith(Condition c) {
		return concurrentSet.contains(c);
	}
	
}
