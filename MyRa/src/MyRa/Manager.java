package MyRa;


import MyRa.data.IPv4Address;
import MyRa.data.ServerID;

/**
 * Passive node that can make changes in distributed system configuration.
 * 
 * @author de la Parra Hernández, Adrián (Airondlph)
 *
 */
public class Manager extends Agent {
	/**
	 * Initializes the manager (passive agent)
	 * 
	 * @param id Server id of the agent (must be unique)
	 * @param ip IPv4 of the device
	 * @param port Port number that will be used by the node (must be unique for the device)
	 * 
	 */
	public Manager(ServerID id, IPv4Address ip, long port) {
		super(id, ip, port);	
	}
	
	/**
	 * Initializes the manager (passive agent), making the agent a passive agent in the distributed system.
	 * 
	 * @param basicAgent the agent that will be converted to an active node.
	 * 
	 */
	public Manager(Agent basicAgent) {
		super(basicAgent);
	}
}
