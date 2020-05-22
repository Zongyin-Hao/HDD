
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Main {
	
	public static int sum = 0;
	public static Map<String, Integer> counter = new HashMap<> ();
	
	public static void initialize() {
		try {
			File file = new File("tmp");
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fileWritter = new FileWriter(file.getName(), false);
			fileWritter.write("dsafsasdfa"+"\n");
			fileWritter.write("sfdsdafas"+"\n");
			fileWritter.write("nreah"+"\n");
			fileWritter.write("trucvsfhreyf"+"\n");
			fileWritter.write("weefggjtr"+"\n");
			fileWritter.write("dsafsasdfa"+"\n");
			fileWritter.write("weefggjtr"+"\n");
			fileWritter.write("a"+"\n");
			fileWritter.write("dsafsasdfa"+"\n");
			fileWritter.write("vewyyidfgtertewa"+"\n");
			fileWritter.write("weeww"+"\n");
			fileWritter.write("a"+"\n");
			fileWritter.write("ytiilhj"+"\n");
			fileWritter.write("weefggjtr"+"\n");
			fileWritter.write("pojlkemvpofjoptjgw"+"\n");
			fileWritter.write("efpwoejf"+"\n");
		    fileWritter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		initialize();
		
		String fileName = "tmp";
		int threadNumber = 3;
		List<String> context = new ArrayList<String> ();
		try {
			File file = new File(fileName); 
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = null;
			while ((s = br.readLine()) != null) {
				context.add(s);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int blockSize = context.size() / threadNumber;
		int id = 0;
		List<String> block1 = new ArrayList<> ();
		for (int i = 1; i <= blockSize; i++) {
			block1.add(context.get(id));
			id++;
		}
		List<String> block2 = new ArrayList<> ();
		for (int i = 1; i <= blockSize; i++) {
			block2.add(context.get(id));
			id++;
		}
		List<String> block3 = new ArrayList<> ();
		for (; id < context.size(); id++) {
			block3.add(context.get(id));
		}
		Spliter split1 = new Spliter(1, fileName, block1);
		Spliter split2 = new Spliter(2, fileName, block2);
		Spliter split3 = new Spliter(3, fileName, block3);
		
		split1.start();
		split2.start();
		split3.start();
		
		try {
			split1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			split2.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			split3.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Counter counter1 = new Counter(1, fileName);
		Counter counter2 = new Counter(2, fileName);
		Counter counter3 = new Counter(3, fileName);
		
		counter1.start();
		counter2.start();
		counter3.start();
		
		try {
			counter1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			counter2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			counter3.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("total number = " + sum);
		try {
			File ans = new File("ans");
			if(!ans.exists()){
				ans.createNewFile();
			}
			FileWriter fileWritter = new FileWriter(ans.getName(), false);
			for (Map.Entry<String, Integer> entry : counter.entrySet()) {
				System.out.println(entry.getKey() + " " + entry.getValue());
				fileWritter.write(entry.getKey() + " " + entry.getValue());
			}
			fileWritter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
