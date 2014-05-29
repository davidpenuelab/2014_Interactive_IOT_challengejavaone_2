package com.lhings.iotchallenge;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

import java.net.URL;
import java.net.URI;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.entity.UrlEncodedFormEntity;


import org.json.JSONArray;
import org.json.JSONObject;

import com.lhings.library.Action;
import com.lhings.library.Stats;
import com.lhings.library.Event;
import com.lhings.library.LhingsDevice;
import com.lhings.library.Payload;


public class DTable extends LhingsDevice {
	
    
	private final String availableCardKey = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	private final String taxiCardKey = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	private final String coworkingApiKey = "1314064a-32f5-41dd-bf27-7469d3ae9dd0";

	private String user;
	private String userApikey;

	private boolean sendCheckedIn = false;
	private boolean sendCheckedOut = false;
	private boolean sendTaxiRequested = false;
	private boolean sendAvailable = false;
	private boolean sendNotAvailable = false;

    private boolean checkIn = false;
    
    static Thread rfid_thread;
    static RP_Rfid rfid;
    
    private boolean on=false;
	private boolean available = false;
    Map<String,String> devicesUser;
    Map<String,String> devicesCoworking;
    
	public DTable() {
		// Co-working space credenti
        super("david@lhings.com", "coworking", 5000, "DTable");

	}

	@Override
	public void setup() {
        //First, setup the RFID
        setupRFID();
        System.setProperty("jsse.enableSNIExtension", "false");

	}

