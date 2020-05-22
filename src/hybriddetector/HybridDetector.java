package hybriddetector;

import java.util.Map;
import java.util.List;

import soot.SceneTransformer;
import soot.SootClass;
import instrumentor.Instrumentor;

import staticanalysis.StaticAnalysis;

public class HybridDetector extends SceneTransformer {
	private SootClass mainClass;
	private SootClass callbackClass;
	private List<SootClass> testClasses;
	private List<List<String>> schedule;
	
	public HybridDetector(SootClass mainClass, SootClass callbackClass, List<SootClass> testClasses) {
		this.mainClass = mainClass;
		this.callbackClass = callbackClass;
		this.testClasses = testClasses;
	}
	
	protected void internalTransform(String phase, Map options) {
		StaticAnalysis staticAnalysis = new StaticAnalysis(mainClass, testClasses);
		staticAnalysis.start();
		schedule = staticAnalysis.getDataRacePath();
		
		Instrumentor instrumentor = new Instrumentor(mainClass, callbackClass, testClasses);
		instrumentor.start();
	}
	
	public List<List<String>> getSchedule() {
		return schedule;
	}
}
