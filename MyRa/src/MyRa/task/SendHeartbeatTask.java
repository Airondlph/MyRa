package MyRa.task;

import java.util.Map;
import java.util.TimerTask;

import MyRa.Node;
import MyRa.data.Log;
import MyRa.data.MyRASocket;
import MyRa.data.ServerID;
import MyRa.thread.SendHeartbeatThread;

public class SendHeartbeatTask<S,E> extends TimerTask {
	private Node<S,E> node;
	private Log<E> log;
	
	private Map<ServerID, MyRASocket> sockets;
	private ServerID me;
	
	public SendHeartbeatTask(Map<ServerID, MyRASocket> sockets, ServerID me, Log<E> log, Node<S,E> node) {
		this.node = node;
		this.log = log;
		
		this.sockets = sockets;
		this.me = me;		
	}
	
	
	@Override
	public void run() {
		SendHeartbeatThread<S,E> th = new SendHeartbeatThread<S,E>(sockets, me, log, node);
		Thread t = new Thread(th);
		t.start();
	}
}
