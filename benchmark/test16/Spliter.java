
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Spliter extends Thread {
	
	private int threadId;
	private String fileName;
	private List<String> block;
	
	
	public Spliter(int threadId, String fileName, List<String> block) {
		this.threadId = threadId;
		this.fileName = fileName;
		this.block = block;
	}
	
	@Override
	public void run() {
		try {
			File file = new File(fileName+threadId);
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWritter = new FileWriter(file.getName(), false);
			for (int i = 0; i < block.size(); i++) {
				fileWritter.write(block.get(i)+"\n");
			}
			fileWritter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
}