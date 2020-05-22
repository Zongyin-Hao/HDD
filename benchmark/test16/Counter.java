import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Counter extends Thread {

	private int threadId;
	private String fileName;
	
	public static synchronized void add(String key) {
		if (!Main.counter.containsKey(key)) {
			Main.counter.put(key, 1);
		}
		else {
			int oldValue = Main.counter.get(key);
			int newValue = oldValue+1;
			Main.counter.put(key, newValue);
		}
	}
	
	public Counter(int threadId, String fileName) {
		this.threadId = threadId;
		this.fileName = fileName;
	}
	
	@Override
	public void run() {
		try {
			File file = new File(fileName+threadId); 
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = null;
			while ((s = br.readLine()) != null) {
				add(s);
				Main.sum += 1;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
