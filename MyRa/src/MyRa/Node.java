package MyRa;

import utils.generator.Random;
import utils.structures.Pair;

import java.util.Map;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import MyRa.data.Clonable;
import MyRa.data.Configuration;
import MyRa.data.Data;
import MyRa.data.Log;
import MyRa.data.LogEntry;
import MyRa.data.ServerID;
import MyRa.data.StateMachine;
import MyRa.rmi.NodeServer;
import MyRa.task.LeaderElectionTask;
import MyRa.task.SendHeartbeatTask;
import MyRa.thread.LeaderAppendEntryThread;
import MyRa.thread.SendToLeaderAppendEntry;

// LOG //

/**
 * Agent actively participating in the consensus algorithm. 
 * A Node can be a Leader, a Follower or a Candidate (+ or a Offline node). 
 * 
 * @author de la Parra Hern�ndez, Adri�n (Airondlph)
 *
 * @param <S> State type accepted by the state machine 
 * 				(must be clonable to ensure that the program does not modify the entry if it is already in the log).
 * @param <E> Entry type accepted by the state machine 
 * 				(must be clonable to ensure that the program does not modify the entry if it is already in the log). 
 * 
 */

// LOG //
public class Node<S,E> extends Agent {	
	/**
	 * Types of role that a node (active node) can has.
	 * 
	 */
	public enum ROLE {LEADER, FOLLOWER, CANDIDATE, OFFLINE};
	
	private Semaphore termSem;
	
	private Configuration conf;
	
	NodeServer<S,E> server;
	long port;
	
	private Semaphore myRoleSem;
	private ROLE myRole;
	private Data<Long> term;	// Data to share with threads
	
	private Semaphore voteForSem;
	private ServerID voteFor;
	
	private Semaphore leaderIDSem;
	private ServerID leaderID;
	
	private Log<E> log;
	private Clonable<S> state;
	
	private StateMachine<S,E> stateMachine;
	private long lastApplied;
	
	// Only used by leaders
	private Map<ServerID,Long> nextIndex;
	private Map<ServerID,Long> matchIndex;
	
	Timer heartbeatTimer, sendHeartbeatTimer, leaderElectionTimer;
	
	SendToLeaderAppendEntry<S,E> appendEntriesThread;
	
	/**
	 * Initializes the node (active agent)
	 * 
	 * @param id Server id of the agent (must be unique)
	 * @param ip IPv4 of the device
	 * @param port Port number that will be used by the node (must be unique for the device)
	 * 
	 */
	public Node(Configuration conf, StateMachine<S,E> stateMachine) {	
		super(conf.serverID, conf.socket.getAddr(), conf.socket.getPort());
		
		this.conf = conf;
		
		this.log = new Log<>();
		
		termSem = new Semaphore(1);
		term = new Data<Long>((long)0);
		
		voteForSem = new Semaphore(1);
		voteFor = null;
		
		myRoleSem = new Semaphore(1);
		off();
		
		leaderIDSem = new Semaphore(1);
		
		this.stateMachine = stateMachine;
		
		appendEntriesThread = new SendToLeaderAppendEntry<S,E>(this);
		
		// loadPersistentData();
		
		initServer();
		
		System.out.println("Listo");
	}
	
	public S prueba(E entry) {
		return stateMachine.next(entry);
	}
	
	public void initServer() {
		String host = this.conf.socket.getAddr().toString();
		String resource = "MyRA/" + this.conf.serverID;
		int port = this.conf.socket.getPort();
		
		Registry registry;
		try {
			server = new NodeServer<>(this); // punto de entrada (recibe peticiones)
			registry = LocateRegistry.createRegistry(port);
			registry.rebind(resource, server);
			System.out.println("Server ready");
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Server error");
		}
	}
	
	public long appendEntry(long term, E entry) {
		return log.append(term, entry);
	}
	
	public void commit(long commit) {
		if(commit <= log.getLastCommittedIndex()) return;
		
		this.log.commit(commit); // Log ya protege el acceso concurrente en commit()
		stateMachine.next(log.get(commit).getEntry());
		advertise();
	}
	
