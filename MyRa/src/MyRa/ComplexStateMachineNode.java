package MyRa;

import MyRa.data.ComplexStateMachine;
import MyRa.data.Configuration;
import MyRa.data.StateMachineAdapter;
import utils.structures.Pair;

public class ComplexStateMachineNode<K,S,E> extends Node<S,Pair<K,E>> {
	
	public ComplexStateMachineNode(Configuration conf, ComplexStateMachine<K,S,E> stateMachine) {
		// StateMachine<S,Pair<K,E>> adapter = new StateMachineAdapter<K,S,E>(stateMachine);
		// super(conf, adapter);
		super(conf, new StateMachineAdapter<K,S,E>(stateMachine));
		
	}
}
