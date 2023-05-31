package MyRa.data;

import utils.structures.Pair;

public class StateMachineAdapter<K,S,E> implements StateMachine<S,Pair<K,E>> {
	private ComplexStateMachine<K,S,E> stateMachine;
	
	public StateMachineAdapter(ComplexStateMachine<K,S,E> stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	/**
	 * Makes a transition from a one state to other state of the state machine
	 * based on the entry given.
	 * 
	 * @param key Key associated to the state machine.
	 * @param complexEntry Pair of the current State and the Entry for the state machine
	 * 
	 * @return State to which it transitions
	 * 
	 */
	public S next(Pair<K,E> complexEntry) {
		if(complexEntry == null) return null;

		S res = stateMachine.next(complexEntry.getFirst(), complexEntry.getSecond());
		System.out.println(res);
		return res;
	}

		
	/**
	 * Returns the current state of the state machine.
	 * 
	 * @param key Key associated to the state machine.
	 * 
	 * @return State of the state machine
	 * 
	 */
	//public S getState(K key);
	
	
	/**
	 * Forces the state machine to set a specific state. 
	 * (NOT recommended to use in normal operation, only for testing)
	 * 
	 * @param key Key associated to the state machine. 
	 * @param state Desired state in the state machine.
	 * 
	 */
	//public void setState(K key, S state);
	
	
	/**
	 * Makes a snapshot of the state machine, so that it can be replicated.
	 * 
	 * @return A snapshoot of the state machine.
	 * 
	 */
	public Snapshot<S> saveSnapshoot(){
		return stateMachine.saveSnapshoot();
	}
	
	
	/**
	 * Load a snapshoot to the state machine (can restore an state machine or make a replica of a state machine)
	 * 
	 * @param snapshoot Snapshoot of the state machine that is going to be loaded.
	 *
	 * @return If the snapshoot is not valid (incomplete, inconsistent data, data not valid for this state machine)
	 * it return false. If the snapshoot is valid and could be correctly loaded to the state machine, it return true.  
	 * 
	 */
	public boolean loadSnapshoot(Snapshot<S> snapshoot) {
		return stateMachine.loadSnapshoot(snapshoot);
	}
	
	public void advertise() {
		stateMachine.advertise();
	}
}
