package pt.unparallel.iot_catalogue.connector;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.keysolutions.ddpclient.DDPClient;
import com.keysolutions.ddpclient.DDPClient.CONNSTATE;
import com.keysolutions.ddpclient.DDPClient.DdpMessageField;
import com.keysolutions.ddpclient.DDPListener;


/**
 * Connection between IoT Catalogue and an external app
 *
 * @author antoniogoncalves
 *
 */

public class Connector{


	/**
	 *
	 * @param socketAddress Web socket address of the instance where IoT Catalogue is running
	 * @param token Used to authenticate a user on IoT Catalogue
	 * @throws MalformedURLException
	 * @throws NoSuchAlgorithmException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	public Connector(String socketAddress, String token) throws MalformedURLException, NoSuchAlgorithmException, URISyntaxException, InterruptedException {
		this(socketAddress, token, null, null);
	}

	/**
	 *
	 * @param socketAddress Web socket address of the instance where IoT Catalogue is running
	 * @param token Used to authenticate a user on IoT Catalogue
	 * @param serviceDescription Object describing the features of the external service
	 * @throws MalformedURLException
	 * @throws NoSuchAlgorithmException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 */
	public Connector(String socketAddress, String token, Object serviceDescription) throws MalformedURLException, NoSuchAlgorithmException, URISyntaxException, InterruptedException {
		this(socketAddress, token, serviceDescription, null);
	}

	/**
	 *
	 * @param socketAddress Web socket address of the instance where IoT Catalogue is running
	 * @param token Used to authenticate a user on IoT Catalogue
	 * @param serviceDescription Object describing the features of the external service
	 * @param connectionProps Props related with data subscription
	 * @throws MalformedURLException
	 * @throws URISyntaxException
	 * @throws InterruptedException
	 * @throws NoSuchAlgorithmException
	 */
	public Connector(String socketAddress, String token, Object serviceDescription, Object connectionProps) throws MalformedURLException, URISyntaxException, InterruptedException, NoSuchAlgorithmException {

		// TODO Auto-generated constructor stub
		this.socketAddress = socketAddress;
		this.token = token;
		this.serviceDescription = serviceDescription;
		this.connectionProps = connectionProps;
		url = new URL(socketAddress);
		String host = url.getHost();
		Integer port = getPortFromURL();

		boolean useSSL = url.getProtocol().equals("https")?true:false;
		ddpClient = new DDPClient(host, port, useSSL);

		connect();
		addObserver();



	}

	/**
	 *
	 * Returns DDPClient instance, allowing external apps to use "call" and "subscribe" methods
	 *
	 * @return DDPClient instance
	 */
	public DDPClient getDDPClientInstance(){
		return ddpClient;
	}

	/**
	 * Used to fix a malformed id coming from IoT Catalogue
	 *
	 * @param id
	 * @return Fixed id
	 */
	private static String fixId(String id) {
		if(id.startsWith("-") && id.length() == 25) {
			String trimmedString = id.substring(1);
			if(Pattern.matches("[0-9A-Fa-f]{24}",trimmedString)) {
				return trimmedString;
			}
		}

		return id;
	}


	/**
	 *
	 * Get port from URL
	 *
	 * @return Port used on connection with IoT Catalogue
	 */
	private Integer getPortFromURL() {
		Integer port = url.getPort();
		if(port < 0) {
			port = url.getProtocol().equals("https")?443:80;
		}
		return port;
	}


	/**
	 * Connects to IoT Catalogue
	 */
	public void connect() {
		logger.info("Connecting to " + this.socketAddress);
		ddpClient.connect();
		addScheduler();
	}


	/**
	 * Terminates the connection with IoT Catalogue
	 */
	public void disconnect() {
		timer.cancel();
		ddpClient.disconnect();
	}


	/**
	 * Hash user token using SHA-256
	 *
	 * @return Hashed user token
	 * @throws NoSuchAlgorithmException
	 */
	private String hashUserToken() throws NoSuchAlgorithmException {

		//Connector connection = new Connector("https://127.0.0.1:3000");
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] encodedhash = digest.digest(this.token.getBytes(StandardCharsets.UTF_8));