	@Override
	public void loop() {
		try {
            //Since we created a thread for the RFID, we check if the credentials are corret/not null in the update
            updateRfid();
            //give just a bit of time to breath
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

    private void setupRFID() {
        //Created a thread so its not blocking the app the RFID
		rfid = new RP_Rfid();
		rfid_thread = new Thread(rfid,"RFID Table");
		rfid_thread.start();
	}
    
    private void updateRfid() {
		String apikey = rfid.getStringApiKey();
		if(apikey != null){
            if(!checkIn){
                if (apikey.equals(taxiCardKey)){
                    return;
                }else if(apikey.equals(availableCardKey)){
                    return;
                }
                
                doCheckin(apikey);
                userApikey = apikey;
                checkIn= true;
                sendCheckedIn = true;
            }else{
                if(userApikey.equals(apikey)){
                    doCheckout(apikey);
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

    // ************************************
    // ************* STATUS ***************
    // ************************************
    @Stats(name ="checkedIn", type="boolean")
    public boolean isCheckedIn(){
        return checkIn;
    }

	@Stats(name = "availability", type = "boolean")
	public boolean isAvailable(){
		return available;
	}
	
	@Stats(name = "light", type = "boolean")
	public boolean lightStatus(){
		return on;
	}

	// ************************************
    // ************* ACTIONS **************
    // ************************************

    @Action (name = "toggleAvailable", description = "Toggle between green/red light of availability of user", argumentNames = {}, argumentTypes = {})
	public void toggleAvailable(){
		setAvailable(!available);
    }

    @Action (name = "toggleLight", description = "Toggle light on/off ", argumentNames = {}, argumentTypes = {})
    public void toggleLight(){
        setLightOn(!on);
    }
    @Action (name = "requestTaxi", description = "Requests a taxi", argumentNames = {}, argumentTypes = {})
	public void requestTaxi(){
        System.out.println("TODO: Request a Taxi!");
	}

    // ************************************
    // ************* OTHER METHODS ********
    // ************************************

	private void doCheckin(String apikey){
        setLightOn(true);
        setAvailable(true);
        getDevicesFromUser(apikey);//and send welcome, send to desktop app apikey
        getDevicesFromCoworking(apikey);
	}
    
    private void doCheckout(String apikey){
            // llamamos al shutdown de DTable y DLamp para terminarlos, ponemos offline DCoffeeMakerU
            // se utiliza el servicio Device.endSession para poner offline DCoffeMakerU
        setLightOn(false);
        setAvailable(false);
        setAvailableOFF();
        sendMessageLhings(apikey,devicesUser.get("PlugLhings"), "CIAO! See you soon in our Co-working space!");
        System.out.println("checkout!");
	}

    private void setLightOn(boolean value){
        if (this.on == false && value == true){
			callWebService_lightOnOff("{\"on\":true}");
		}
		else if (this.on == true && value == false){
			callWebService_lightOnOff("{\"on\":false}");
		}
        
		this.on = value;
        
        if(this.on)
            System.out.println("Light ON");
        else
            System.out.println("Light OFF");
    }
    
	private void setAvailable(boolean available) {
		if (this.available == false && available == true){
			callWebService_available("{\"on\":true, \"hue\":25000}");//green {"on":true, "hue": 25000}
			sendAvailable = true;
		}
		else if (this.available == true && available == false){
			callWebService_available("{\"on\":true, \"hue\":1000}");//red {"on":true, "hue": 1000}
			sendNotAvailable = true;
		}
        
		this.available = available;
        
        if(this.available)
            System.out.println("I am Available right now");
        else
            System.out.println("I am NOT Available right now");
	}

    private void setAvailableOFF() {
        callWebService_available("{\"on\":false}");
        System.out.println("Turning off Available light");
	}

    private void getDevicesFromUser(String apikey){
        devicesUser = getAllDevicesInAccount(apikey);
        String uuidPlugLhings = devicesUser.get("PlugLhings");
        sendMessageLhings(apikey, uuidPlugLhings, "Welcome to the Co-working space");
    }
    private void getDevicesFromCoworking(String apikeyUser){
        devicesCoworking = getAllDevicesInAccount(coworkingApiKey);
        String uuidDCoffeeMaker = devicesCoworking.get("CoffeeMaker");
        System.out.println("DEVICES FROM COWORKING"+devicesCoworking.toString());
        sendApikeyToCoffee(apikeyUser, uuidDCoffeeMaker);
    }
    
    private void sendMessageLhings(String apikey, String uuid, String message){
        CloseableHttpClient httpClient=null;
        CloseableHttpResponse response=null;
        try {
            
            httpClient = HttpClients.createDefault();
            URI uri = new URI("https://www.lhings.com/laas/api/v1/devices/"+uuid+"/actions/notifications");
            HttpPost httpPost = new HttpPost(uri);
            
            httpPost.setHeader("X-Api-Key", apikey);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            String json="[{ \"name\": \"text\", \"value\": \""+message+"\"}]";
            HttpEntity postBody = new StringEntity(json);
            httpPost.setEntity(postBody);
            
            response = httpClient.execute(httpPost);
            
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                return;
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(
                                                                         (response.getEntity().getContent())));
            
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
        } catch (IOException | URISyntaxException e) {
            System.out.println("Error "+e.toString());
        } finally {
            try{
                if(response!=null){
                    response.close();
                }
                if(httpClient!=null){
                    httpClient.close();
                }
            }catch(IOException ex) {
                System.out.println("Finally Error "+ex.toString());
            }
        }

    }

    private void sendApikeyToCoffee(String apikeyUser, String uuidCoffeMaker){
        System.out.println("DOING: Send Apikey of user to coffeemaker");
        System.out.println("Apikey Coworking->"+coworkingApiKey);
        System.out.println("Apikey User->"+apikeyUser);
        System.out.println("Apikey UUID Coffeemaker->"+uuidCoffeMaker);
        CloseableHttpClient httpClient=null;
        CloseableHttpResponse response=null;
        try {

            httpClient = HttpClients.createDefault();
            URI uri = new URI("https://www.lhings.com/laas/api/v1/devices/"+uuidCoffeMaker+"/actions/AllowUser");
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setHeader("X-Api-Key", coworkingApiKey);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            String json="[{ \"name\": \"apikey\", \"value\": \""+apikeyUser+"\"}]";
            HttpEntity postBody = new StringEntity(json);
            httpPost.setEntity(postBody);

            response = httpClient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
            (response.getEntity().getContent())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
        }   catch (IOException | URISyntaxException e) {
            System.out.println("Error "+e.toString());
        } finally {
            try{
                if(response!=null){
                    response.close();
                }
                if(httpClient!=null){
                    httpClient.close();
                }
            }catch(IOException ex) {
                System.out.println("Finally Error "+ex.toString());
            }
        }
    }

    private void sendGoodByeMessage(String apikey){
        System.out.println("TODO: Send goodby to Pereda and Lhings");
    }

    /*
     TODO: send message to pereda of welcome and goodbye
     send confirmation message when action is done from pereda
     send api key to coffeemaker
     */
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

    // ************* private methods (Lamp related) ***************
	private void callWebService_available(String payload) {
		try {
			URL hueColorService = new URL("http://192.168.0.111/api/newdeveloper/lights/4/state");
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
			e.printStackTrace();
		}
	} 

    private void callWebService_lightOnOff(String payload) {
		try {
			URL hueColorService = new URL("http://192.168.0.111/api/newdeveloper/lights/3/state");
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
			e.printStackTrace();
		}
	} 
    
    // ************************************
    // ************* MAIN *****************
    // ************************************
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		// starting your device is as easy as creating an instance!!
//        System.setProperty("jsse.enableSNIExtension", "false");
		DTable table = new DTable();

	}

}
