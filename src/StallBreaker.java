import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

public class StallBreaker extends Thread {
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
//			System.out.println("==========stallBreaker check==========");
			int aliveNumber = 0;
			int size = Controller.threadList.size();
			for (int i = 0; i < size; i++) {
				Thread t = Controller.threadList.get(i);
				if (t == null) continue;
//				System.out.println(t + " " + t.getState());
				if (t.getState() == Thread.State.NEW
						|| t.getState() == Thread.State.RUNNABLE
						|| t.getState() == Thread.State.TIMED_WAITING) {
					aliveNumber++;
				}
			}
			if (aliveNumber == 0) {
				if (!Controller.schedule.isEmpty()) {
					String s = Controller.schedule.poll();
					Controller.used.put(s, Controller.used.get(s)-1);
					System.out.println("[HDD-DynamicAnalysis] Warning: can not schedule <" + s + ">");
					synchronized (Controller.controllerLock) {						
						Controller.controllerLock.notifyAll();
					}
//					while (!Controller.blocked.isEmpty()) {
//						Thread t = Controller.blocked.poll();
//						LockSupport.unpark(t);
//					}
				}
				else {
					break;
				}
			}	
		}
		try {
			File file = new File("errorList"); 
	        if(!file.exists()){
	        	file.createNewFile();
	        }
	        FileWriter fileWritter = new FileWriter(file.getName(), false);
			for (String dataRace : Controller.dataRacePair) {
				fileWritter.write(dataRace + "\n");
			}
	    	fileWritter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("[HDD-DynamicAnalysis] Can not process the result!");
			System.exit(1);
		}
	}
}
