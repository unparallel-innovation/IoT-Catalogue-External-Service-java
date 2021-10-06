package pt.unparallel.iot_catalogue.connector;

import com.keysolutions.ddpclient.DDPClient;

/**
 * 
 * Action added to the queue by IoT Catalogue
 * 
 * @author antoniogoncalves
 *
 */
public class Action {

	/**
	 * Action instance
	 * 
	 * @param ddpClient DDPClient instance
	 * @param queueId Id of the queue instance
	 * @param value Of the actions
	 */
	public Action( DDPClient ddpClient, String queueId, Object value) {

		this.ddpClient = ddpClient;
		this.queueId = queueId;
		this.value = value;
	}
	
	
	/**
	 * Returns queue id
	 * 
	 * @return Queue id
	 */
	public String getQueueId() {
		return queueId;
	}
	
	/**
	 * 
	 * Gets queue value
	 * 
	 * @return Object with queue object
	 */
	public Object getValue() {
		return value;
	}
	
	/**
	 * 
	 * Method used to reply to IoT Catalogue with action result
	 * 
	 * @param result This is the result available on the queue after the external app processing the action
	 * @param error Error happened during processing the action
	 */
	public void reply(Object result, Object error) {
	
		Object[] params = new Object[] {queueId, result, error};
		ddpClient.call("actionCallback", params);
		
	}

	/**
	 * 
	 * Method used to reply to IoT Catalogue with action result
	 * 
	 * @param result This is the result available on the queue after the external app processing the action
	 */
	public void reply(Object result) {
		reply(result, null);
	}
	
	
	/**
	 * 
	 * DDPClient instance
	 * 
	 */
	private final DDPClient ddpClient;
	
	/**
	 * Queue id
	 */
	private final String queueId;
	
	/**
	 * 
	 * queue value
	 * 
	 */
	private final Object value;
}