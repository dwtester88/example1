package net.majorkernelpanic.example1;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.audio.AudioQuality;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * A straightforward example of how to use the RTSP server included in libstreaming.
 */
public class MainActivity extends Activity {

	private final static String TAG = "MainActivity";
	static final int SocketServerPORT = 1234;
	ServerSocket serverSocket;
	ServerSocketThread serverSocketThread;

	private SurfaceView mSurfaceView;
	TextView ipaddress;
	File file,file1;
	Intent icam;



	private static final int LISTENER_PORT = 50003;


	private static final int BUF_SIZE = 1024;
	private boolean IN_CALL = false;
	private boolean LISTEN = false;
	private boolean waitforpicture = true;

	ReciverCall reciverCall = new ReciverCall();

	BroadcastIP broadcastIP;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		ipaddress = (TextView) findViewById(R.id.ipaddress);
		ipaddress.setText(getIpAddress());

		Log.d(TAG,"Mainactivity started and incoming call listener is about to start");

		//Listner is turned ON to listen for incomming call (push to talk incoming call from client)
		startCallListener();

		// Sets the port of the RTSP server to 1234
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(RtspServer.KEY_PORT, String.valueOf(8555));
		editor.commit();

		Log.d(TAG, "waitforpicture on start " +waitforpicture);


