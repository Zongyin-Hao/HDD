package staticanalysis.petrinet;

public class TransitionStart extends Transition {
	String thread;
	String threadType;
	
	public TransitionStart(int id, String signature, String message, String thread, String threadType) {
		super(id, signature, message);
		this.thread = thread;
		this.threadType = threadType;
	}
	
	public String getThread() {
		return thread;
	}
	
	public String getThreadType() {
		return threadType;
	}
}