		byte[] encodedBytes = Base64.getEncoder().encode(encodedhash);
		return new String(encodedBytes);
	}



	/**
	 *
	 * Logs in on IoT Catalogue using user token
	 *
	 * @throws NoSuchAlgorithmException
	 */
	private void login() throws NoSuchAlgorithmException {
		Connector that = this;
		Object[] params = new Object[1];
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		hashMap.put("userToken",getUserToken());
		params[0] = hashMap;
		ddpClient.call("login", params, new DDPListener() {
			@Override
			public void onResult(Map<String, Object> resultFields) {
				if (resultFields.containsKey(DdpMessageField.ERROR)) {
					Map<String, Object> error = (Map<String, Object>) resultFields.get(DdpMessageField.ERROR);
					String errorReason = (String) error.get("reason");
					logger.log(Level.SEVERE, errorReason);
					that.disconnect();
				} else {
					logger.info("loggedIn");
					getConnectionService();
				}
			}
		});
	}


	/**
	 *
	 * Obtains the service props based on app description
	 *
	 */
	private void getConnectionService() {
		Object[] params = new Object[] {serviceDescription};

		ddpClient.call("getConnectionService", params, new DDPListener() {
			@Override
			public void onResult(Map<String, Object> resultFields) {
				Map<String, Object> hashMap = (Map<String, Object>)resultFields.get("result");
				Boolean serviceFound =(Boolean) hashMap.get("serviceFound");
				Object props = hashMap.get("props");
				String name = (String) hashMap.get("name");
				if(serviceFound) {
					onSubscribedToService(name, props);
				}
				subscribeToExternalServiceCommunication();
				subscribeToData();
			}
		}
		);
	}


	/**
	 * Subscribes to service actions coming from IoT Catalogue
	 */
	private void subscribeToExternalServiceCommunication() {
		ddpClient.subscribe("subscribeToExternalServiceCommunication", null,new DDPListener() {
			@Override
			public void onReady(String callId) {
				logger.info("subscribed to service actions");
			}
		});
	}

	/**
	 * Subscribes to user data
	 */
	private void subscribeToData() {
		ddpClient.call("getUserDataCollectionNames", null, new DDPListener() {
			@Override
			public void onResult(Map<String, Object> resultFields) {
				ArrayList<String> collectionNames =(ArrayList<String>) resultFields.get("result");
				for(String collectionName: collectionNames) {
					subscribeToCollection(collectionName);
				}
			}
		});
	}


	/**
	 * Subscribe to a single collection
	 *
	 * @param collectionName
	 */
	private void subscribeToCollection(String collectionName) {
		
		Object fields = null;
		
		if(connectionProps != null) {
			HashMap<String, Object>  props = (HashMap<String, Object> )connectionProps;
			if(props != null) {
				fields = props.get("dataFields");
				if(fields == null) {
					fields = props.get("fields");
				}
			}
			
		}	


		HashMap<String, Object> propsToMethod = new HashMap<String, Object>();

		propsToMethod.put("fields",fields);

		Object[] params = new Object[] {collectionName, propsToMethod};
		ddpClient.subscribe("subscribeToServiceData", params,new DDPListener() {
			@Override
			public void onReady(String callId) {
				logger.info("subsribed to collection " + collectionName);
			}
		});
	}



	/**
	 *
	 * Get hashed user token object
	 *
	 * @return Returns hashed user token object
	 * @throws NoSuchAlgorithmException
	 */
	private HashMap<String,String> getUserToken() throws NoSuchAlgorithmException{
		HashMap<String,String> hashMap = new HashMap<String,String>();
		hashMap.put("digest", hashUserToken());
		hashMap.put("algorithm", "sha-256");
		hashMap.put("encoding", "base64");
		return hashMap;
	}


	/**
	 *
	 * Add connector observer
	 *
	 */
	private void addObserver() {
		observer = new ConnectorObserver() {

			@Override
			public void onConnectedFromRemote() {
				// TODO Auto-generated method stub
				onConnected();
				try {
					login();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			@Override
			public void onDisconnectedFromRemote() {
				// TODO Auto-generated method stub
				onDisconnected();
			}
			@Override
			public void onCollectionUpdate(String collectionName, String actionName, String id, Object obj) {
				if(collectionName.equals(actionsCollectionName) && actionName.equals("added")){
					String state =( (Map<String, String>) obj).get("state");
					if(state!=null && state.equals("added")) {
						Action action = new Action(ddpClient,fixId(id), obj);
						onActionAdded(action);
					}


				}
				if(!collectionName.equals(actionsCollectionName)) {
					onDataChange(collectionName, actionName, fixId(id), obj);
				}

			}

		};
		ddpClient.addObserver(observer);
	}



	/**
	 *
	 * Method called when subscribed to a service
	 *
	 * @param name Name of the service
	 * @param props Properties of the service
	 */
	public void onSubscribedToService(String name, Object props) {
	}


	/**
	 *
	 * Method called during a data update from IoT Catalogue
	 *
	 * @param collectionName Name of the collection
	 * @param actionName Name of the action added changed or removed
	 * @param id Id of the element
	 * @param obj Info of the element
	 */
	public void onDataChange(String collectionName, String actionName, String id, Object obj) {

	}

	/**
	 *
	 * Method called when an action is added by IoT Catalogue
	 *
	 * @param action
	 */
	public void onActionAdded(Action action) {

	}


	/**
	 * Method called when a connection is established
	 */
	public void onConnected() {

	}

	/**
	 * Method called when a connection is terminated
	 */
	public void onDisconnected() {

	}

	/**
	 * Added scheduler required to check the connection
	 */
	private void addScheduler(){
		timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask(){

		    @Override
		    public void run(){

		    	CONNSTATE state = ddpClient.getState();

		    	if(CONNSTATE.Closed ==state ) {
		    		ddpClient.connect();
		    	}


		    }
		},0,reconnectPingInterval);
	}

	/**
	 * URL Object created through the socket address
	 */
	private URL url;

	/**
	 * DDPClient instance
	 */
	private DDPClient ddpClient;

	/**
	 * Web socket address of the instance where IoT Catalogue is running
	 */
	private String socketAddress;

	/**
	 * Used to authenticate a user on IoT Catalogue
	 */
	private String token;

	/**
	 * serviceDescription Object describing the features of the external service
	 */
	private Object serviceDescription;


	/**
	 *
	 * connectionProps Props related with data subscription
	 */
	private Object connectionProps;

	/**
	 * Observer which react to remote updates
	 */
	private ConnectorObserver observer;

	/**
	 * Logger used by the Connector
	 */
	private static final Logger logger = Logger.getLogger(Connector.class.getName());

	/**
	 * Ping interval (ms)
	 */
	private static final int reconnectPingInterval = 2000;

	/**
	 * Timer used by the scheduler
	 */
	private Timer timer;
	
	private static final String actionsCollectionName = "externalServiceCommunication";




}