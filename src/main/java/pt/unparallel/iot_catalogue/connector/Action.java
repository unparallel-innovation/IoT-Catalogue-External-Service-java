package pt.unparallel.iot_catalogue.connector;

import com.keysolutions.ddpclient.DDPClient;

/**
 * Action added by IoT Catalogue
 * @author antoniogoncalves
 *
 */
public class Action {


	/**
	 * 
	 * @param ddpClient ddpClient DDPClient instance
	 * @param id of the action instance
	 * @param value Of the actions
	 */
	
	public Action( DDPClient ddpClient, String id, Object value) {

		this.ddpClient = ddpClient;
		this.id = id;
		this.value = value;
	}
	
	
	/**
	 * Returns id
	 * @return
	 */
	public String getId() {
		return id;
	}
	

	
	/**
	 * Gets the action value
	 * 
	 * @return Object with action value
	 */
	public Object getValue() {
		return value;
	}
	

	/**
	 * 
	 * Method used to reply to IoT Catalogue with action result
	 * 
	 * @param result This is the result available after processing the action
	 * @param error Error happened during processing the action
	 */
	public void reply(Object result, Object error) {
	
		Object[] params = new Object[] {id, result, error};
		ddpClient.call("actionCallback", params);
		
	}
	
	/**
	 * Method used to reply to IoT Catalogue with action result
	 * @param result This is the result available after processing the action
	 */
	public void reply(Object result) {
		reply(result, null);
	}
	
	

	private final DDPClient ddpClient;

	private final String id;
	

	private final Object value;
}