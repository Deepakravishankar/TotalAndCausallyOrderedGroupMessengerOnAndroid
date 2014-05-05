package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author Deepak Ravishankar Ramkumar
 *
 */
public class GroupMessengerActivity extends Activity {
	static final String TAG=GroupMessengerActivity.class.getSimpleName();
	static final String PORT_ARRAY[]=new String []{"11108","11112","11116","11120","11124"};
	volatile HashMap<String,String> keyValuePair= new HashMap<String,String>();
	volatile HashMap<String,String> buffer= new HashMap<String,String>();
	static final int SERVER_PORT=10000;
	static String curravd=null;
	volatile int groupSequence=0;
	volatile int localSequence=0;
	volatile String gSequence=null;
	volatile int i=0;
    private  Uri mUri;

 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
        /*
		 * Calculate the port number that this AVD listens on. It is just a hack
		 * that I came up with to get around the networking limitations of AVDs.
		 * The explanation is provided in the PA1 spec.
		 */
	TelephonyManager tel = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);
		final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		if(portStr.equals("5554"))
			curravd="avd0";
		else if(portStr.equals("5556"))
			curravd="avd1";
		else if(portStr.equals("5558"))
			curravd="avd2";
		else if(portStr.equals("5560"))
			curravd="avd3";
		else if(portStr.equals("5562"))
			curravd="avd4";
		try{
			ServerSocket serverSocket=new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,serverSocket);
		}catch(IOException E){
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
       
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
       final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs in a total-causal order.
         */
        final EditText edittext= (EditText)findViewById(R.id.editText1);
        final Button button=(Button) findViewById(R.id.button4);
        button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
				String msg=edittext.getText().toString();
				edittext.setText("");
				msg=msg.trim();
				msg=msg+"_"+curravd;
				if(!msg.equals(null))
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,msg,myPort);	
				}catch(NullPointerException E){
					Log.e(TAG,"NullPointerException");
				}
			}
		});
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
    private class ServerTask extends AsyncTask<ServerSocket,String,Void>{

		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];
			while (true) {
				try {
					Socket client = serverSocket.accept();
					BufferedReader is = new BufferedReader(
							new InputStreamReader(client.getInputStream()));
					String msg = is.readLine();
					String[] split=msg.split("_");
					if(split[2].equals("true")){
					//If the receiving avd is the sequencer, order it and send a multicast message.
						sequencer(msg);
					}
					else if(split[2].equals("false")){
						keyValuePair.put(split[1],split[0]); // If its a group member store it in a buffer
					}
					if(split[0].equals("order")){
						//This message is from a sequencer.Deliver the message
						String message=split[1];
						String textid=split[2];
						keyValuePair.put(textid,message);
						if(keyValuePair.containsKey(textid)){   //Check buffer if it contains key.If yes deliver it.
						deliver(Integer.parseInt(split[3]),textid);
						}
						else{
							buffer.put(split[3],textid);
						}
					}
			      //Display the message
					publishProgress(msg);
				} catch (IOException e) {
					Log.e(TAG, "Error");
				}
			}
		}
		//Sequencer for assigning tokens
		 synchronized void sequencer(String TextWithid){  
			String[] textwithid=TextWithid.split("_");
			String messageToSend=textwithid[0];
			String textid=textwithid[0];
			keyValuePair.put(textid,messageToSend); //Store message in buffer
			deliver(groupSequence,textid);           // Deliver the message from the buffer   
			try {
				for(int i=1;i<PORT_ARRAY.length;i++){
				Socket socket = new Socket(InetAddress.getByAddress(new byte[] {
						10, 0, 2, 2 }),Integer.parseInt(PORT_ARRAY[i]));
				DataOutputStream os=new DataOutputStream(socket.getOutputStream());
				OutputStreamWriter out1=new OutputStreamWriter(os);
				out1.write("order"+"_"+messageToSend+"_"+textid+"_"+groupSequence);        //Broadcast groupSequence to all processors
				out1.flush();
				os.close();
				socket.close();
				}
				++groupSequence;    //Increment group specific number by one for each message
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			catch(NullPointerException E){
				Log.e(TAG,"NullPointerException");
			}
		}
		 void deliver(int seq,String id){
			//Insert into the content Provider based on the sequence order
			ContentValues cv = new ContentValues();           //Copied from project1
	    	cv.put("key",String.valueOf(seq));
	    	cv.put("value",keyValuePair.get(id));
	    	getBaseContext().getContentResolver().insert(mUri,cv);
		}
    	protected void onProgressUpdate(String... strings){  //Copied from project1
    		//To display the String
    		String strReceived = strings[0].trim();
    		TextView textView=(TextView) findViewById(R.id.textView1);
    		textView.append(strReceived +"\t\n");
			return;
    	}
    }
    
    private class ClientTask extends AsyncTask<String,Void,Void>{

		@Override
		protected Void doInBackground(String... params) {
			String msg=params[0];
			multicast(msg,PORT_ARRAY);   //Send a multicast message to all avds
			return null;
		}
		protected void multicast(String messageToMulticast,String[] port){
			//Broadcast message to all avds including the one thats sending it.
			String[] msgToSend=messageToMulticast.split("_");
			String id=msgToSend[1]+i;
			try {
				//Broadcast the msg to all the avds one by one along with a msg id and flag.
				for(int i=0;i<port.length;i++){
				Socket socket = new Socket(InetAddress.getByAddress(new byte[] {
						10, 0, 2, 2 }),Integer.parseInt(port[i]));
				boolean flag=false;
				if(port[i].equals("11108")){
					flag=true;                  //Flag will be true if the avd is the sequencer
				}
				DataOutputStream os=new DataOutputStream(socket.getOutputStream());
				OutputStreamWriter out1=new OutputStreamWriter(os);
				out1.write(msgToSend[0]+"_"+id+"_"+flag);
				out1.close();
				os.close();
				socket.close();
				}
				i=i+1;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			catch(NullPointerException E){
				Log.e(TAG,"NullPointerException");
			}
		}	
    }
}
