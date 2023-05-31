package MyRa.thread;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import MyRa.Node;
import MyRa.Node.ROLE;
import MyRa.data.Configuration;
import MyRa.data.MyRASocket;
import MyRa.data.ServerID;
import MyRa.rmi.NodeServerAPI;
import utils.structures.Pair;

public class SendToLeaderAppendEntry<S,E> implements Runnable {
	private Node<S,E> node;
	
	private Semaphore sem;
	private Queue<E> entries;
	
	public SendToLeaderAppendEntry(Node<S,E> node) {
		this.node = node;
		entries = new LinkedList<E>();
		sem = new Semaphore(1);
	}
	
	// Checks if the thread is stoped (has end == no more entries to append)
	public boolean stoped() {
		return entries.isEmpty();
	}
	
	public boolean add(E entry) {
		try {
			sem.acquire();
			entries.add(entry);
			sem.release();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public void sendAppendEntry(E entry) {
		while(true) {
			// If I dont know the leader, or i am offline or i am a candidate -> wait, you cannot append the entry
			while(node.getLeaderID() == null || node.getRole().equals(ROLE.OFFLINE) || node.getRole().equals(ROLE.CANDIDATE)) {
				try {
					TimeUnit.MILLISECONDS.sleep(500);
				} catch (InterruptedException e) { node.restart(); }
			}
			
			Pair<Boolean, ServerID> response = null;
			
			String service = "MyRA/" + node.getLeaderID();

			MyRASocket socket = node.getConfiguration().getSocket(node.getLeaderID());			
			String host = socket.getAddr().toString();
			int port = socket.getPort();
			// LOG // System.out.println("SENDING... " + entry);
			try {
				Registry registry = LocateRegistry.getRegistry(host, port);
				@SuppressWarnings("unchecked")
				NodeServerAPI<S,E> s = (NodeServerAPI<S,E>) registry.lookup(service);
				response = s.appendEntry(node.getID(), entry);
				if(response.getFirst()) return;
				
				node.setLeaderID(response.getSecond());
				
			} catch (RemoteException | NotBoundException e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	@Override
	public void run() {
		// LOG // System.out.println("Queue: " + entries.size());
		while(!entries.isEmpty()) {
			try {
				sem.acquire();
				sendAppendEntry(entries.peek());
				entries.poll();	// after start append -> remove last
				sem.release();
				// LOG // System.out.println("Queue: " + entries.size());
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
