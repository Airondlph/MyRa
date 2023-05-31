package MyRa.data;


/**
 * This socket has all necessary information to communicate with the node. This class is immutable.
 * @author airon
 *
 */
public class MyRASocket implements Immutable {
	final public IPv4Address addr;
	public int port; // SOCKET DEL NODO AL QUE LE ENVIAS LA REQUEST // ESTE OBJETO TIENE TODA LA INFORMACION NECESARIA PARA HACER LA RPC, PERO NO TIENE EL OBJETO PARA hacer el send()
	
	public MyRASocket(String ipv4Addr, int port) {
		this.addr = new IPv4Address(ipv4Addr);
		this.port = port;
	}
	
	
	public IPv4Address getAddr() {
		return this.addr;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String toString() {
		return "(" + addr + "," + port + ")";
	}
}
