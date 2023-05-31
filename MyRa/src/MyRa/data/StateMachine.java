package MyRa.data;


/**
 * Machine State interface used by MyRA.
 * 
 * @author de la Parra Hernández, Adrián (Airondlph)
 *
 * @param <S> States of the state machine
 * @param <E> Entries accepted by the state machine
 * 
 */
public interface StateMachine<S, E> {
	/**
	 * Makes a transition from a one state to other state of the state machine
	 * based on the entry given.
	 * 
	 * @param entry Entry to state machine
	 * 
	 * @return State to which it transitions
	 */
	public S next(E entry);
	
	
	/**
	 * Returns the current state of the state machine.
	 * 
	 * @return State of the state machine
	 * 
	 * 
	 * NOT IMPLEMENTED: If you want to know the state, implement an entry like Entry.INFO that tell you the current state when you make next(Entry.INFO)
	 */
	// public S getState();
	
	
	/**
	 * Forces the state machine to set a specific state. 
	 * (NOT recommended to use in normal operation, only for testing)
	 * 
	 * @param state Desired state in the state machine.
	 */
	// public void setState(S state);
	
	
	/**
	 * Makes a snapshot of the state machine, so that it can be replicated.
	 * 
	 * @return A snapshoot of the state machine.
	 */
	public Snapshot<S> saveSnapshoot();
	
	
	/**
	 * Load a snapshoot to the state machine (can restore an state machine or make a replica of a state machine)
	 * @param snapshoot Snapshoot of the state machine that is going to be loaded.
	 * @return If the snapshoot is not valid (incomplete, inconsistent data, data not valid for this state machine)
	 * it return false. If the snapshoot is valid and could be correctly loaded to the state machine, it return true.  
	 */
	public boolean loadSnapshoot(Snapshot<S> snapshoot);
	
	/**
	 * 
	 */
	public void advertise();
}
