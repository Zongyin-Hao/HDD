package staticanalysis.petrinet;

public class TransitionSyn extends Transition {
	private String lock;
	private boolean isAcquire;
	
	public TransitionSyn(int id, String signature, String message, String lock, boolean isAcquire) {
		super(id, signature, message);
		this.lock = lock;
		this.isAcquire = isAcquire;
	}
	
	public String getLock() {
		return lock;
	}
	
	public boolean isAcquire() {
		return isAcquire;
	}
}
