package MyRa;

import MyRa.data.IPv4Address;
import MyRa.data.ServerID;

/**
 * Agent actively or passively participating in the consensus algorithm
 * 
 * @author de la Parra Hernández, Adrián (Airondlph)
 *
 */
public class Agent {
	private ServerID id;
	private IPv4Address ip;
	private long port;
	
	
	/**
	 * Initializes a basic agent only with its communication information.
	 * 
	 * @param id Server id of the agent (must be unique)
	 * @param ip IPv4 of the device
	 * @param port Port number that will be used by the node (must be unique for the device)
	 * 
	 */
	public Agent(ServerID id, IPv4Address ip, long port) {
		this.id 	= id;
		this.ip 	= ip;
		this.port 	= port;	
	}
	
	/**
	 * Initializes a basic agent only with its communication information.
	 * This class constructor is only to be used by extended classes. 
	 * Agent does not have a copy class constructor.
	 * 
	 * @param basicAgent Agent with the information
	 * 
	 */
	protected Agent(Agent basicAgent) {
		this.id 	= basicAgent.id;
		this.ip 	= basicAgent.ip;
		this.port 	= basicAgent.port;		
	}
}
