package hybriddetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;


import java.util.ArrayList;
import java.util.Arrays;

import soot.options.Options;
import soot.PackManager;
import soot.Transform;
import soot.jimple.JasminClass;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;
import soot.util.JasminOutputStream;

public class Main {
	
	private SootClass mainClass;
	private SootClass callbackClass;
	private List<SootClass> testClasses;
	private Set<String> dataRace;
	
	private SootClass loadClass(String name, boolean main) {
		SootClass c = Scene.v().loadClassAndSupport(name);
		c.setApplicationClass();
		if (main) Scene.v().setMainClass(c);
		return c;
	}
	
	private void initialize(String mainPath, String callbackPath, String[] auxiliaryPaths, List<String> otherPaths) {
		soot.G.reset();
		String JAVA_HOME = System.getProperty("java.home");
		String rt_jar = JAVA_HOME+"/lib/rt.jar";
		String jce_jar = JAVA_HOME+"/lib/jce.jar";
		String sootClassPath = "sootInput"+File.pathSeparator+rt_jar+File.pathSeparator+jce_jar;
		Scene.v().setSootClassPath(sootClassPath);
		Options.v().set_prepend_classpath(true);
		Options.v().set_allow_phantom_refs(true);
		
		Options.v().set_src_prec(Options.src_prec_class);
		Options.v().set_whole_program(true);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_keep_line_number(true);
		Options.v().set_output_format(Options.output_format_jimple);
		
        Options.v().setPhaseOption("cg.spark", "on");
        
        testClasses = new ArrayList<SootClass> ();
        mainClass = loadClass(mainPath, true);
        testClasses.add(mainClass);
        callbackClass = loadClass(callbackPath, false);
        for (int i = 0; i < auxiliaryPaths.length; i++) {
        	loadClass(auxiliaryPaths[i], false);
        }
        for (String s : otherPaths) {
        	testClasses.add(loadClass(s, false));
        }
        soot.Scene.v().loadNecessaryClasses();
	}
	
	private void start(String mainPath, String callbackPath, String[] auxiliaryPaths, List<String> otherPaths) throws FileNotFoundException {
		long startTime = System.currentTimeMillis();
		System.out.println("[HDD] Initialize");
		initialize(mainPath, callbackPath, auxiliaryPaths, otherPaths);
		HybridDetector hybridAnalysis = new HybridDetector(mainClass, callbackClass, testClasses);
		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", hybridAnalysis));
		
		PackManager.v().runPacks();
		
		Iterator<SootClass> classIterator = Scene.v().getApplicationClasses().iterator();
		while (classIterator.hasNext()) {
			SootClass curClass = classIterator.next();
			String fileName = SourceLocator.v().getFileNameFor(curClass, Options.output_format_class);
			OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
			PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
			JasminClass jasminClass = new soot.jimple.JasminClass(curClass);
			jasminClass.print(writerOut);
			writerOut.flush();
			writerOut.close();
		}
		
		dataRace = new HashSet<> ();
		List<List<String>> schedule = hybridAnalysis.getSchedule();
		if (schedule.size() == 0) {
			System.out.println("[HDD] No data race");
		}
		else {
			System.out.println("[HDD] There are " + schedule.size() + " potential data race");
			try {
				File file = new File("errorList"); 
		        if(!file.exists()){
		        	file.createNewFile();
		        }
		        String [] command = new String [] {
						"./run.sh",
						mainPath
				};
				ProcessBuilder processBuilder = new ProcessBuilder();
				processBuilder.command(Arrays.asList(command));
				processBuilder.redirectErrorStream(true);
		        
		        for (int i = 0; i < schedule.size(); i++) {
		        	System.out.println("========================================");
		        	System.out.println("[HDD-DynamicAnalysis] Round " + (i+1));
		        	FileWriter fileWritter = new FileWriter(file.getName(), false);
		        	List<String> list = schedule.get(i);
		        	for (String s : list) {
		        		 fileWritter.write(s + "\n");
		        	}
		        	fileWritter.close();
		        	
		        	Process process = processBuilder.start();
					InputStream inputStream = process.getInputStream();
					InputStreamReader reader = new InputStreamReader(inputStream,"gbk");
					char[] chars = new char[1024];
			        int len = -1;
			        while((len = reader.read(chars)) != -1){
			            String s = new String(chars, 0, len);
			            System.out.print(s);
			        }
			        inputStream.close();
			        reader.close();
			        
			        BufferedReader br = new BufferedReader(new FileReader(file));
					String s = null;
					while ((s = br.readLine()) != null) {
						dataRace.add(s);
					}
					br.close();
		        }
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("[HDD] can not load DynamicAnalysis!");
				System.exit(1);
			}
		}
		
		System.out.println("[HDD] There are " + dataRace.size() + " data race");
		for (String result : dataRace) {
			System.out.println("[HDD] Between " + result);
		}
		System.out.println("[HDD] time = " + (System.currentTimeMillis()-startTime));
		System.out.println("[HDD] Finished");
	}
	
	private static void deleteFiles(String dir) {
		File file = new File(dir);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].exists()) {
				files[i].delete();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		deleteFiles("sootInput");
		deleteFiles("sootOutput");
		File errorList = new File("errorList");
		errorList.delete();
		Files.copy((new File("bin/Controller.class")).toPath(), 
				(new File("sootInput/Controller.class")).toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy((new File("bin/StallBreaker.class")).toPath(), 
				(new File("sootInput/StallBreaker.class")).toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy((new File("bin/DynamicAnalysis.class")).toPath(), 
				(new File("sootInput/DynamicAnalysis.class")).toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.copy((new File("bin/DynamicMonitor.class")).toPath(), 
				(new File("sootInput/DynamicMonitor.class")).toPath(), StandardCopyOption.REPLACE_EXISTING);
		
		
		String callbackPath = "Controller";
		String [] auxiliaryPaths = new String [] {
			"StallBreaker",
			"DynamicAnalysis",
			"DynamicMonitor"
		};
		
		String classPath = "benchmark/test1";
		String mainPath = "Main";
		
		boolean mainExist = false;
		List<String> otherPaths = new ArrayList<> ();
		File file = new File(classPath);
		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			if (name.length() > 6 && name.substring(name.length()-6).equals(".class")) {
				Files.copy(files[i].toPath(), 
						(new File("sootInput/"+name)).toPath(), StandardCopyOption.REPLACE_EXISTING);
				String prefix = name.substring(0, name.length()-6);
				if (prefix.equals(mainPath)) {
					mainExist = true;
				}
				else {
					otherPaths.add(prefix);
				}
			}
		}
		if (!mainExist) {
			System.err.println("[HDD] Main class not found!");
			System.exit(1);
		}
		
		// entrance
		(new Main()).start(mainPath, callbackPath, auxiliaryPaths, otherPaths);
	}
	
}
