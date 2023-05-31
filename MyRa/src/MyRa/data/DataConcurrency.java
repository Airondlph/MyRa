package MyRa.data;

import java.util.concurrent.Semaphore;

/**
 * Asegura que no se sobreescribe la referencia a la vez, no que no se modifica el objeto a la vez.
 * @author airon
 *
 * @param <E>
 */
public class DataConcurrency<E> {
	private Semaphore sem;
	private E data;
	
	
	public DataConcurrency(E data, int permits) {
		this.sem = new Semaphore(permits);
		this.data = data;
	}
	
	
	public boolean setData(E data) {
		try {
			sem.acquire();
			this.data = data;
			sem.release();
			
		} catch (InterruptedException e) {
			return false;
		}
		
		return true;
	}
	
	public final E getData() {
		return data;
	}
	
	
	public String toString() {
		return data.toString();
	}
}