		// Configures the SessionBuilder
		SessionBuilder.getInstance()
		.setPreviewOrientation(0)
		.setSurfaceView(mSurfaceView)
		.setContext(getApplicationContext())
		.setVideoQuality(new VideoQuality(176,144,10,100000))//my change
		.setAudioEncoder(SessionBuilder.AUDIO_AAC)
		.setVideoEncoder(SessionBuilder.VIDEO_H264)
		.setCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);

		//Camera intent is used to take picture and save it in memory
		/*try {
			icam = new Intent(MainActivity.this, CameraView.class);
			startActivityForResult(icam, 999);
		} catch (Exception e) {
			Log.d("error on camera intent", e.getMessage());
		}*/

		Log.d(TAG,"Camera intent is started to take picture and save for first time");
		icam = new Intent(MainActivity.this, CameraView.class);
		icam.putExtra("First",true);
		icam.putExtra("wait_flag",true);
		startActivityForResult(icam, 999);

		//This will broadcast the IPaddress of server to all the units in the network.
		//broadcastIP= new BroadcastIP(getIpAddress(),getBroadcastIp());


		// Starts the RTSP server
		try {
			Log.d(TAG,"RTSPServer intent is called");
			this.startService(new Intent(this, RtspServer.class));
		}
		catch (Exception e){
			Toast.makeText(getApplicationContext(),"keyport: "+e.getMessage(),Toast.LENGTH_LONG).show();
		}
		isDeviceSupportCamera();

		// ServerSocket Thread is used to catch the picture request from the client and take picture and send it to client.
		serverSocketThread = new ServerSocketThread();
		serverSocketThread.start();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

			if(resultCode == Activity.RESULT_OK){
				Log.d(TAG,"Camera intent returns OK Code");
				//waitforpicture=false;
				if(data.getBooleanExtra("result",false)){
					Log.d(TAG,"Camera took piture for first time");

					Log.d(TAG," activity result"+data.getBooleanExtra("result",false));
					//This will broadcast the IPaddress of server to all the units in the network.
					broadcastIP= new BroadcastIP(getIpAddress(),getBroadcastIp());
				}
				waitforpicture=data.getBooleanExtra("wait_flag",true);
			}
			if (resultCode == Activity.RESULT_CANCELED) {
				//Write your code if there's no result
			}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private InetAddress getBroadcastIp() {
		// Function to return the broadcast address, based on the IP address of the device
		try {

			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			String addressString = toBroadcastIp(ipAddress);
			InetAddress broadcastAddress = InetAddress.getByName(addressString);
			return broadcastAddress;
		}
		catch(UnknownHostException e) {

			Log.e("MainAvtivity", "UnknownHostException in getBroadcastIP: " + e);
			return null;
		}

	}

	private void isDeviceSupportCamera() {
		if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			Toast.makeText(getApplicationContext(), "Device has Camera", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), "Device doesnot supportCamera", Toast.LENGTH_LONG).show();
		}
	}

	private String getIpAddress() {
		String ip = "";
		try {
			Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
					.getNetworkInterfaces();
			while (enumNetworkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = enumNetworkInterfaces
						.nextElement();
				Enumeration<InetAddress> enumInetAddress = networkInterface
						.getInetAddresses();
				while (enumInetAddress.hasMoreElements()) {
					InetAddress inetAddress = enumInetAddress.nextElement();

					if (inetAddress.isSiteLocalAddress()) {
						ip += inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ip += "Something Wrong! " + e.toString() + "\n";
		}
		return ip;
	}

	public class ServerSocketThread extends Thread {
		@Override
		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(SocketServerPORT);
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
//						infoPort.setText("Port: "
//								+ serverSocket.getLocalPort());
					}
				});
				while (true) {
					socket = serverSocket.accept();
					Log.d(TAG,"Server Socket started"+socket.getInetAddress().toString());
					FileTxThread fileTxThread = new FileTxThread(socket);
					fileTxThread.start();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	public class FileTxThread extends Thread {
		Socket socket;
		FileTxThread(Socket socket) {
			this.socket = socket;
		}
		@Override
		public void run() {

			DataInputStream dis = null;
			String ClientCommand=null;
			try {
				dis = new DataInputStream(socket.getInputStream());
				ClientCommand = dis.readUTF();
				//dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d("json is ", ClientCommand);
			file = new File(
					Environment.getExternalStorageDirectory(),
					"test2.png");
			byte[] bytes = new byte[(int) file.length()];
			BufferedInputStream bis;

			if(ClientCommand.contains("First")){
				//if(file.exists()){

					try {
						Log.d("ClientCommand ",ClientCommand);
						bis = new BufferedInputStream(new FileInputStream(file));
						bis.read(bytes, 0, bytes.length);
						OutputStream os = socket.getOutputStream();
						os.write(bytes, 0, bytes.length);
						os.flush();

						final String sentMsg = "File sent to: " + socket.getInetAddress().toString();
						socket.close();
						//file.delete();
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this,
										sentMsg,
										Toast.LENGTH_LONG).show();
							}
						});
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					/*//send pitture
				}
				else{

					try {
						icam = new Intent(MainActivity.this, CameraView.class);
						startActivityForResult(icam, 999);
					} catch (Exception e) {
						Log.d("error on camera intent", e.getMessage());
					}

					Log.d("contains file",file.getPath());
					//Log.d("contains file", String.valueOf(camrun));
					try {
						Thread.sleep(2500); // 5sec delay so that picture is taken and saved in memory later we will streamout
					} catch (InterruptedException ex) {
						System.out.println("I'm interrupted");
					}

					//take and send pitture

					try {
						Log.d("contains ",ClientCommand);
						bis = new BufferedInputStream(new FileInputStream(file));
						bis.read(bytes, 0, bytes.length);
						OutputStream os = socket.getOutputStream();
						os.write(bytes, 0, bytes.length);
						os.flush();

						final String sentMsg = "File sent to: " + socket.getInetAddress().toString();
						socket.close();

						//file.delete();
						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {
								Toast.makeText(MainActivity.this,
										sentMsg,
										Toast.LENGTH_LONG).show();
							}
						});
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							socket.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				//check for saved piture or take picture one time and save
*/
			}
			else if(ClientCommand.contains("SNAP")){
				file.delete();
				try {
					icam = new Intent(MainActivity.this, CameraView.class);
					icam.putExtra("wait_flag",false);
					startActivityForResult(icam, 999);
				} catch (Exception e) {
					Log.d("error on camera intent", e.getMessage());
				}

				/*try {
					Thread.sleep(4500); // 7.5sec delay so that picture is taken and saved in memory later we will streamout
				} catch (InterruptedException ex) {
					System.out.println("I'm interrupted");
				}
*/				Log.d(TAG, "waitforpicture before loop " +waitforpicture);
				while (waitforpicture){

				// waiting for picture to be saved.
				}
				Log.d(TAG, "waitforpicture after loop " +waitforpicture);

                bytes = new byte[(int) file.length()];
				waitforpicture = true;

				try {
					//Log.d("contains ",ClientCommand);
					bis = new BufferedInputStream(new FileInputStream(file));
					bis.read(bytes, 0, bytes.length);
					OutputStream os = socket.getOutputStream();
					os.write(bytes, 0, bytes.length);
					os.flush();

					final String sentMsg = "File sent to: " + socket.getInetAddress().toString();
					socket.close();
					//file.delete();
					MainActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							Toast.makeText(MainActivity.this,
									sentMsg,
									Toast.LENGTH_LONG).show();
						}
					});
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				//take picture and delete
			}