	public long getLastCommit() {
		return this.log.getLastCommittedIndex();
	}
	
	public boolean setTerm(long term) {
		// if(getTerm() > term) return false;
		
		try {
			termSem.acquire();
			this.term.setData(term);
			clearVote();
			termSem.release();
			
		} catch (InterruptedException e) {
			restart();
			return false;
		}
		
		return true;
	}
	
	public ServerID getLeaderID() {
		return this.leaderID;
	}
	
	public boolean setLeaderID(ServerID leaderID) {
		try {
			leaderIDSem.acquire();
			this.leaderID = leaderID;
			leaderIDSem.release();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
		
	}
	
	public void advertise() {
		stateMachine.advertise();
	}
	
	public boolean clearVote() {
		try {
			voteForSem.acquire();
			voteFor = null;
			voteForSem.release();
		} catch (InterruptedException e) {
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * To set vote, the vote must be clear
	 * @param serverID
	 * @return
	 */
	public boolean setVote(ServerID serverID) {		
		boolean result = false;
		try {
			voteForSem.acquire();
			if(voteFor == null) {
				voteFor = serverID;
				result = true;
			} else {
				if(getRole().equals(ROLE.CANDIDATE) || getRole().equals(ROLE.LEADER)) {
					System.out.println("INCOHERENCIAAA!!! -> SOY LEADER y he votado...");
				}
			}
			voteForSem.release();
			
		} catch (InterruptedException e) {
			return false;
		}
		
		return result;
	}
	
	public ServerID getVote() {
		return voteFor;
	}
	
	public boolean incrementTerm(long increment) {
		try {
			termSem.acquire();
			term.setData(term.getData() + increment);
			clearVote();
			termSem.release();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	
	public long getTerm() {
		return term.getData();
	}
	
	public ROLE getRole() {
		return this.myRole;
	}
	
	private boolean setRole(ROLE role) {
		try {
			myRoleSem.acquire();
			myRole = role;
			myRoleSem.release();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	public ServerID getID() {
		return conf.serverID;
	}

	/**
	 * If a node won the election, then it is the leader.
	 * 
	 */
	public void won() {
		if(!getRole().equals(ROLE.CANDIDATE)) return;
		
		System.out.println("WON (" + getID().toString() + ")");
		
		stopElectionTimer();
				
		if(!setRole(ROLE.LEADER)) restart();
		setLeaderID(getID());
		
		nextIndex = new HashMap<>();
		matchIndex = new HashMap<>();
		
		long lastEntryIndex = log.getLastIndex();
		long lastEntryTerm = -1;
		if(lastEntryIndex >= 0) lastEntryTerm = log.get(lastEntryIndex).getTerm();
		
		(new SendHeartbeatTask<S,E>(conf.nodesSockets, conf.serverID, this.log, this)).run();
		resetSendHeartbeatTimer();
	}
	
	
	public void follow() {
		if(!getRole().equals(ROLE.OFFLINE)) return;
		
		if(!setRole(ROLE.FOLLOWER)) restart();
		
		resetHeartbeatTimer();
	}
	
	/**
	 * If a node suffers a coup, then it is no longer the leader,
	 * it becomes a follower.
	 * 
	 */
	public void coup(long term, ServerID leader) {
		// System.out.println("-------------> COUP <-------------");
		// System.out.println("[" + term + "," + getTerm() + "]");
		// System.out.println("[" + leader + "," + getLeaderID() + "]");
		// System.out.println(log.toString());
		
		stopSendHeartbeatTimer();
		stopElectionTimer();
		
		if(!setRole(ROLE.FOLLOWER)) restart();
		if(!setLeaderID(leaderID))  restart();
		if(!setTerm(term)) 			restart();
		
		resetHeartbeatTimer();
	}
	
	
	/**
	 * If a node does not see a leader, it tries to be the leader (it becomes a candidate).
	 * 
	 */
	public void candidate() {
		if(!getRole().equals(ROLE.FOLLOWER)) return;
		
		stopHeartbeatTimer();
		
		if(!setRole(ROLE.CANDIDATE)) restart();
	}
	
	
	/**
	 * The node is tired and go to sleep.
	 * 
	 */
	public void off() {
		stopHeartbeatTimer();
		stopElectionTimer();
		stopSendHeartbeatTimer();
		
		if(!setRole(ROLE.OFFLINE)) restart();
	}
	
	public void stopSendHeartbeatTimer() {
		if(sendHeartbeatTimer != null) sendHeartbeatTimer.cancel();	
	}
	
	public void resetSendHeartbeatTimer() {
		stopSendHeartbeatTimer();

		sendHeartbeatTimer = new Timer("Send Heartbeat Timer", false);		
		sendHeartbeatTimer.schedule(new SendHeartbeatTask<S,E>(conf.nodesSockets, conf.serverID, this.log, this), conf.SEND_HEARTBEAT);
	}
	
	
	
	
	public void stopElectionTimer() {
		if(leaderElectionTimer != null) leaderElectionTimer.cancel();
	}
	public void resetElectionTimer() {
		leaderElectionTimer = new Timer("Reset Election Timer", false);
		leaderElectionTimer.schedule(new HeartbeatTimerTask<>(this), conf.LEADER_ELECTION_TIME);
	}
	
	public void startLeaderElection() {
		candidate();
		
		long lastEntryIndex = log.getLastIndex();
		
		long lastEntryTerm = -1;
		if(lastEntryIndex >= 0) lastEntryTerm = log.get(lastEntryIndex).getTerm();
		
		(new LeaderElectionTask<S, E>(conf.nodesSockets, conf.serverID, this.log, this)).run();
	}
	
	public Configuration getConfiguration() {
		return conf;
	}
	
	
//	/**
//	 * Gets the current role of the active node.
//	 * 
//	 * @return The current role of the node.
//	 * 
//	 */
//	public ROLE getRole() {
//		return myRole;
//	}
//
//	public void appendEntry(E entry) {
//		/*
//		for(Map.Entry<ServerID,MyRASocket> s : conf.nodesSockets.entrySet()) {
//			if(s.getKey().equals(conf.serverID)) continue;
//		}
//		
//		*/
//	}
	
//	public Configuration getConfiguration() {
//		return this.conf;
//  }
	
	
//	/**
//	 * Gets a the entry in the log with the index specified.
//	 * 
//	 * @return The entry of the log with the index specified.
//	 * 
//	 */
//	public E getEntry(long index) {
//		return log.get(index).getEntry();
//	}
//	
//	/**
//	 * Gets a the log entry in the log with the index specified.
//	 * 
//	 * @return The log entry of the log with the index specified.
//	 * 
//	 */
//	public LogEntry<E> getLogEntry(long index) {
//		return log.get(index);
//	}
	
	
	
	
//	private boolean loadPersistentData() throws NotImplemented {
//		
//		setTerm(0);
//		voteFor = null;
//		log = new Log<>();
//		
//		throw new NotImplemented();
//	}
//	
//	private boolean savePersistentData() throws NotImplemented {
//		
//		throw new NotImplemented();
//	}
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////// NODE SERVICES ////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	public void restart() {
		// restart the node to a void state (clear all data because data is not consistent)
		// KILL AL THREADS AND TASKS
	}

	public void stopHeartbeatTimer() {
		if(heartbeatTimer != null) heartbeatTimer.cancel();
	}
	
	
	
	/**
	 * 
	 * @return	>0 my log is greater
	 *			=0 my log is equal
	 *			<0 my log is lower (not up-to-date) 
	 */
	public int compareLogs(long lastEntryIndex, long lastEntryTerm) {
		return log.equals(lastEntryIndex, lastEntryTerm);
	}
	
	/**
	 * Resets the timer
	 * 
	 * @param milliseconds Delay to execute the task.
	 * 
	 */
	public void resetHeartbeatTimer() {
		stopHeartbeatTimer();
		
		heartbeatTimer = new Timer("No received heartbeat timer", false);
		heartbeatTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// comentadas las dos lineas de abajo, porque al iniciar la votacion, incremento el term, al incrementar el term, en esa votacion no he votado a nadie
				// if(voteFor != null) return;	// ya hay una votacion en curso, no puedo participar en esta votacion
				// setVote(conf.serverID);	// me aseguro mi voto (podria recivir una solicitud antes de votarme a mi mismo una vez ya empezado mi vote election
				startLeaderElection();
			}
		}, Random.random(conf.MIN_HEARTBEAT, conf.MAX_HEARTBEAT));
	}
	
	
	public Pair<Boolean, Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm) {
		long myTerm = getTerm();
		
		LogEntry<E> lst = log.getLast();
		
		int cmp;
		if(lst == null) { // my log is empty
			if(lastEntryIndex < 0) 	cmp = 0;   // both are clear logs
			else 					cmp = -1;  // other node log has at least one entry
		} else {
			cmp = lst.compare(new LogEntry<E>(lastEntryIndex, lastEntryTerm, null));
		}
		
		
		if(cmp > 0) { // mines is bigger (more up-to-date)
			// System.out.println("WRONG1: " + myTerm + "-" + term);
			return new Pair<>(false,myTerm); // Your are not the leader
			
		} else if(cmp == 0) { // both are equal (equal up-to-date)
			// Leader is not the leader of this term
			// System.out.println("WRONG2 > " + (myTerm > term));
			if(myTerm > term) {
				return new Pair<>(false,myTerm); // Your are not the leader
			}
			
			
		}
		
		// else: less (less up-to-date) -> I cannot be the leader
		//       or
		//       equal (equal up-to-date) but my term is lower -> I cannot be the leader

		
		// I am more up-to-date than you -> your are not the leader
		// if(myTerm == term) if(log.equals(lastEntryIndex, lastEntryTerm) > 0) return new Pair<>(false,myTerm); // Your are not the leader
		
		
		// /// /// /// /// //		
		// VALID HEARTBEAT //
		// /// /// /// /// //
		stopHeartbeatTimer();
		
		if(getRole().equals(ROLE.CANDIDATE) || getRole().equals(ROLE.LEADER)) {
			coup(term, leaderID); // en empate -> yo no soy el leader
			
		} else {
			setLeaderID(leaderID);
			setTerm(term);
		}
		
		resetHeartbeatTimer();
		// LOG // System.out.println("HEARTBEAT [leader=" + leaderID.toString() + ", term=" + term + "]");
		return new Pair<>(true,term);
	}
	
	public Pair<Boolean, Long> heartbeat(ServerID leaderID, long term, long lastEntryIndex, long lastEntryTerm, long commitIndex) {
		Pair<Boolean,Long> response = heartbeat(leaderID, term, lastEntryIndex, lastEntryTerm);
		
		// It is not the leader (if it cannot heartbeat me, it is not the leader)
		commit(commitIndex);
		
		return response;
	}
	
	
	
	// Para asemejar los nombres con la maquina de estados, hace lo mismo que la funcion appendEntry. Debe ser usada por los el cliente.
	public void next(E entry) { appendEntry(entry); }
	
	// Follower
	public void appendEntry(E entry) {
		// nuevo hilo que envia append al leader
		// appendEntry(conf.serverID, entry)
		boolean stoped = appendEntriesThread.stoped();
		
		appendEntriesThread.add(entry);
		
		if(stoped) {
			// If thread has end -> restart it, so it can append this entry
			Thread t = new Thread(appendEntriesThread);
			t.start();
		}
		
	}
	
	/*
	// RPC
	// Boolean -> is going to be proceded (completed? maybe) -> when it is procesed, it will be appended to the Node that requested this append, and when it is commited, this node will know it because when it is the leader commits it, the leader will send a heartbeat to the request node to know it 
	// ServerID -> leaderID (if i am not the leader, it will point to what i am considering as the leader)
	public Pair<Boolean, ServerID> appendEntry(ServerID requestNode, E entry) {
		if(!this.myRole.equals(ROLE.LEADER)) return new Pair<Boolean, ServerID>(false,getLeaderID());
		// nuevo hilo que envia entry a los followers
		
		// appendEntry
		
		return new Pair<Boolean, ServerID>(true,getLeaderID());
	}*/
	
	
	// Received by server
	public Pair<Boolean, ServerID> appendEntry(ServerID requestNode, E entry) {
		if(!getRole().equals(ROLE.LEADER)) return new Pair<>(false,getLeaderID());	// im not the leader, but i will send you the ones who is
		
		LeaderAppendEntryThread<S,E> th = new LeaderAppendEntryThread<S,E>(requestNode, entry, getConfiguration().nodesSockets.entrySet(), log, this); 
		Thread t = new Thread(th);
		t.start();
		
		return new Pair<>(true,getLeaderID()); // the request will be procesed
	}
	
	/**
	 * Si el leader es el autor de esta llamada, realiza el append en este Nodo
	 * @param leaderID
	 * @param term
	 * @param prevLogEntryIndex
	 * @param prevLogEntryTerm
	 * @param logEntry
	 * @param commitIndex
	 * @return
	 */
	public Pair<Boolean, Long> appendEntry(ServerID leaderID, long term, long prevLogEntryIndex, long prevLogEntryTerm, LogEntry<E> logEntry, long commitIndex) {
		/*
		Pair<Boolean,Long> response = heartbeat(leaderID, term, prevLogEntryIndex, prevLogEntryTerm, commitIndex);
		// It is not the leader (if it cannot heartbeat me, it is not the leader)
		if(!response.getFirst()) return response;
		*/
		
		if(!leaderID.equals(getLeaderID())) {
			return new Pair<Boolean, Long>(false, -2L); // Not leader
		}
		
		if(log.getLastIndex() == -1) {
			
			if(prevLogEntryIndex != -1) return new Pair<Boolean,Long>(false, -1L);
			
			// my log is clear
		} else {
			LogEntry<E> aux = log.get(prevLogEntryIndex);
			if(aux == null) return new Pair<Boolean, Long>(false, -1L);								// log does not contain the previous entry
			
			
			if(aux != null) {
				
				
				if(aux.getTerm() != prevLogEntryTerm) return new Pair<Boolean, Long>(false, -1L);	// log contain a previous entry that is not correct
			}
			
			aux = log.get(logEntry.getIndex());
			if(aux != null) if(aux.getTerm() != logEntry.getTerm()) log.removeFrom(aux.getIndex()); 					// existing entry conflict with previous
			
			if(aux != null) if(aux.getTerm() == logEntry.getTerm()) return new Pair<Boolean, Long>(true, 0L); 	// same entry -> do not nothing
			
		}
		
		if(log.append(logEntry.getTerm(), logEntry.getEntry()) < 0) restart(); // append entry
		
		commit(Math.min(commitIndex, log.getLastIndex()));
		
		return new Pair<Boolean, Long>(true, 0L);
		

	}
	
	public Pair<Boolean, Long> requestVote(ServerID candidateID, long term, long lastLogIndex, long lastLogTerm) {
		System.out.println("REQUEST VOTE");
		Pair<Boolean,Long> response = heartbeat(candidateID, term, lastLogIndex, lastLogTerm);

		// It is not the leader (if it cannot heartbeat me, it is not the leader)
		if(!response.getFirst()) return response;
		
		// Check if I voted, if i not voted, i try to vote
		if(!setVote(candidateID)) return new Pair<Boolean, Long>(false, getTerm());
		return new Pair<Boolean, Long>(true, term);
		
		
		// if(lastEntry != null) if(lastEntry.compare(new LogEntry<E>(lastLogIndex, lastLogTerm, null)) < 0) return new Pair<>(false, getTerm());	// not up to date
	}
}



