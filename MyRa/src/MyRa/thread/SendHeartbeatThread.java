package MyRa.thread;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.TimerTask;
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

public class SendHeartbeatThread<S,E> implements Runnable {	
	private Node<S,E> node;
	private Log<E> log;
	
	private Map<ServerID, MyRASocket> sockets;
	private ServerID me;
	
	
	private long lastEntryIndex;
	private long lastEntryTerm;
	
	private long lstCommited;
	
	private long term;
	
	

	
	public SendHeartbeatThread(Map<ServerID, MyRASocket> sockets, ServerID me, Log<E> log, Node<S,E> node) {
		this.node = node;
		this.log = log;
		
		this.sockets = sockets;
		this.me = me;
		
		term = node.getTerm();
	}
	
	public void end() {}
	
	@Override
	public void run() {
		if(!node.getRole().equals(ROLE.LEADER)) {
			this.node.stopSendHeartbeatTimer();
			return;
		}
		
		// LOG // System.out.println("Sending... HEARTBEAT (" + term + ") (" + node.getRole() + ") <" + lastEntryIndex + "," + lastEntryTerm + ">" + log.toString());
		for(Map.Entry<ServerID, MyRASocket> e : sockets.entrySet()) {
			LogEntry<E> lst = log.getLast();
			if(lst == null) {
				lastEntryIndex = -1;
				lastEntryTerm = -1;
				lstCommited = -1;
			} else {
				lastEntryIndex = lst.getIndex();
				lastEntryTerm = lst.getTerm();
				lstCommited = log.getLastCommittedIndex();
			}
			
			
			if(e.getKey().equals(me)) continue;
			Thread task = new Thread(new SendHeartbeatTask(e.getKey(), e.getValue(), me, term, lastEntryIndex, lastEntryTerm, this));
			task.start();
		}
		
		this.node.resetSendHeartbeatTimer();
	}
	
	
	private class SendHeartbeatTask implements Runnable {
		private SendHeartbeatThread<S,E> parent;
		
		private ServerID id;
		private MyRASocket socket;
		
		// Request data
		private ServerID me;
		private long term;
		long lastEntryIndex;
		long lastEntryTerm;
		
		public SendHeartbeatTask(ServerID id, MyRASocket socket, ServerID me, long term, long lastEntryIndex, long lastEntryTerm, SendHeartbeatThread<S,E> parent) {
			this.parent = parent;
			
			this.id = id;
			this.socket = socket;
			
			this.lastEntryIndex = lastEntryIndex;
			this.lastEntryTerm = lastEntryTerm;
			
			this.me = me;
			this.term = term;
		}
		
		public void sendHeartBeat() {				
			String service = "MyRA/" + id;
			
			String host = socket.getAddr().toString();
			int port = socket.getPort();
			
			Pair<Boolean, Long> response = null;	
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				NodeServerAPI<S,E> s = (NodeServerAPI<S,E>) registry.lookup(service);
				if(lstCommited < 0)		response = s.heartbeat(me, term, lastEntryIndex, lastEntryTerm);
				else 					response = s.heartbeat(me, term, lastEntryIndex, lastEntryTerm, lstCommited);
			} catch (RemoteException | NotBoundException e) {
				// e.printStackTrace();
				// error, heartbeat not sent
			}
				
			parent.end();
		}
		
		@Override
		public void run() {
			if(!node.getRole().equals(ROLE.LEADER)) return;
			
			sendHeartBeat();
		}
	}
}
