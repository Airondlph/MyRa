package MyRa;

import java.util.TimerTask;

public class HeartbeatTimerTask<S,E> extends TimerTask {
	private Node<S,E> node;
	
	public HeartbeatTimerTask(Node<S,E> node) {
		this.node = node;
	}

	@Override
	public void run() {
		node.startLeaderElection();
	}	
}
