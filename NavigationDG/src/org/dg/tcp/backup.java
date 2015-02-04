//DetectDescribe det = new DetectDescribe();
//(new Thread(det)).start();
// 
//VisualOdometry vo = new VisualOdometry();
//(new Thread(vo)).start();
// 		
// 	
// 				if (connected == false) {
// 					
// 					InetAddress selected_ip;
// 					try {
//  						selected_ip = InetAddress.getByName("192.168.1.132");
// 						selected_ip = InetAddress.getByName("192.168.0.11");
// 						ConnectionIPPort adres = new ConnectionIPPort(selected_ip, 3000);
// 						
// 						 selected_ip = InetAddress.getByName("192.168.2.222");
// 						 IP_PORT adres = new IP_PORT(selected_ip, 27000);
// 
// 						new connectionTCP().execute(adres);
// 						
// 						Log.e("TCP activity", "Connecting to : " + selected_ip.toString()
// 								+ ":" + 3000);
// 					} catch (UnknownHostException e) {
// 					
// 						e.printStackTrace();
// 					}
// 				} 
// 			
//	
// 				
//public static org.dg.tcp.TCPClient mTcpClient;
//boolean connected = false;
//public int synchronizationTime = 0;
//		
//		
//		
//		
//		
//		public class connectionTCP extends AsyncTask<ConnectionIPPort,String,Void> {
//	   	 
//		@Override
//		protected Void doInBackground(ConnectionIPPort... adres) {
//
//			
//			mTcpClient = new org.dg.tcp.TCPClient(adres[0],new org.dg.tcp.TCPClient.OnMessageReceived() {
//				@Override
//				// here the messageReceived method is implemented
//				public void messageReceived(String message) {
//					// this method calls the onProgressUpdate
//					publishProgress(message);
//				}
//			});
//			mTcpClient.run();
//			
//			return null;
//		}
//
//		@Override
//		protected void onProgressUpdate(String... msg) {
//  	      	
//			long timeTaken = (System.nanoTime() - startTimeGlobal) - startTime;
//			
//			Log.e("TCP", "Progress: " + msg[0]);
//
//			// Connection is established properly
//			if (connected == false) {
//				// We start something
//				connected = true;
//				startTime =0 ;
//				startTimeGlobal =  System.nanoTime();
//			}
//
//			// Message -> show as toast
//			int duration = Toast.LENGTH_LONG;
//			Toast.makeText(getApplicationContext(), msg[0], duration).show();
//
//			if (msg[0].contains("SYN")) {
//				String[] separated = msg[0].split(" ");
//				long compTime =  Long.parseLong(separated[1].trim());
//				Log.d("TCP", "timeTaken: " + timeTaken + "\n");
//				Log.d("TCP", "Computer time: " + compTime + "\n");
//				Log.d("TCP", "Time for ping/pong in ns: " + (timeTaken-compTime) + "\n");
//				Log.d("TCP", "Time for ping/pong in ms: " + (timeTaken-compTime)/1000000 + "\n");
//				
//				if ((timeTaken-compTime)/1000000 <= 3)
//				{
//					mTcpClient.sendMessage("END " + (timeTaken-compTime)/2 + "\n");
//				}
//			}
//
//			if (msg[0].contains("START")) {
//				startTime = System.nanoTime() - startTimeGlobal;
//				Log.d("TCP", "Sending: " + "SYN " + startTime + "\n");
//				mTcpClient.sendMessage("SYN " + startTime + "\n");
//			}
//
//			if (msg[0].contains("X")) {
//				// We end something
//				connected = false;
//			}
//
//		}
//	}