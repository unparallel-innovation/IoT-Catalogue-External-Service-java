package pt.unparallel.iot_catalogue.connector;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * It reacts with remote updates from IoT Catalogue
 * 
 * @author antoniogoncalves
 *
 */
public class ConnectorObserver implements Observer {

	/**
	 * Generic update method
	 * 
	 * @param o the observable object.
	 * @param arg an argument passed to the notifyObservers method.
	 */
	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		String msg =  (String) ((Map) arg).get("msg");
		if(msg.equals("connected")) {
			connected = true;
			onConnectedFromRemote();

		}
		if(msg.equals("closed")) {				
			if(connected) {
				onDisconnectedFromRemote();						
				connected = false;
			}					
		}
		if(msg.equals("added") || msg.equals("changed") || msg.equals("removed")) {
			String collectionName =  (String) ((Map) arg).get("collection");
			String id =  ((String) ((Map) arg).get("id"));
			
			Object fields =  (Object) ((Map) arg).get("fields");
			onCollectionUpdate(collectionName, msg, id, fields);
		}
		if(msg.equals("error")) {
			logger.log(Level.INFO,arg.toString());
		}

		
	}

	
	/**
	 * Method called when a connection is established
	 */
	public void onConnectedFromRemote() {
		
	}
	
	/**
	 * Method called when a connection is terminated	
	 */
	public void onDisconnectedFromRemote() {
		
	}
	
	/**
	 * Method called during a collection update from IoT Catalogue
	 * 
	 * @param collectionName Collection name
	 * @param action added, changed or removed
	 * @param id of the element added
	 * @param obj element added
	 */	
	public void onCollectionUpdate(String collectionName, String action, String id, Object obj) {
		
	}
	
	/**
	 * 
	 * Variable to control if the connections is established
	 * 
	 */
	private Boolean connected = false;
	
	/**
	 * Logger of this class
	 */
	private static final Logger logger = Logger.getLogger(ConnectorObserver.class.getName());
}