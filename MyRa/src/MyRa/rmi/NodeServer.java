package MyRa.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import MyRa.Node;
import MyRa.Node.ROLE;
import MyRa.data.LogEntry;
import MyRa.data.ServerID;
import MyRa.thread.LeaderAppendEntryThread;
import utils.structures.Pair;


public class NodeServer<S,E> extends UnicastRemoteObject implements NodeServerAPI<S,E> {
	private static final long serialVersionUID = 1L;
	Node<S,E> node;
	
	private int i = 0;
	
	public NodeServer(Node<S,E> node) throws RemoteException {
		this.node = node;
	}

	@Override
	public Pair<Boolean, Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm)
			throws RemoteException {
		
		// Redirect to parent. This class is only an intermediary.
		return node.heartbeat(leaderID, term, lastEntryIndex, lastEntryTerm);	// in future could create a new process with the parent Node as an argument (parent must implement concurrence).
	}
	
	@Override
	public Pair<Boolean, Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm, long commitIndex) throws RemoteException {
		return node.heartbeat(leaderID, term, lastEntryIndex, lastEntryTerm, commitIndex);
	}

	@Override
	public Pair<Boolean, Long> appendEntry(ServerID leaderID, long term, long prevLogEntryIndex, long prevLogEntryTerm, LogEntry<E> logEntry, long commitIndex) throws RemoteException {
		return node.appendEntry(leaderID, term, prevLogEntryIndex, prevLogEntryTerm, logEntry, commitIndex);
	}
	
	@Override
	public Pair<Boolean, ServerID> appendEntry(ServerID requestNode, E entry) throws RemoteException {
		return node.appendEntry(requestNode, entry);
	}

	@Override
	public Pair<Boolean, Long> requestVote(ServerID candidateID, long term, long lastLogIndex, long lastLogTerm) throws RemoteException {
		return node.requestVote(candidateID, term, lastLogIndex, lastLogTerm);
	}
	
	
	@Override
	public void advertise() throws RemoteException {		
		node.advertise();
	}
}
