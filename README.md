
# IoT Catalogue external service Java

Allows the communication between a Java application and IoT Catalogue throgh a queue and data subscription.

## Connection

Connection instance between a Java application and IoT Catalogue, it receives the following parameters

* **socketAddress:** URL of IoT Catalogue instance
* **token:** Token used of user authentication
* **serviceDescription (optional):** Object describing what the external service can offer
* **connectionProps (optional):** Properties related with the connection, currently suported options:
	* **dataFields:** Which data fields must be returned from a data subscription

## Examples

### PDF Exporter Service

```java
String analyseRepositoryToken = "xxx";
HashMap<String, String> serviceDescription = new HashMap<String, String>();
serviceDescription.put("repositoryType","github");

Connector connector = new Connector("https://www.iot-catalogue.com",analyseRepositoryToken, serviceDescription){
	@Override
	public void onSubscribedToService(String name, Object props) {
		System.out.println(name);  //analyse github
		System.out.println(props); //{repositoryType=github}
	};

	@Override
	public void onActionAdded(Action action) {
		// TODO Auto-generated method stub
		System.out.println(action.getValue());
		//{action=analyseRepository, props={id=5c9e411abc89249a12102199, url= https://github.com/waveshare/LCD-show, repositoryType=github}, state=added}
		action.reply("some reply"); // Reply to the action
	}

```

### Data Subscription

```java
String userWithDataAccess = "xxx";
HashMap<String, Integer> fields = new HashMap<String, Integer>();
fields.put("name",1);
HashMap<String, HashMap<String, Integer> > connectionProps = new HashMap<String, HashMap<String, Integer> >();
connectionProps.put("dataFields", fields);
Connector connector = new Connector("https://www.iot-catalogue.com",userWithDataAccess, null, connectionProps){
	@Override
	public void onDataChange(String collectionName, String action, String id, Object obj){
		System.out.println(collectionName); //validations
		System.out.println(action); //added
		System.out.println(id); //xyz
		System.out.println(obj); //{name=XXX}
	}
}
```
