
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class DynamicAnalysis {
	private Map<Integer, Set<String>> lockset;
	private Map<String, DynamicMonitor> vMonitor;
	
	public DynamicAnalysis(Integer mainThread) {
		lockset = new HashMap<> ();
		Set<String> set = new HashSet<String> ();
		lockset.put(mainThread, set);
		vMonitor = new HashMap<> ();
	}
	
	public void startBefore(Integer fThread, String message, Integer cThread) {
		System.out.println("[" + message + "]: Thread " + fThread + " start Thread " + cThread);
		Set<String> set = new HashSet<String> ();
		for (String lock : lockset.get(fThread)) {
			set.add(lock);
		}
		lockset.put(cThread, set);
	}
	public void lockBefore(Integer thread, String message, String lock) {
		System.out.println("[" + message + "]: Thread " + thread + " acquire " + lock);
		lockset.get(thread).add(lock);
	}
	public void unlockBefore(Integer thread, String message, String lock) {
		System.out.println("[" + message + "]: Thread " + thread + " release " + lock);
		lockset.get(thread).remove(lock);
	}
	public void writeBefore(Integer thread, String message, String v) {
		System.out.println("[" + message + "]: Thread " + thread + " write " + v);
		if (!vMonitor.containsKey(v)) {
			DynamicMonitor monitor = new DynamicMonitor(v);
			monitor.update_w(thread, message, lockset.get(thread));
			vMonitor.put(v, monitor);
		}
		else {
			DynamicMonitor monitor = vMonitor.get(v);
			monitor.detectDataRace_w(thread, message, lockset.get(thread));
			monitor.update_w(thread, message, lockset.get(thread));
		}
	}
	public void readBefore(Integer thread, String message, String v) {
		System.out.println("[" + message + "]: Thread " + thread + " read " + v);
		if (!vMonitor.containsKey(v)) {
			DynamicMonitor monitor = new DynamicMonitor(v);
			monitor.update_r(thread, message, lockset.get(thread));
			vMonitor.put(v, monitor);
		}
		else {
			DynamicMonitor monitor = vMonitor.get(v);
			monitor.detectDataRace_r(thread, message, lockset.get(thread));
			monitor.update_r(thread, message, lockset.get(thread));
		}
	}
}
