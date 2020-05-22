package staticanalysis.unfolding;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;

import staticanalysis.petrinet.*;
import staticanalysis.occurrentnet.*;

public class Unfolder {
	private Place Ps;
	private List<Condition> C;
	private List<Event> E;
	private Event Es;
	private PriorityQueue<Event> Ext;
	private int cutoffNumber;
	
	public Unfolder(Place ps) {
		Ps = ps;
		C = new ArrayList<> ();
		E = new ArrayList<> ();
		Ext = new PriorityQueue<> ();
		cutoffNumber = 0;
	}
	
	private void initialize() {
		Condition c = newCondition(Ps);
		Event e = newEvent(Ps.getPostSet().get(0));
		Es = e;
		e.addPreSet(c);
		List<Event> cfg = new ArrayList<> ();
		cfg.add(e);
		Map<Place, Integer> mark = new HashMap<> ();
		for (Place p : e.getMap().getPostSet()) {
			mark.put(p, 1);
		}	
		List<Transition> lex = new ArrayList<> ();
		lex.add(e.getMap());
		e.setConfiguration(cfg);
		e.setMark(mark);
		e.setLexOrder(lex);
		Ext.offer(e);
	}
	
	public void start(long maxTime) {
		initialize();
		long startTime = System.currentTimeMillis();
		while (!Ext.isEmpty()) {
			if (maxTime != 0 && System.currentTimeMillis()-startTime > maxTime) {
				break;
			}
			Event e = Ext.poll();
			if (isCutoff(e)) {
				/**************************************************
				System.out.println("[Running] e"+e.getId()+" is a cut-off event");
				**************************************************/
				cutoffNumber++;
				continue;
			}
			for (Condition c : e.getPreSet()) {
				c.addPostSet(e);
			}
			/**************************************************
			System.out.println("[Running] Add e"+e.getId()+"="+e.toString()+" to Unf");
			**************************************************/
			for (Place p : e.getMap().getPostSet()) {
				Condition c = newCondition(p);
				e.addPostSet(c);
				c.setPreSet(e);
				/**************************************************
				System.out.println("[Running] Add c" + c.getId() + " = p" + p.getId() + " to Unf");
				**************************************************/
			}
			for (Condition c : e.getPostSet()) {
				updateConcurrentSet(c);
				for (Transition t : c.getMap().getPostSet()) {					
					updateExtension(c, t);
				}
			}
		}
	}
	
	private void updateConcurrentSet(Condition c0) {
		Event e = c0.getPreSet();
		if (e != null) {
			List<Condition> preSet = e.getPreSet();
			Set<Condition> concurrentSet = new HashSet<> ();
			concurrentSet.addAll(preSet.get(0).getConcurrentSet());
			for (int i = 1; i < preSet.size(); i++) {
				concurrentSet.retainAll(preSet.get(i).getConcurrentSet());
			}
			for (Condition c : e.getPostSet()) {
				if (!c.equals(c0)) {
					concurrentSet.add(c);
				}
			}
			c0.setConcurrentSet(concurrentSet);
			for (Condition c : concurrentSet) {
				c.addConcurrentSet(c0);
			}
		}
	}
	
	private void updateExtension(Condition c, Transition t) {
		Map<Integer, Integer> map = new HashMap<> ();
		int count = 0;
		for (Place p : t.getPreSet()) {
			if (c.getMap().getId() == p.getId()) {
				continue;
			}
			map.put(p.getId(), count++);
		}
		Set<Condition> concurrentSet = c.getConcurrentSet();
		List<List<Condition>> list = new ArrayList<> ();
		for (int i = 0; i < count; i++) {
			list.add(new ArrayList<Condition> ());
		}
		for (Condition c1 : concurrentSet) {
			int id = c1.getMap().getId();
			if (!map.containsKey(id)) {
				continue;
			}
			list.get(map.get(id)).add(c1);
		}
		updateExtension_dfs(count-1, c, t, list, new ArrayList<Condition>());
	}

	private void updateExtension_dfs(int d, Condition c, Transition t, List<List<Condition>> list, List<Condition> res) {
		if (d < 0) {
			Event e = newEvent(t);
			e.addPreSet(c);
			for (Condition c1 : res) {
				e.addPreSet(c1);
			}
			updateEvent(e);
			Ext.offer(e);
			/**************************************************
			System.out.println("[Running] -- Add "+e.toString()+" to Ext");
			**************************************************/
			return;
		}
		for (Condition c1 : list.get(d)) {
			boolean ok = true;
			for (Condition c2 : res) {
				if (!c1.isConcurrentWith(c2)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				res.add(c1);
				updateExtension_dfs(d-1, c, t, list, res);
				res.remove(res.size() - 1);
			}
		}
	}
	
	private void updateEvent(Event e0) {
		Set<Event> unionSet = new HashSet<> ();
		unionSet.add(e0);
		for (Condition c : e0.getPreSet()) {
			Event e = c.getPreSet();
			if (e == null) continue;
			unionSet.addAll(e.getConfiguration());
		}
		List<Event> cfg = new ArrayList<> ();
		cfg.addAll(unionSet);
		Set<Condition> preSet = new HashSet<> ();
		Set<Condition> postSet = new HashSet<> ();
		for (Event e : cfg) {
			preSet.addAll(e.getPreSet());
			postSet.addAll(e.getPostSet());
		}
		postSet.removeAll(preSet);
		Map<Place, Integer> mark = new HashMap<> ();
		for (Condition c : postSet) {
			Place p = c.getMap();
			if (mark.containsKey(p)) {
				mark.put(p, mark.get(p)+1);
			}
			else {
				mark.put(p, 1);
			}
		}
		for (Place p : e0.getMap().getPostSet()) {
			if (mark.containsKey(p)) {
				mark.put(p, mark.get(p)+1);
			}
			else {
				mark.put(p, 1);
			}
		}
		List<Transition> lex = new ArrayList<> ();
		for (Event e : cfg) {
			lex.add(e.getMap());
		}
		Collections.sort(lex, new Comparator<Transition> () {
			@Override
			public int compare(Transition t1, Transition t2) {
				return t1.getId() - t2.getId();
			}
		});
		e0.setConfiguration(cfg);
		e0.setMark(mark);
		e0.setLexOrder(lex);
	}
	
	private boolean isCutoff(Event e0) {
		for (Event e : E) {
			if (e.equals(e0)) continue;
			if (e.isMarkEqualWith(e0) && e.isAdeSmallerThan(e0)) {
				return true;
			}
		}
		return false;
	}
	
	private Condition newCondition(Place place) {
		int id = C.size();
		Condition node = new Condition(place, id);
		C.add(node);
		return node;
	}
	
	private Event newEvent(Transition transition) {
		int id = E.size();
		Event node = new Event(transition, id);
		E.add(node);
		return node;
	}
	
	public List<Condition> getC() {
		return C;
	}
	
	public List<Event> getE() {
		return E;
	}
	
	public int getCutoffNumber() {
		return cutoffNumber;
	}
	
	public Event getEs() {
		return Es;
	}
	
}