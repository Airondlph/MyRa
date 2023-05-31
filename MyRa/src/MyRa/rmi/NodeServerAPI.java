package MyRa.rmi;

import utils.structures.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;

import MyRa.data.LogEntry;
import MyRa.data.ServerID;

/**
 * Basic server for a node (active agent) of MyRA.
 * 
 * @author de la Parra Hern�ndez, Adri�n (Airondlph)
 *
 * @param <S> State type that the state machine, used with MyRA, can have.
 * @param <E> Entry type that the state machine, used with MyRA, accepts.
 * 
 */
public interface NodeServerAPI<S,E> extends Remote {
	/**
	 * USED BY LEADERS. The leader sends a heartbeat to the follower.
	 * 
	 * @param term Current term of the leader
	 * @param prevLogEntry Last log entry of the leader (indexEntry and termEntry)
	 * @param leaderID Identification of the current leader so the follower can redirect other nodes to it.
	 * @param commitIndex Last commit index.
	 * 
	 * @return Two values:
	 * 		First: (Boolean value) If it is true, the entry has been appended; if it is false, there is a problem 
	 * 				(the leader is not anymore the leader, or the node that receive the request does not have the previous entry) 
	 * 
	 * @throws RemoteException
	 * 
	 */
	public Pair<Boolean,Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm) throws RemoteException;
	
	/**
	 * USED BY LEADERS. The leader sends a heartbeat to the follower.
	 * 
	 * @param term Current term of the leader
	 * @param prevLogEntry Last log entry of the leader (indexEntry and termEntry)
	 * @param leaderID Identification of the current leader so the follower can redirect other nodes to it.
	 * @param commitIndex Last commit index.
	 * 
	 * @return Two values:
	 * 		First: (Boolean value) If it is true, the entry has been appended; if it is false, there is a problem 
	 * 				(the leader is not anymore the leader, or the node that receive the request does not have the previous entry) 
	 * 
	 * @throws RemoteException
	 * 
	 */
	public Pair<Boolean, Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm, long commitIndex) throws RemoteException;
	
	
	/**
	 * USED BY LEADERS. The leader sends to a the node an append entry request. If the node meets the requirements, it must append the received log entry to its log.
	 * 
	 * @param term Current term of the leader
	 * @param prevLogEntryIndex Last log entry index.
	 * @param prevLogEntryTerm Last log entry term.
	 * @param leaderID Identification of the current leader so the follower can redirect other nodes to it.
	 * @param commitIndex Last commit index.
	 * @param logEntry Entry to append.
	 * 
	 * @return Two values:
	 * 		First: (Boolean value) If it is true, the entry has been appended; if it is false, there is a problem 
	 * 				(the leader is not anymore the leader, or the node that receive the request does not have the previous entry) 
	 * 
	 * 		Second: (Long value) current term for leader to update itself.
	 * 
	 * @throws RemoteException
	 * 
	 */
	public Pair<Boolean,Long> appendEntry(ServerID leaderID, long term, long prevLogEntryIndex, long prevLogEntryTerm, LogEntry<E> logEntry, long commitIndex) throws RemoteException;
	
	
	public Pair<Boolean,ServerID> appendEntry(ServerID requestNode, E entry) throws RemoteException;
	
	
	/**
	 * USED BY CANDIDATES
	 * @param term
	 * @param logEntry
	 * @return Two values:
	 * 		First: (Boolean value) If it is true, the receiver grants me the vote; if it is false, the receiver does not grant me the vote. 
	 * 
	 * 		Second: (Long value) current term for leader to update itself.
	 * @throws RemoteException
	 */
	public Pair<Boolean,Long> requestVote(ServerID candidateID, long term, long lastLogIndex, long lastLogTerm) throws RemoteException;
	
	
	/**
	 * Important change in the state machine has been made (you should check it).
	 */
	public void advertise() throws RemoteException;
}
