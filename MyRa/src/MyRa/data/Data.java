package MyRa.data;


// USED by THREADS to share data, and modify its values
public class Data<E> {
	private E data;
	
	public Data(E data) {
		this.data = data;
	}
	
	
	public void setData(E data) {
		this.data = data;
	}
	
	public E getData() {
		return data;
	}
	
	
	public String toString() {
		return data.toString();
	}
}
