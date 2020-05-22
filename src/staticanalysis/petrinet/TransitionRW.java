package staticanalysis.petrinet;

public class TransitionRW extends Transition {
	private String variable;
	private boolean isWrite;
	
	public TransitionRW(int id, String signature, String message, String variable, boolean isWrite) {
		super(id, signature, message);
		this.variable = variable;
		this.isWrite = isWrite;
	}
	
	public String getVariable() {
		return variable;
	}
	
	public boolean isWrite() {
		return isWrite;
	}
}
