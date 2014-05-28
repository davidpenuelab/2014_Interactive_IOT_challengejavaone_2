package com.lhings.iotchallenge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.lhings.library.Action;
import com.lhings.library.Stats;
import com.lhings.library.Event;
import com.lhings.library.LhingsDevice;
import com.lhings.library.Payload;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class DTable extends LhingsDevice {

	
	private String user;
	private boolean available = true;

	//final GpioController gpio = GpioFactory.getInstance();
	//GpioPinDigitalOutput relay;

    static Thread rfid_thread;
    static RP_Rfid rfid;

    
	public DTable() {
        // substituir credenciales con las del coworking antes de enviar a JavaOne
 	   	super("davidpenuelab@gmail.com", "fabrica", 5000, "DTable");
	}

	@Override
	public void setup() {
		setupRFID();
		
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loop() {
		try {
            updateRfid();
            System.out.println("update");
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
			if(isAllowed(apikey)){
				System.out.println("User with apikey " + apikey + " is allowed to make coffee");
				lastTimeChecked = System.currentTimeMillis();
			}else{
				System.out.println("User with apikey " + apikey + " is NOT allowed to make coffee");
			}
		}
	}

    /*********************************************/
    /******** Lhings EVENTS section **************/
    /*********************************************/

	@Event(name="CheckedIn")
	public String checkedIn(){
		return null;
	}

	@Event(name="CheckedOut")
	public String checkedOut(){
		return null;
	}

	@Event(name="TaxiRequested")
	public String taxiRequested(){
		return null;
	}

	@Event(name="TaxiRequestedFailure")
	public String taxiRequestedFailure(){
		return null;
	}

	@Event(name="TaxiRequestedSuccess")
	public String taxiRequestedSuccess(){
		return null;
	}

	@Event(name="Available")
	public String available(){
		return null;
	}

	@Event(name="NotAvailable")
	public String notAvailable(){
		return null;
	}


    /*********************************************/
    /******** Lhings STATS section **************/
    /*********************************************/

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

	private void callWebService(float temperature) {
		float percentage = temperatureToPercentage(temperature);
		System.out.println("Temperature " + temperature + " is " + percentage + "%");
		percentage = percentage / 100;
		float x = percentage * 0.51f + 0.16f;
		float y = percentage * 0.27f + 0.05f;
		String payload = "{\"xy\":[" + x + ", " + y + "]}";
		callWebService(payload);
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
		DTable table = new DTable();

	}

}
