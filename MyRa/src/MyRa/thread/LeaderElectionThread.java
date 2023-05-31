package MyRa.thread;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import MyRa.Node;
import MyRa.Node.ROLE;
import MyRa.data.Log;
import MyRa.data.MyRASocket;
import MyRa.data.ServerID;
import MyRa.rmi.NodeServerAPI;

import static utils.generator.Random.random; // Importo solo la funcion estatica
import utils.structures.Pair;

public class LeaderElectionThread<S,E> implements Runnable {
	public enum ELECTION_STATE {IN_PROCESS, ABORTED, DONE};
	
	
	private Node<S,E> node;
	private Log<E> log;
	
	private Map<ServerID, MyRASocket> sockets;
	private ServerID me;
	private long term;
	private long lastEntryIndex;
	private long lastEntryTerm;
	
	private long RETRY_VOTE;
	private Timer retryTimer;

	private Semaphore stateSem;
	private ELECTION_STATE state;
	private Semaphore votesSem;
	private long votes;
	
	
	
	
	public LeaderElectionThread(Map<ServerID, MyRASocket> sockets, ServerID me, Log<E> log, Node<S,E> node) {
		if(!node.setVote(me)) node.restart();
		
		long min = node.getConfiguration().MIN_HEARTBEAT;
		long max = node.getConfiguration().MAX_HEARTBEAT;
		this.RETRY_VOTE = random(min, max);
		
		
		this.node = node;
		this.log = log;

		this.stateSem = new Semaphore(1);
		this.state = ELECTION_STATE.IN_PROCESS;
		
		this.votesSem = new Semaphore(1);
		this.votes = 0; // 0 because when this thread starts, it will make a end(true) my vote to myself. 
		
		this.sockets = sockets;
		
		this.me = me;
		
		if(!node.incrementTerm(1)) node.restart();
		this.term = node.getTerm();
		
		lastEntryIndex = log.getLastIndex();
		lastEntryTerm = -1;
		if(lastEntryIndex >= 0) lastEntryTerm = log.get(lastEntryIndex).getTerm();
		
		
		// LOG // System.out.println("N Sockets: " + sockets.size());
	}
	
	private void startTimer() {
		retryTimer = new Timer("Retry vote", false);
		
		retryTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				boolean done = false;
				
				try {
					stateSem.acquire();
					done = state.equals(ELECTION_STATE.DONE);
					if(!done) state = ELECTION_STATE.ABORTED;
					stateSem.release();

				} catch (InterruptedException e) { node.restart(); }
				
				// Timeout without a won -> retry only if I am a candidate
				if(!done) if(node.getRole().equals(ROLE.CANDIDATE)) (new LeaderElectionThread<>(sockets, me, log, node)).run();
			}
		}, this.RETRY_VOTE);
	}
	
	private void stopTimer() {
		if(retryTimer != null) {
			retryTimer.cancel();
			retryTimer.purge();
		}
	}
	
	public void end(boolean vote) {		
		if(state != ELECTION_STATE.IN_PROCESS) return; // election has end (win or retry, this is not valid anymore)
		
		boolean won = false;
		if(vote) {
			try {
				this.votesSem.acquire();
				this.votes = this.votes + 1;
				this.votesSem.release();
				// vote solo puede crecer, por lo que no necesito bloquear la variable con el semaforo
				if(this.votes > Math.floor(this.sockets.size()/2)) { // votes >= 1 + N/2
					this.stateSem.acquire(); // la condicion anterior, si se cumple una vez, no se puede no cumplir en el futuro (por eso da igual poner el semaforo dentro)
					if(this.state == ELECTION_STATE.IN_PROCESS) {
						stopTimer();
						this.state = ELECTION_STATE.DONE;
						won = true;
					}
					this.stateSem.release();
				}
			} catch (InterruptedException e) { node.restart(); }
		}
		
		if(won) node.won();
		
	}
	
	@Override
	public void run() {
		if(node.getRole().equals(Node.ROLE.FOLLOWER) || node.getRole().equals(Node.ROLE.OFFLINE)) return;	// Thre is a coup, so a leader has been elected... I will stop... I fail... Sorry :(
		
		startTimer();
		
		for(Map.Entry<ServerID, MyRASocket> e : sockets.entrySet()) {			
			if(e.getKey().equals(me)) continue;
			
			// LOG // 
			System.out.println("REQUEST TO " + e.toString());
			Thread task = new Thread(new SendRequestVote(e.getKey(), e.getValue(), me, term, lastEntryIndex, lastEntryTerm, this));
			task.start();
		}
		
		end(true); // If there is only one node (me), someone must done the end(). That is me. This is why "votes" its initialized with 0.
	}
	
	
	
	
	
	private class SendRequestVote implements Runnable {
		private LeaderElectionThread<S,E> parent;
		
		private ServerID id;
		private MyRASocket socket;
		
		// Request data
		private ServerID me;
		private long term;
		long lastEntryIndex;
		long lastEntryTerm;
		
		public SendRequestVote(ServerID id, MyRASocket socket, ServerID me, long term, long lastEntryIndex, long lastEntryTerm, LeaderElectionThread<S,E> parent) {
			this.parent = parent;
			
			this.id = id;
			this.socket = socket;
			
			this.me = me;
			this.term = term;
			this.lastEntryIndex = lastEntryIndex;
			this.lastEntryTerm = lastEntryTerm;
		}
		
		public void sendHeartBeat() {				
			String service = "MyRA/" + id;
			
			String host = socket.getAddr().toString();
			int port = socket.getPort();
			
				
			Pair<Boolean, Long> response = null;	
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				@SuppressWarnings("unchecked")
				NodeServerAPI<S,E> s = (NodeServerAPI<S,E>) registry.lookup(service);
				response = s.heartbeat(me, term, lastEntryIndex, lastEntryTerm);
			} catch (RemoteException | NotBoundException e) {
				// e.printStackTrace();
				// error, heartbeat not sent
			}
			
			// LOG // System.out.println("RESPONSE: " + response);
			if(response != null) parent.end(response.getFirst());
		}
		
		@Override
		public void run() {
			sendHeartBeat();
		}
	}
}
