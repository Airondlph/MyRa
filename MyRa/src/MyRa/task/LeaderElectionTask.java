package MyRa.task;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import MyRa.Node;
import MyRa.data.Data;
import MyRa.data.Log;
import MyRa.data.MyRASocket;
import MyRa.data.ServerID;
import MyRa.thread.LeaderElectionThread;
import MyRa.thread.LeaderElectionThread.ELECTION_STATE;

public class LeaderElectionTask<S,E> extends TimerTask {
	private Node<S,E> node;
	private Log<E> log;
	
	private Map<ServerID, MyRASocket> sockets;
	private ServerID me;
	
	public LeaderElectionTask(Map<ServerID, MyRASocket> sockets, ServerID me, Log<E> log, Node<S,E> node) {
		this.node = node;
		this.log = log;
		
		this.sockets = sockets;	
		this.me = me;
	}
	

	
	@Override
	public void run() {
		System.out.println("Starting election");
		
		LeaderElectionThread<S,E> th = new LeaderElectionThread<S,E>(sockets, me, log, node);
		Thread t = new Thread(th);
		t.start();
	}
}
