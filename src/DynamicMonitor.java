
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class DynamicMonitor {
	private String sharedVariable;
	private List<Integer> visitedThread_w;
	private List<Integer> visitedThread_r;
	private List<String> thread_w_message;
	private List<String> thread_r_message;
	private List<Set<String>> thread_w_lockset;
	private List<Set<String>> thread_r_lockset;
	private List<Set<Integer>> thread_w_concurrent;
	private List<Set<Integer>> thread_r_concurrent;
	
	public DynamicMonitor(String sharedVariable) {
		this.sharedVariable = sharedVariable;
		visitedThread_w = new ArrayList<> ();
		visitedThread_r = new ArrayList<> ();
		thread_w_message = new ArrayList<> ();
		thread_r_message = new ArrayList<> ();
		thread_w_lockset = new ArrayList<> ();
		thread_r_lockset = new ArrayList<> ();
		thread_w_concurrent = new ArrayList<> ();
		thread_r_concurrent = new ArrayList<> ();
	}
	
	public void detectDataRace_w(Integer thread, String message, Set<String> lockset) {
		for (int i = 0; i < visitedThread_w.size(); i++) {
			if (visitedThread_w.get(i) == thread) continue;
			boolean ok = false;
			for (String lock : thread_w_lockset.get(i)) {
				if (lockset.contains(lock)) {
					ok = true;
					break;
				}
			}
			if (ok || !thread_w_concurrent.get(i).contains(thread)) continue;
			System.out.println("****************************************");
			System.out.println("*Data race detected!");
			System.out.println("*["+thread_w_message.get(i)+"]: Thread " + visitedThread_w.get(i) + " write " + sharedVariable);
			System.out.println("*[" + message + "]: Thread " + thread + " write " + sharedVariable);
			System.out.println("****************************************");
			String A = thread_w_message.get(i);
			String B = message;
			if (A.compareTo(B) <= 0) {
				Controller.dataRacePair.add(A + " and " + B);
			}
			else {
				Controller.dataRacePair.add(B + " and " + A);
			}
		}
		for (int i = 0; i < visitedThread_r.size(); i++) {
			if (visitedThread_r.get(i) == thread) continue;
			boolean ok = false;
			for (String lock : thread_r_lockset.get(i)) {
				if (lockset.contains(lock)) {
					ok = true;
					break;
				}
			}
			if (ok || !thread_r_concurrent.get(i).contains(thread)) continue;
			System.out.println("****************************************");
			System.out.println("*Data race detected!");
			System.out.println("*["+thread_r_message.get(i)+"]: Thread " + visitedThread_r.get(i) + " read " + sharedVariable);
			System.out.println("*[" + message + "]: Thread " + thread + " write " + sharedVariable);
			System.out.println("****************************************");
			String A = thread_r_message.get(i);
			String B = message;
			if (A.compareTo(B) <= 0) {
				Controller.dataRacePair.add(A + " and " + B);
			}
			else {
				Controller.dataRacePair.add(B + " and " + A);
			}
		}
	}
	
	public void detectDataRace_r(Integer thread, String message, Set<String> lockset) {
		for (int i = 0; i < visitedThread_w.size(); i++) {
			if (visitedThread_w.get(i) == thread) continue;
			boolean ok = false;
			for (String lock : thread_w_lockset.get(i)) {
				if (lockset.contains(lock)) {
					ok = true;
					break;
				}
			}
			if (ok || !thread_w_concurrent.get(i).contains(thread)) continue;
			System.out.println("****************************************");
			System.out.println("*Data race detected!");
			System.out.println("*["+thread_w_message.get(i)+"]: Thread " + visitedThread_w.get(i) + " write " + sharedVariable);
			System.out.println("*[" + message + "]: Thread " + thread + " read " + sharedVariable);
			System.out.println("****************************************");
			String A = thread_w_message.get(i);
			String B = message;
			if (A.compareTo(B) <= 0) {
				Controller.dataRacePair.add(A + " and " + B);
			}
			else {
				Controller.dataRacePair.add(B + " and " + A);
			}
		}
	}
	
	public void update_w(Integer thread, String message, Set<String> lockset) {
		int id = -1;
		for (int i = 0; i < visitedThread_w.size(); i++) {
			if (visitedThread_w.get(i) == thread) {
				id = i;
				break;
			}
		}
		if (id == -1) {
			visitedThread_w.add(thread);
			thread_w_message.add(message);
			Set<String> newLockset = new HashSet<> ();
			newLockset.addAll(lockset);
			thread_w_lockset.add(newLockset);
			Set<Integer> newConcurrent = new HashSet<> ();
			for (Thread t : Controller.threadList) {
				newConcurrent.add(Controller.thread_to_id.get(t));
			}
			thread_w_concurrent.add(newConcurrent);
		}
		else {
			thread_w_message.set(id, message);
			Set<String> newLockset = new HashSet<> ();
			newLockset.addAll(lockset);
			thread_w_lockset.set(id, newLockset);
			Set<Integer> newConcurrent = new HashSet<> ();
			for (Thread t : Controller.threadList) {
				newConcurrent.add(Controller.thread_to_id.get(t));
			}
			thread_w_concurrent.set(id, newConcurrent);
		}
	}
	
	public void update_r(Integer thread, String message, Set<String> lockset) {
		int id = -1;
		for (int i = 0; i < visitedThread_r.size(); i++) {
			if (visitedThread_r.get(i) == thread) {
				id = i;
				break;
			}
		}
		if (id == -1) {
			visitedThread_r.add(thread);
			thread_r_message.add(message);
			Set<String> newLockset = new HashSet<> ();
			newLockset.addAll(lockset);
			thread_r_lockset.add(newLockset);
			Set<Integer> newConcurrent = new HashSet<> ();
			for (Thread t : Controller.threadList) {
				newConcurrent.add(Controller.thread_to_id.get(t));
			}
			thread_r_concurrent.add(newConcurrent);
		}
		else {
			thread_r_message.set(id, message);
			Set<String> newLockset = new HashSet<> ();
			newLockset.addAll(lockset);
			thread_r_lockset.set(id, newLockset);
			Set<Integer> newConcurrent = new HashSet<> ();
			for (Thread t : Controller.threadList) {
				newConcurrent.add(Controller.thread_to_id.get(t));
			}
			thread_r_concurrent.set(id, newConcurrent);
		}
	}
}
