package MyRa.data;

import java.io.Serializable;

/**
 * Entry of the Log with additional information (index and term) -> Log entry = [index,term,entry].
 * This type is inmutable.
 * 
 * @author de la Parra Hernández, Adrián (Airondlph)
 *
 * @param <E> Entry type of the state machine
 * 
 */
public class LogEntry<E> implements Serializable {
	private static final long serialVersionUID = 21L;
	
	final private long index;
	final private long term;
	final private E entry;
	
	
	/**
	 * Creates a log entry
	 * 
	 * @param index Index of the log entry
	 * @param term Term of the log entry
	 * @param entry Entry or value of the log entry
	 * 
	 */
	public LogEntry(long index, long term, E entry) {
		this.index 		= index;
		this.term 		= term;
		this.entry 		= entry;
	}	
	
	
	/**
	 * Compare a First entry with a Second entry.
	 * 
	 * @param second Entry compared with first entry
	 * 
	 * @return If First entry is lower (older) than Second entry, returns -1
	 * 		   If First entry is bigger (newer) than Second entry, returns 1
	 * 		   If First entry is equal (the same) than Second entry, returns 0 
	 * 
	 */
	public byte compare(LogEntry<E> second) {
		// check term
		if(this.term > second.term) {
			System.out.println("\n----------->" + this.term + " - " + second.term);
			
			return 1;
		}
		if(this.term < second.term) return -1;
		
		// same term -> check index
		if(this.index > second.index) return 1;
		if(this.index < second.index) return -1;
		
		return 0; // same term and same index -> same entry
	}
	
	
	// Getters
	
	/**
	 * Gets the index of the log entry.
	 * 
	 * @return Returns the index of the log entry.
	 * 
	 */
	public long getIndex() {
		return index;
	}
	
	
	/**
	 * Gets the term of the log entry.
	 * 
	 * @return Returns the term of the log entry.
	 * 
	 */
	public long getTerm() {
		return term;
	}
	
	
	/**
	 * Gets a copy of the entry of the log entry.
	 * 
	 * @return Returns the entry of the log entry.
	 * 
	 */
	public E getEntry() {
		if(entry == null) return null;
		return entry;
	}
	
	
	/**
	 * Converts the LogEntry to a readable string.
	 *  
	 */
	public String toString() {
		return ""
				+ " { index : " + index + ", "
				+ "term : " + term + ", "
				+ "entry : " + entry
				+ " }";
	}
	
	
	@Override
	public boolean equals(Object o) {
		LogEntry<E> aux = (LogEntry<E>)o;
		return ((this.index == aux.index) && (this.term == aux.term));
	}
	
	@Override
	public int hashCode() {
		return this.hashCode();
	}
}