/*
            try {
                InputStream dIn = socket.getInputStream();
                int in=dIn.read();
                Log.d("Message ", String.valueOf(in));
            } catch (IOException e) {
                e.printStackTrace();
            }*/




			/*File file = new File(
					Environment.getExternalStorageDirectory(),
					"test2.png");*/

		}
	}

	@Override
	protected void onDestroy() {
		file.delete();
		super.onDestroy();
	}

	private void startCallListener() {
		// Creates the listener thread
		LISTEN = true;
		Thread listener = new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					// Set up the socket and packet to receive
					Log.i(TAG, "Incoming call listener started");
					DatagramSocket socket = null;
					try {
						 socket = new DatagramSocket(LISTENER_PORT);
					}catch (SocketException e) {

						Log.e(TAG, "SocketExcepion in listener: " + e);
						return;
					}

					//DatagramSocket socket = new DatagramSocket(LISTENER_PORT);
					socket.setSoTimeout(10000);
					byte[] buffer = new byte[BUF_SIZE];
					DatagramPacket packet = new DatagramPacket(buffer, BUF_SIZE);
					while(LISTEN) {
						// Listen for incoming call requests
						try {
							Log.i(TAG, "Listening for incoming calls" );
							socket.receive(packet);
							String data = new String(buffer, 0, packet.getLength());
							Log.i(TAG, "Packet received from "+ packet.getAddress() +" with contents: " + data);
							String action = data.substring(0, 4);
							if(action.equals("CAL:")) {

								Log.i(TAG, "CAL: is received so accept the call" );
								// Received a call request. Start the ReceiveCallActivity
								String address = packet.getAddress().toString();
								String name = data.substring(4, packet.getLength());

								//Intent intent = new Intent(MainActivity.this, ReceiveCallActivity.class);
								reciverCall.acceptcall(address.substring(1, address.length()));

								//intent.putExtra(EXTRA_CONTACT, name);
								//intent.putExtra(EXTRA_IP, address.substring(1, address.length()));
								IN_CALL = true;
								//LISTEN = false;
								//stopCallListener();
								//startActivity(intent);
							}
							else {
								// Received an invalid request
								Log.w(TAG, packet.getAddress() + " sent invalid message: " + data);
							}
						}
						catch(Exception e) {}
					}
					Log.i(TAG, "Call Listener ending");
					socket.disconnect();
					socket.close();
				}
				catch(SocketException e) {

					Log.e(TAG, "SocketException in listener " + e);
				}
			}
		});
		listener.start();
	}


	private String toBroadcastIp(int ip) {
		// Returns converts an IP address in int format to a formatted string
		return (ip & 0xFF) + "." +
				((ip >> 8) & 0xFF) + "." +
				((ip >> 16) & 0xFF) + "." +
				"255";
	}


	
}
