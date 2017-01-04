package org.dg.obsolete;
// OpenAIL - Open Android Indoor Localization
// Copyright (C) 2015 Michal Nowicki (michal.nowicki@put.poznan.pl)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright notice,
//   this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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