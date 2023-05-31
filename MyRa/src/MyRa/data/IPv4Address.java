package MyRa.data;

import java.util.regex.Pattern;

/**
 * Represents a typical IPv4 address.
 * 
 * @author de la Parra Hern�ndez, Adri�n (Airondlph)
 *
 */
public class IPv4Address implements Immutable {
	private byte address[] = new byte[8];
	private boolean localhost;
	
	
	/**
	 * IPv4 address specified from more significant byte to less significant byte 
	 * from 0000.0000.0000.0000 to 1111.1111.1111.1111 
	 * or from 0.0.0.0 to 255.255.255.255
	 * 
	 * Significant order: B3.B2.B1.B0 == FEDC.BA98.7654.3210
	 * 
	 * @param B3 More significant byte
	 * @param B2 Second more significant byte
	 * @param B1 Third more significant byte
	 * @param B0 Less significant byte
	 * 
	 */
	public IPv4Address(byte B3, byte B2, byte B1, byte B0) {
		localhost = false;
		loadAddr(B3,B2,B1,B0);
	}
	
	
	/**
	 * IPv4 address specified from more significant byte to less significant byte using a string. 
	 * from "0.0.0.0" to "255.255.255.255"/
	 * 
	 * @param addr IPv4 address in string format.
	 * 
	 */
	public IPv4Address(String addr) {
		localhost = false;
		try {
			String[] addrBytesStr = addr.split(Pattern.quote("."));
			loadAddr((byte)Integer.parseInt(addrBytesStr[0]), 
					(byte)Integer.parseInt(addrBytesStr[1]), 
					(byte)Integer.parseInt(addrBytesStr[2]), 
					(byte)Integer.parseInt(addrBytesStr[3]));
		} catch (Exception e) {
			if(addr.toLowerCase().equals("localhost")) {
				localhost = true;
			} else {
				System.err.println("La direccion IPv4 no es valida ([localhost] U [0.0.0.0, 255.255.255.255])");
				e.printStackTrace();
			}
		}
		
	}
	
	
	/**
	 * Loads the bytes of the address.
	 * 
	 * @param B3 More significant byte
	 * @param B2 Second more significant byte
	 * @param B1 Third more significant byte
	 * @param B0 Less significant byte
	 * 
	 */
	private void loadAddr(byte B3, byte B2, byte B1, byte B0) {		
		address[3] = B3;
		address[2] = B2;
		address[1] = B1;
		address[0] = B0;
	}
	
	
	
	
	/**
	 * Converts the IPv4Address to a readable string.
	 *  
	 */
	public String toString() {
		if(localhost) return "localhost";
		return "" 
				+ (address[3] & 0xFF) + "."
				+ (address[2] & 0xFF) + "."
				+ (address[1] & 0xFF) + "."
				+ (address[0] & 0xFF);
	}
	
	
	@Override
	public boolean equals(Object o) {
		IPv4Address aux = (IPv4Address)o;
		
		if(this.localhost == aux.localhost) return true;
		
		for(int i = 0; i < this.address.length; i++) {
			if(this.address[i] != aux.address[i]) return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return this.address.hashCode();
	}

}
