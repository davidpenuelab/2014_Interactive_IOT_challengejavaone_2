package com.lhings.iotchallenge;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.lhings.library.Event;
import com.lhings.library.LhingsDevice;
import com.lhings.library.Stats;

public class DTableU extends LhingsDevice {
	
        // cambiar estas keys por las correctas <-------OJO
    
	private final String availableCardKey = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private final String taxiCardKey = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	private final String coworkingApiKey = "fa953e05-cfb0-4a43-bbbf-0153c48d7824";
    
	private String user;
	private String userApikey;
	private boolean available = true;
    
	private boolean sendCheckedIn = false;
	private boolean sendCheckedOut = false;
	private boolean sendTaxiRequested = false;
	private boolean sendAvailable = false;
	private boolean sendNotAvailable = false;
    
    private boolean checkIn = false;
    
    static Thread rfid_thread;
    static RP_Rfid rfid;
    
	public DTableU() {
            // substituir las credenciales por las del coworking <------- OJO
            //		super("jose@lhings.com", "eniac", 5000, "Table");
        super("davidpenuelab@gmail.com", "fabrica", 5000, "DTable");
        
	}
    
	@Override
	public void setup() {
            // ensure availabity is in a known state
		setAvailable(true);
        setupRFID();
        
	}
    
	@Override
	public void loop() {
		try {
            updateRfid();
            
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
	}
    
    private void setupRFID() {
		rfid = new RP_Rfid();
		rfid_thread = new Thread(rfid,"RFID Table");
		rfid_thread.start();
	}
    
    private void updateRfid() {
		String apikey = rfid.getStringApiKey();
		if(apikey != null){
            if(!checkIn){
                doCheckout();
                sendCheckedOut= true;
            }else{
                if(userApikey.equals(apikey)){
                    checkIn = false;
                    sendCheckedOut= true;
                }else if (apikey.equals(taxiCardKey)){
                    requestTaxi();
                }else if(apikey.equals(availableCardKey)){
                    toggleAvailable();
                }
                
            }
            
        }
	}
    
    
    
        // todos los eventos funcionan igual, si en alguna parte del código se pone la
        // variable checkedIn (la que toque en cada caso) a true el evento se envía una vez
	@Event(name="CheckedIn")
	public String checkedIn(){
		if (sendCheckedIn){
			sendCheckedIn = false;
			return userApikey;
		} else
			return null;
	}
    
	@Event(name="CheckedOut")
	public String checkedOut(){
		if (sendCheckedOut){
			sendCheckedOut = false;
			return "checkout";
		} else
			return null;
	}
    
	@Event(name="TaxiRequested")
	public String taxiRequested(){
		if (sendTaxiRequested){
			sendTaxiRequested = false;
			return "";
		} else
			return null;
	}
    
	@Event(name="Available")
	public String available(){
		if (sendAvailable){
			sendAvailable = false;
			return "";
		} else
			return null;
	}
    
	@Event(name="NotAvailable")
	public String notAvailable(){
		if (sendNotAvailable){
			sendNotAvailable = false;
			return "";
		} else
			return null;
	}
    
    
    @Stats(name ="checkedIn", type="boolean")
    public boolean isCheckedIn(){
        return checkIn;
    }
	@Stats(name = "user", type = "string")
	public String getUser(){
		if (user == null)
			return "no user";
		else
			return user;
	}
    
	@Stats(name = "available", type = "boolean")
	public boolean isAvailable(){
		return available;
	}
	
	
    
        // ************* private methods (Lamp related) ***************
    
        // llamar este método para cambiar entre available y notAvailable
        // cada vez que se llame la bombilla hue1 debería ponerse roja
        // y apagada alternativamente (estoy ya está implementado, todavía no
        // probado. si no funcionase, ver el código en DLamp.java, que hace
        // exactamente lo mismo con la bombilla 4. si es necesario
        // ver api de hue en http://developers.meethue.com/1_lightsapi.html)
	private void toggleAvailable(){
		setAvailable(!available);
        
    }
    
        // esto de momento es dummy, que incluya aquí sergi un ejemplo de llamada a
        // web service del taxi
	private void requestTaxi(){
        System.out.println("Request a Taxi!");
	}
    
	private void doCheckin(String apikey){
        
        Map<String, String> devices = getAllDevicesInAccount(apikey);
            // check if device Lamp exists in account with apikey, create if not (descriptor will be uploaded by the device itself)
		if (devices.get("Lamp")==null){
			String uuid = createDevice(apikey, "Lamp");
			devices.put("Lamp", uuid);
		}
		
            // check if device CoffeeMaker exists in account with apikey, create if not and upload descriptor
		if (devices.get("CoffeeMaker")== null){
			String uuid = createDevice(apikey, "CoffeeMaker");
			devices.put("CoffeeMaker", uuid);
			String descriptor = "{\r\n    \"modelName\": \"Nespresso\",\r\n    \"manufacturer\": \"DeLonghi\",\r\n    \"deviceType\": \"CoffeeMaker\",\r\n    \"serialNumber\": \"SN04732\",\r\n    \"friendlyName\": \"CoffeeMaker\",\r\n    \"uuid\": \"13dfc9c7-f8f2-4cd0-9472-89c1b179144f\",\r\n    \"version\": 1,\r\n    \"stateVariableList\": [],\r\n    \"actionList\": [],\r\n    \"eventList\": [\r\n        {\r\n            \"name\": \"CheckedIn\"\r\n        }\r\n    ]\r\n}";
			uploadDescriptor(apikey, uuid, descriptor);
		}
		
            // check if device Table exists in account with apikey, create if not (descriptor will be uploaded by the device itself)
		if (devices.get("Table")== null){
			String uuid = createDevice(apikey, "Table");
			devices.put("CoffeeMaker", uuid);
		}
		
            // Start device lamp
            // first create device.properties file for it
		Properties props = new Properties();
		props.setProperty("apikey", apikey);
		props.setProperty("port", "5001");
		props.setProperty("name", "Lamp");
		try {
			props.store(new FileOutputStream("../DLamp/device.properties"), "");
		} catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
            // now device is ready to start
        try {
			Process p = Runtime.getRuntime().exec("../DLamp/DLamp.sh"); // para apagarla se hace mediante el acción shutdown de DLamp
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
		
            // se hace lo mismo con DTable, se crea otro dispositivo y se arranca de forma análoga a como
            // hemos hecho con DLamp
        
        Properties props2 = new Properties();
		props2.setProperty("apikey", apikey);
		props2.setProperty("port", "5002");
		props2.setProperty("name", "DTableU");
		try {
			props2.store(new FileOutputStream("../DTable/device.properties"), "");
		} catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
            // now device is ready to start
        try {
			Process p2 = Runtime.getRuntime().exec("../DTable/DTable.sh"); // para apagarla se hace mediante el acción shutdown de DLamp
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
        
            // se utiliza el servicio Device.startSession para poner online DCoffeMakerU
            // se utiliza el servicio Device.doAction para llamar a la acción allowUser the DCoffeeMakerC (https://github.com/lhings/documentation/wiki/Device-dot-doAction)
            // pasándole el apikey del usuario
        
        System.out.println("Checked in done with apikey+"+apikey);
        System.exit(0);
	}
    
	private void doCheckout(){
            // llamamos al shutdown de DTable y DLamp para terminarlos, ponemos offline DCoffeeMakerU
            // se utiliza el servicio Device.endSession para poner offline DCoffeMakerU
        System.out.println("checkout!");

        Properties props2 = new Properties();
		props2.setProperty("apikey", apikey);
		props2.setProperty("port", "5000");
		props2.setProperty("name", "DTable");
		try {
			props2.store(new FileOutputStream("../DTable/device.properties"), "");
		} catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
            // now device is ready to start
        try {
			Process p2 = Runtime.getRuntime().exec("../DTable/DTable.sh"); // para apagarla se hace mediante el acción shutdown de DLamp
		} catch (IOException e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
    
	private void uploadDescriptor(String apikey, String uuid, String descriptor){
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost post = new HttpPost("https://www.lhings.com/laas/api/v1/devices/" + uuid + "/");
			post.addHeader("X-Api-Key", apikey);
			StringEntity requestBody = new StringEntity(descriptor);
			post.setEntity(requestBody);
			CloseableHttpResponse response = httpclient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Unable to upload descriptor for device " + uuid + ", request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
        
	}
	
	private String createDevice(String apikey, String devName){
            //register device
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost post = new HttpPost("https://www.lhings.com/laas/api/v1/devices/");
			post.addHeader("X-Api-Key", apikey);
            post.addHeader("Content-Type", "application/json");
            
			StringEntity requestBody = new StringEntity("{ \"name\": \"deviceName\", \"value\": \"" + devName + "\"}");
			post.setEntity(requestBody);
			CloseableHttpResponse response = httpclient.execute(post);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Unable to register device Lamp, request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			JSONObject device = new JSONObject(responseBody);
			return device.getString("uuid");
			
			
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
		return null;
	}
    
	private void setAvailable(boolean available) {
		
		if (this.available == false && available == true){
			callWebService("{\"on\":false}");
			sendAvailable = true;
		}
		else if (this.available == true && available == false){
			callWebService("{\"on\":true, \"hue\":0}");
			sendNotAvailable = true;
		}
        
        
		this.available = available;
        
        if(this.available)
            System.out.println("I am Available right now");
        else
            System.out.println("I am NOT Available right now");
	}
    
	private void callWebService(String payload) {
		
		try {
			URL hueColorService = new URL(
                                          "http://192.168.1.129/api/jfokususer/lights/1/state");
			HttpURLConnection conn = (HttpURLConnection) hueColorService.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("PUT");
			conn.setRequestProperty("Content-Type", "application/json");
            
			String input = payload;
            
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();
            
			BufferedReader br = new BufferedReader(new InputStreamReader(
                                                                         (conn.getInputStream())));
            
			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				System.out.println(output);
			}
            
			conn.disconnect();
		} catch (Exception e) {
                // TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
	private float temperatureToPercentage(float x) {
		float in_min=30;
		float in_max=45;
		float out_min=0;
		float out_max=100;
		
		if (x<in_min)
			return out_min;
		if (x>in_max)
			return out_max;
		
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}
    
    
	public static void main(String[] args) {
		@SuppressWarnings("unused")
            // starting your device is as easy as creating an instance!!
		DTableU table = new DTableU();
        
	}
    
	private Map<String,String> getAllDevicesInAccount(String apikey) {
		
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet get = new HttpGet("https://www.lhings.com/laas/api/v1/devices/");
			get.addHeader("X-Api-Key", apikey);
			CloseableHttpResponse response = httpclient.execute(get);
			if (response.getStatusLine().getStatusCode() != 200) {
				System.err.println("Device.list request failed: " + response.getStatusLine());
				response.close();
				System.exit(1);
			}
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();
			JSONArray listDevices = new JSONArray(responseBody);
			int numElements = listDevices.length();
			Map<String,String> results = new HashMap<String, String>();
			for (int i = 0; i<numElements; i++){
				JSONObject device = listDevices.getJSONObject(i);
				String uuid = device.getString("uuid");
				String name = device.getString("name");
				results.put(name, uuid);
			}
			
			return results;
		} catch (IOException ex) {
			ex.printStackTrace(System.err);
			System.exit(1);
		}
		return null;
	} 
    
}
