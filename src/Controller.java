
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Controller {

	public static Object controllerLock = new Object();
	public static Map<Thread, String> thread_to_prefix = new HashMap<> ();
	public static Map<Thread, Integer> thread_to_id = new HashMap<> ();
	public static List<Thread> threadList = new ArrayList<Thread> ();
	public static Queue<String> schedule = new LinkedList<> ();
	public static Map<String, Integer> used = new HashMap<> ();
	public static Queue<Thread> blocked = new LinkedList<> ();
	public static DynamicAnalysis dynamicAnalysis;
	public static Set<String> dataRacePair; //A + " and " + B
	
	static {
		thread_to_prefix.put(Thread.currentThread(), "0");
		thread_to_id.put(Thread.currentThread(), 1);
		threadList.add(Thread.currentThread());
		try {
			File file = new File("errorList"); 
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = null;
//			System.out.println("<errorList>");
			while ((s = br.readLine()) != null) {
				schedule.offer(s);
				if (!used.containsKey(s)) used.put(s, 1);
				else used.put(s, used.get(s)+1);
//				System.out.println("<" + s + ">");
			}
//			System.out.println("");
			br.close();
			
			//清空一下，防止线程异常退出时文件里有脏数据
	        FileWriter fileWritter = new FileWriter(file.getName(), false);
	        fileWritter.write("\n");
	    	fileWritter.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.err.println("[HDD-DynamicAnalysis] can not load errorList!");
			System.exit(1);
		}
		dynamicAnalysis = new DynamicAnalysis(1);
		dataRacePair = new HashSet<> ();
		(new StallBreaker()).start();
	}
	
	public static String getiid(String s) {
		int pos = s.length()-1;
		while (pos >= 0) {
			if ('0' <= s.charAt(pos) && s.charAt(pos) <= '9') {
				pos--;
				continue;
			}
			if (s.charAt(pos) == ':') break;
			System.err.println("[HDD-DynamicAnalysis] can not get the iid of <" + s + ">!");
			System.exit(1);
		}
		if (pos < 0) {
			System.err.println("[HDD-DynamicAnalysis] can not get the iid of <" + s + ">!");
			System.exit(1);
		}
		return s.substring(pos+1);
	}
	
	public static void startBefore(String message, String signatureV, Object thread) {
		synchronized (controllerLock) {
			Thread fThread = Thread.currentThread();
			Thread cThread = (Thread)thread;
			
			String fThreadPrefix = thread_to_prefix.get(fThread);
			String signature = fThreadPrefix + ":" + signatureV;
			String cThreadPrefix = fThreadPrefix + "." + getiid(signatureV);
			
			thread_to_prefix.put(cThread, cThreadPrefix);
			thread_to_id.put(cThread, thread_to_id.size()+1);
			threadList.add(cThread);
			
			if (used.containsKey(signature) && used.get(signature) > 0) {
				while (!schedule.peek().equals(signature)) {
					try {
						controllerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("block " + fThread + " " + message);
//					blocked.offer(fThread);
//					LockSupport.park(fThread);
				}
//				System.out.println("<"+signature+">");
				dynamicAnalysis.startBefore(thread_to_id.get(fThread), message, thread_to_id.get(cThread));
				schedule.poll();
				used.put(signature, used.get(signature)-1);
				controllerLock.notifyAll();
//				while (!blocked.isEmpty()) {
//					Thread t = blocked.poll();
//					LockSupport.unpark(t);
//				}
			}
			else {
				dynamicAnalysis.startBefore(thread_to_id.get(fThread), message, thread_to_id.get(cThread));
			}
		}
	}
	
	public static void lockBefore(String message, String signatureV, String lock) {
		synchronized (controllerLock) {
			Thread thread = Thread.currentThread();
			
			String threadPrefix = thread_to_prefix.get(thread);
			String signature = threadPrefix + ":" + signatureV;
			
			if (used.containsKey(signature) && used.get(signature) > 0) {
				while (!schedule.peek().equals(signature)) {
					try {
						controllerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("block " + thread + " " + message);
//					blocked.offer(thread);
//					LockSupport.park(thread);
				}
//				System.out.println("<"+signature+">");
				dynamicAnalysis.lockBefore(thread_to_id.get(thread), message, lock);
				schedule.poll();
				used.put(signature, used.get(signature)-1);
				controllerLock.notifyAll();
//				while (!blocked.isEmpty()) {
//					Thread t = blocked.poll();
//					LockSupport.unpark(t);
//				}
			}
			else {
				dynamicAnalysis.lockBefore(thread_to_id.get(thread), message, lock);
			}
		}
	}
	
	public static void unlockBefore(String message, String signatureV, String lock) {
		synchronized (controllerLock) {
			Thread thread = Thread.currentThread();
			
			String threadPrefix = thread_to_prefix.get(thread);
			String signature = threadPrefix + ":" + signatureV;
			
			if (used.containsKey(signature) && used.get(signature) > 0) {
				while (!schedule.peek().equals(signature)) {
					try {
						controllerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("block " + thread + " " + message);
//					blocked.offer(thread);
//					LockSupport.park(thread);
				}
//				System.out.println("<"+signature+">");
				dynamicAnalysis.unlockBefore(thread_to_id.get(thread), message, lock);
				schedule.poll();
				used.put(signature, used.get(signature)-1);
				controllerLock.notifyAll();
//				while (!blocked.isEmpty()) {
//					Thread t = blocked.poll();
//					LockSupport.unpark(t);
//				}
			}
			else {
				dynamicAnalysis.unlockBefore(thread_to_id.get(thread), message, lock);
			}
		}
	}
	
	public static void writeBefore(String message, String signatureV, String v) {
		synchronized (controllerLock) {
			Thread thread = Thread.currentThread();
			
			String threadPrefix = thread_to_prefix.get(thread);
			String signature = threadPrefix + ":" + signatureV;
			
			if (used.containsKey(signature) && used.get(signature) > 0) {
				while (!schedule.peek().equals(signature)) {
					try {
						controllerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("block " + thread + " " + message);
//					blocked.offer(thread);
//					LockSupport.park(thread);
				}
//				System.out.println("<"+signature+">");
				dynamicAnalysis.writeBefore(thread_to_id.get(thread), message, v);
				schedule.poll();
				used.put(signature, used.get(signature)-1);
				controllerLock.notifyAll();
//				while (!blocked.isEmpty()) {
//					Thread t = blocked.poll();
//					LockSupport.unpark(t);
//				}
			}
			else {
				dynamicAnalysis.writeBefore(thread_to_id.get(thread), message, v);
			}
		}
	}
	
	public static void readBefore(String message, String signatureV, String v) {
		synchronized (controllerLock) {
			Thread thread = Thread.currentThread();
			
			String threadPrefix = thread_to_prefix.get(thread);
			String signature = threadPrefix + ":" + signatureV;
			
			if (used.containsKey(signature) && used.get(signature) > 0) {
				while (!schedule.peek().equals(signature)) {
					try {
						controllerLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
//					System.out.println("block " + thread + " " + message);
//					blocked.offer(thread);
//					LockSupport.park();
				}
//				System.out.println("<"+signature+">");
				dynamicAnalysis.readBefore(thread_to_id.get(thread), message, v);
				schedule.poll();
				used.put(signature, used.get(signature)-1);
				controllerLock.notifyAll();
//				while (!blocked.isEmpty()) {
//					Thread t = blocked.poll();
//					LockSupport.unpark(t);
//				}
			}
			else {
				dynamicAnalysis.readBefore(thread_to_id.get(thread), message, v);
			}
		}
	}
	
}
