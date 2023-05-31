package MyRa.data;

import java.io.Serializable;

import MyRa.err.NotImplemented;

/**
 * Type of ID used to identify an agent in MyRA
 * 
 * @author de la Parra Hernández, Adrián (Airondlph)
 *
 */
public class ServerID implements Serializable {
	private static final long serialVersionUID = 20L;
	// private byte id[] = new byte[16];
	private String id;
	
	public ServerID(String id) {
		this.id = id;
	}
	
	/*
	public byte[] getServerID() {
		return id;		
	}
	*/
	public String getServerID() {
		return id;
	}
	
	
	public String toString() {
		return id;
	}
	
	@Override
	public boolean equals(Object o) {
		ServerID aux = (ServerID)o;
		/*
		for(int i = 0; i < this.id.length; i++) {
			if(this.id[i] != aux.id[i]) return false;
		}
		*/
		
		
		// return true;
		
		return id.equals(aux.id);
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
}
