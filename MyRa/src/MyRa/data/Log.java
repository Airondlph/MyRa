package MyRa.data;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.HashMap;


/**
 * Basic log of entries with some additional information (index and term).
 * It protects setters and the method pop() for concurrent access. 
 * (But does not protect the modification of entries made out of this class)
 * 
 * @author de la Parra Hern�ndez, Adri�n (Airondlph)
 *
 * @param <E> Entry type of the state machine
 * 
 */
public class Log<E> {
	private Semaphore commitSem;
	private Semaphore mapSem;
	
	private long lastIndex;
	private long lastCommit;
	private Map<Long,LogEntry<E>> entries;
	
	
	/**
	 * Creates a log, and initializes it to default values (not commits, not entries, size = 0, etc.).
	 * 
	 */
	public Log() {
		commitSem = new Semaphore(1);
		mapSem = new Semaphore(1);
		
		lastCommit = -1;
		lastIndex = -1;
		entries = new HashMap<>();
	}
	
	
	/**
	 * Adds to the end (last position) a new log entry to the log
	 * 
	 * @param term Current term
	 * @param entry Entry that will be added
	 * 
	 * @return lastIndex if the entry has been append;
	 * or -1 if the Log is full (recommend to make a snapshot)
	 *  
	 */
	public long append(long term, E entry) {
		try {
			mapSem.acquire();
			
			long index = lastIndex + 1;	// esta protegido porque para modificarlo se usa tambien el semaforo de map (mapSem)
			
			if(index >= Long.MAX_VALUE) return -1; // Log is full (recommend to make a snapshot) 
			
			entries.put(index, new LogEntry<E>(index, term, entry));
			
			lastIndex = index;
			
			mapSem.release();
			return lastIndex;
			
		} catch (InterruptedException e) {
			return -1;
		}
	}
	
	
	/**
	 * Commit the log entry with the index specified.
	 * 
	 * @param index Index of the entry that will be committed.
	 * 
	 * @return True if the entry has been committed; 
	 * or False if the log does not have any entry with the index specified.
	 * 
	 */
	public boolean commit(long index) {
		System.out.println("COMIIITTT!!!!");
		try {
			commitSem.acquire();
			if(getLastIndex() < index) return false;
			lastCommit = index;
			
			commitSem.release();
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	
	/**
	 * Gets the size of the log (committed and not committed entries)
	 * 
	 * @return the size of the log.
	 * 
	 */
	public long getLastIndex() {
		return lastIndex;
	}
	
	
	
	/**
	 * Gets the size of the committed log (only count committed entries)
	 * 
	 * @return the number of committed entries of the log.
	 * 
	 */
	public long getLastCommittedIndex() {
		return lastCommit;
	}
	
	
	/**
	 * Gets the log entry of the log with the index specified.
	 * 
	 * @param index Index of the entry that wants to get.
	 * 
	 * @return null if the index is out of the range of the log;
	 * or the log entry with the index specified if the index is on the range of the log.
	 * 
	 */
	public LogEntry<E> get(long index) {
		if(index < 0) return entries.get((long)0);
		return entries.get(index);
	}
	
	
	/**
	 * Gets the log entry of the log before the index (entry with that index) specified.
	 * 
	 * @param index Index of the entry of reference.
	 * 
	 * @return null if there is not log entry before the specified.
	 * or the log entry before the index specified if entry before is on the log.
	 * 
	 */
	public LogEntry<E> getBefore(long index) {
		if((index-1) <= -1) return null;
		return get(index-1);
	}
	
	
	/**
	 * Gets the log entry of the log after the index (entry with that index) specified.
	 * 
	 * @param index Index of the entry of reference.
	 * 
	 * @return null if there is not log entry after the specified.
	 * or the log entry after the index specified if entry after is on the log.
	 * 
	 */
	public LogEntry<E> getAfter(long index) {
		return get(index+1);
	}
	
	
	/**
	 * Gets the last log entry (committed or not) of the log.
	 * 
	 * @return null if the log is empty;
	 * or the last log entry, if log is not empty.
	 * 
	 */
	public LogEntry<E> getLast() {
		return get(getLastIndex());
	}
	
	/**
	 * Gets the last committed log entry of the log.
	 * 
	 * @return null if the log is empty;
	 * or the last committed log entry, if log is not empty.
	 * 
	 */
	public LogEntry<E> getLastCommited(long index) {
		return get(getLastCommittedIndex());
	}
	
	
	/**
	 * Extract the last log entry (committed or not) of the log.
	 * 
	 * @return null if the log is empty;
	 * or log entry extracted, if log is not empty.
	 * 
	 */
	public LogEntry<E> pop() {
		LogEntry<E> aux = null;
		try {
			mapSem.acquire();
			aux = entries.remove(lastIndex);
			mapSem.release();
			
			lastIndex -= 1;
			if(lastIndex < -1) lastIndex = -1;
			
			commitSem.acquire();
			if(lastIndex < lastCommit) lastCommit = lastIndex;
			commitSem.release();
			
		} catch (InterruptedException e) {}
		
		return aux;
	}
	
	public boolean removeFrom(long index) {
		while(getLastIndex() >= index) {
			pop();
		}
		
		return true;
	}
	
	
	/**
	 * Converts the Log to a readable string.
	 *  
	 */
	public String toString() {
		String res = "Map <Long,LogEntry<E>> [";
		
		for(Long key : entries.keySet()) {
			res += " { " + key + " : " + entries.get(key) + " }, ";
		}
				
		res += "]";
		
		return res;
	}
	
	
	public int equals(long lastEntryIndex, long lastEntryTerm) {
		if(this.lastIndex > lastEntryIndex) return 1;
		if(this.lastIndex < lastEntryIndex) return -1;
		if(this.lastIndex == -1) return 0;
		
		long lastTerm = this.entries.get(this.lastIndex).getTerm();
		if(lastTerm > lastEntryTerm) return 1;
		if(lastTerm < lastEntryTerm) return -1;
		return 0;
	}
	
}
