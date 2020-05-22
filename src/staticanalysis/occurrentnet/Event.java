package staticanalysis.occurrentnet;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import staticanalysis.petrinet.*;

public class Event implements Comparable<Event> {
	
	private Transition map;
	private int id;
	private List<Condition> preSet;
	private List<Condition> postSet;
	private List<Event> configuration;
	private Map<Place, Integer> mark;
	private List<Transition> lexOrder;
	private Set<String> lockSet;
	
	public Event(Transition map, int id) {
		this.map = map;
		this.id = id;
		preSet = new ArrayList<> ();
		postSet = new ArrayList<> ();
		configuration = new ArrayList<>();
		mark = new HashMap<>();
		lexOrder = new ArrayList<> ();
		lockSet = new HashSet<> ();
	}
	
	public Transition getMap() {
		return map;
	}
	
	public int getId() {
		return id;
	}
	
	public void addPreSet(Condition c) {
		preSet.add(c);
	}
	
	public List<Condition> getPreSet() {
		return preSet;
	}
	
	public void addPostSet(Condition c) {
		postSet.add(c);
	}
	
	public List<Condition> getPostSet() {
		return postSet;
	}
	
	public void setConfiguration(List<Event> cfg) {
		configuration = cfg;
	}

	public List<Event> getConfiguration() {
		return configuration;
	}

	public void setMark(Map<Place, Integer> mk) {
		mark = mk;
	}
	
	public Map<Place, Integer> getMark() {
		return mark;
	}
	
	public void setLexOrder(List<Transition> lo) {
		lexOrder = lo;
	}
	
	public List<Transition> getLexOrder() {
		return lexOrder;
	}
	
	public void addLockSet(String s) {
		lockSet.add(s);
	}
	
	public void removeLockSet(String s) {
		if (!lockSet.contains(s)) {
			System.err.println("[HDD-StaticAnalysis] have not acquired the lock ["+s+"]!");
			System.exit(1);
		}
		lockSet.remove(s);
	}
	
	public Set<String> getLockSet() {
		return lockSet;
	}
	
	public boolean isConcurrentWith(Event that) {
		for (Condition c1 : this.preSet) {
			for (Condition c2 : that.preSet) {
				if (!c1.isConcurrentWith(c2)) return false;
			}
		}
		return true;
	}
	
	public boolean hasSameLockWith(Event that) {
		for (String lock : this.lockSet) {
			if (that.lockSet.contains(lock)) return true;
		}
		return false;
	}
	
	public boolean isMarkEqualWith(Event that) {
		if (this.mark.size() != that.mark.size()) {
			return false;
		}
		for (Map.Entry<Place, Integer> entry : this.mark.entrySet()) {
			if (!that.mark.containsKey(entry.getKey())) {
				return false;
			}
			if (that.mark.get(entry.getKey()) != entry.getValue()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isAdeSmallerThan(Event that) {
		if (this.configuration.size() < that.configuration.size()) return true;
		else if (this.configuration.size() == that.configuration.size()) {
			for (int i = 0; i < this.lexOrder.size(); i++) {
				if (this.lexOrder.get(i).getId() < that.lexOrder.get(i).getId()) {
					return true;
				}
				else if (this.lexOrder.get(i).getId() > that.lexOrder.get(i).getId()) {
					return false;
				}
			}
			return false;
		}
		return false;
	}
	
	@Override
	public int compareTo(Event that) {
		// 1.adequate order
		if (this.configuration.size() != that.configuration.size()) {
			return this.configuration.size()-that.configuration.size();
		}
		for (int i = 0; i < this.lexOrder.size(); i++) {
			if (this.lexOrder.get(i).getId() < that.lexOrder.get(i).getId()) {
				return -1;
			}
			else if (this.lexOrder.get(i).getId() > that.lexOrder.get(i).getId()) {
				return 1;
			}
		}
		return 0;
	}
	
	@Override
	public String toString() {
		String s = "<t"+map.getId()+",{";
		for (int i = 0; i < preSet.size(); i++) {
			s += "c"+preSet.get(i).getId()+(i==preSet.size()-1?"}>":",");
		}
		return s;
	}
	
	public void outputConfiguration() {
		for (Event e : configuration) {
			System.out.print("e"+e.getId() + " ");
		}
		System.out.println();
	}
	
	public void outputMark() {
		for (Map.Entry<Place, Integer> entry : mark.entrySet()) {
			System.out.print("p"+entry.getKey().getId()+"*"+entry.getValue()+" ");
		}
		System.out.println();
	}
	
	public void outputLex() {
		for (Transition t : lexOrder) {
			System.out.print("t"+t.getId()+" ");
		}
		System.out.println();
	}
}
