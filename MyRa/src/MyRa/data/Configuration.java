package MyRa.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONObject;

import utils.generator.Random;

public class Configuration {
	private String configurationFile = "./MyRA.conf";
	
	public Configuration() {
		nodesSockets = new HashMap<>();
		loadConfiguration();
	}
	
	// Custom configuration ---> Maybe received by argument in the main
	public Configuration(String configurationFile) {
		this.configurationFile = configurationFile;
		nodesSockets = new HashMap<>();
		loadConfiguration();
	}
			
	
	public void saveConfiguration() {
		return;
	}
	
	public void loadConfiguration() {		
		String rawData = "";
		
		try {
			FileReader file;
			BufferedReader buff;
			
			file = new FileReader(configurationFile);
			buff = new BufferedReader(file);
			
			if(buff != null) {
				int c;
				while((c = buff.read()) != -1) {
					rawData += (char)c;
				}
				
				buff.close();
				file.close();
			}
			
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
			return;
					
		} catch (IOException e) {
			// e.printStackTrace();
			return;
		}
		
		JSONObject json = new JSONObject(rawData);
		
		if(json.has("me")) this.serverID = new ServerID(json.getString("me"));
		
		
		if(json.has("cluster")) {
			JSONObject clusterJSON  = json.getJSONObject("cluster");
			
			for(String id : clusterJSON.keySet()) {
				JSONObject aux = clusterJSON.getJSONObject(id);
				ServerID auxID = new ServerID(id);
				MyRASocket auxSocket = new MyRASocket(aux.getString("IPv4Address"), aux.getInt("Port"));
				this.nodesSockets.put(auxID, auxSocket);
				if(this.serverID.equals(auxID)) this.socket = auxSocket;
			}
			
		}
		
		 
		 
		 
		 if(json.has("MIN_HEARTBEAT"))	this.MIN_HEARTBEAT = json.getLong("MIN_HEARTBEAT");
		 if(json.has("MAX_HEARTBEAT"))	this.MAX_HEARTBEAT = json.getLong("MAX_HEARTBEAT");
		 if(json.has("SEND_HEARTBEAT"))	this.SEND_HEARTBEAT = json.getLong("SEND_HEARTBEAT");
	
		 
		 return;
	}
	
	
	
	public String toString() {
		String res = "";
		res += " //////// \\\\\\\\\\\\\\\\ " + "\n";
		res += "// Configuration \\\\" + "\n";
		res += "\\\\\\\\\\\\\\\\\\ /////////" + "\n";
		res += "\n";
		
		res += " -> My server id: " + serverID + "\n";
		res += " -> My socket: " + socket + "\n";
		
		res += " -> Cluster: {" + "\n";
		
		int i = 0;
		for(Map.Entry<ServerID,MyRASocket> entry : nodesSockets.entrySet()) {
			res += "\t\t [" + (++i) + "] " + entry.getKey() + ": " + entry.getValue() + "\n";
		}
		
		res += "    }" + "\n";
		
		res += " -> MIN HEARTBEAT: " + MIN_HEARTBEAT + "\n";
		res += " -> MAX HEARTBEAT: " + MAX_HEARTBEAT + "\n";
		res += " -> SEND HEARTBEAT: " + SEND_HEARTBEAT + "\n";
		
		
		// res += " -> HEARTBEAT_TIME: " + HEARTBEAT_TIME + "\n";
		res += " -> LEADER_ELECTION_TIME: " + LEADER_ELECTION_TIME + "\n";
		
		return res;
	}
	
	
	
	// public long HEARTBEAT_TIME = 30 * 1000;	// 30 segs = 30 000 milliseconds -> incluir en la configuracion
	public long LEADER_ELECTION_TIME = 30 * 1000;	// 30 segs = 30 000 milliseconds -> incluir en la configuracion
	
	// My own data
	public ServerID serverID;
	public MyRASocket socket;
	
	public long MIN_HEARTBEAT;
	public long MAX_HEARTBEAT;
	public long SEND_HEARTBEAT;
	
	public HashMap<ServerID,MyRASocket> nodesSockets;	// includes itself -> SO CAUTION NO MAKE request to your self
	
	public MyRASocket getSocket(ServerID id) {
		return nodesSockets.get(id);
	}
}
