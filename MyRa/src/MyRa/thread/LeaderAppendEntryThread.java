package MyRa.thread;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import MyRa.Node;
import MyRa.Node.ROLE;
import MyRa.data.Data;
import MyRa.data.Log;
import MyRa.data.LogEntry;
import MyRa.data.MyRASocket;
import MyRa.data.ServerID;
import MyRa.rmi.NodeServerAPI;
import utils.structures.Pair;

public class LeaderAppendEntryThread<S,E> implements Runnable {
	private Node<S,E> node;
	private Log<E> log;
	private Set<Map.Entry<ServerID, MyRASocket>> cluster;
	
	private ServerID myID;
	
	private ServerID requestNode;
	private E entry;
	private long term;
	
	
	private LogEntry<E> logEntry;
	
	private Semaphore countSem;
	private long count;
	
	
	public LeaderAppendEntryThread(ServerID requestNode, E entry, Set<Map.Entry<ServerID, MyRASocket>> cluster, Log<E> log, Node<S,E> node) {
		this.node = node; // only for detect coups and get the role
		this.log = log;
		this.myID = node.getID();
		this.cluster = cluster;
		
		this.requestNode = requestNode;
		this.entry = entry;
		this.term = node.getTerm();
		
		countSem = new Semaphore(1);
		count = 0;		
	}
	
	public void appendEntry() {
		long index = log.append(term, entry);
		
		if(index < 0) {
			node.restart();
			return;
		}
		
		logEntry = log.get(index);
		
		end(true);
		
		
		
		ServerID to;
		MyRASocket socket;
		
		for(Map.Entry<ServerID, MyRASocket> e : cluster) {
			if(e.getKey().equals(myID)) continue; // I dont send appends to myself
				
			to = e.getKey();
			socket = e.getValue();
			
			if(to.equals(myID)) continue;
			
			Thread t = new Thread(new SendAppendEntry(to, socket));
			t.start();
		}
	}
	
	private void advertise(ServerID id) {
		String service = "MyRA/" + id;
		
		MyRASocket socket = node.getConfiguration().getSocket(id);
		String host = socket.getAddr().toString();
		int port = socket.getPort();
		
		try {
			Registry registry = LocateRegistry.getRegistry(host, port);
			@SuppressWarnings("unchecked")
			NodeServerAPI<S,E> s = (NodeServerAPI<S,E>) registry.lookup(service);
			
			s.advertise();				
		} catch (RemoteException | NotBoundException e) { e.printStackTrace(); }
		
	}
	
	public void end(boolean result) {
		if(!result) return;
		
		try {
			countSem.acquire();
			++count;
			countSem.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(count == (1+(cluster.size()/2))) {
			System.out.println("COMMIT");
			node.commit(logEntry.getIndex());
			advertise(requestNode);
		}
	}

	@Override
	public void run() {		
		appendEntry();		
	}
	
	private class SendAppendEntry implements Runnable {
		private ServerID to;
		private MyRASocket socket;
		
		public SendAppendEntry(ServerID to, MyRASocket socket) {
			this.to = to;
			this.socket = socket;
		}
		
		public boolean sendAppend() {
			String service = "MyRA/" + to;
			
			String host = socket.getAddr().toString();
			int port = socket.getPort();
			
				
			Pair<Boolean, Long> response = null;
			
			LogEntry<E> sentEntry = logEntry;
			
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				@SuppressWarnings("unchecked")
				NodeServerAPI<S,E> s = (NodeServerAPI<S,E>) registry.lookup(service);
				
				
				while(true) {
					LogEntry<E> prevEntry = log.getBefore(sentEntry.getIndex());
					long prevIndex = -1;
					long prevTerm = -1;
					if(prevEntry != null) {
						prevIndex = prevEntry.getIndex();
						prevTerm = prevEntry.getTerm();
					}
					
					
					if(!node.getRole().equals(ROLE.LEADER) || (logEntry == null)) return false; // I am not the leader or something happen with the log entry (other node has remove it or rare situation)
					
					response = s.appendEntry(myID, node.getTerm(), prevIndex, prevTerm, sentEntry, node.getLastCommit());
					
					
					
					/* Posibles responses and what to do
					   False:
							-2 -> You are not the leader... -> STOP
							-1 -> Previous is not correct   -> Send previous one 

						True:
							All okey 						-> Send next
					 */
					if(response.getFirst()) {
						if(logEntry == null) return false; // defensive code
						if(sentEntry.equals(logEntry)) return true; 	// para no sobrecargar de llamadas al receptor, si la entrada que tenia como objetivo añadir ya esta -> fin 
																		// (si hay alguna nueva despues de esta, habra otro proceso encargado de ello)
						
						sentEntry = log.getAfter(sentEntry.getIndex()); // continues until the log is as up to date as mine
						if(sentEntry == null) return true;				// end -> log is up to date

					} else {
						if(response.getSecond() == -1) {
							sentEntry = log.getBefore(sentEntry.getIndex()); // (after an empty log there is the first log)
						
						} else { // == -2 -> I am not the leader || other code -> code not valid
							return false;
						}
					}
				}				
			} catch (RemoteException | NotBoundException e) { 
				// e.printStackTrace(); 
				return false; 
			}
		}
		
		@Override
		public void run() {
			boolean r = sendAppend();
			// LOG // 
			System.out.println("End: " + to.toString() + " -> " + r);
			end(r);
		}
	}
}
