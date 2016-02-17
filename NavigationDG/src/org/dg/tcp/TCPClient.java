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

package org.dg.tcp;

import android.util.Log;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
 
 
public class TCPClient {
 
    public static String SERVERIP;
    public static int SERVERPORT;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
 
    PrintWriter out;
    BufferedReader in;
 
    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     *  Setting up the server's IP and PORT
     */
    public TCPClient(ConnectionIPPort server, OnMessageReceived listener) {
        mMessageListener = listener;
        SERVERIP = server.getIP().toString().substring(1);
        SERVERPORT = server.getPORT();
    }
 
    /**
     * Sends the message entered by client to the server
     */
    public void sendMessage(String message){
        if (out != null && !out.checkError()) {
            out.print(message);
            out.flush();
            Log.e("TCP Client", "C: Sent:" + message);
        }
    }
    
    /**
     * Ending receiving, closing socket and ending ASync Thread
     */
    public void stopClient(){
        mRun = false;
    }
 
    /**
     * Running TCP connection
     */
    public void run() {
 
        mRun = true;
 
        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(SERVERIP);
 
            Log.d("TCP Client", "C: Connecting... to " + SERVERIP + ":" + SERVERPORT);
 
            //create a socket to make the connection with the server
            Socket socket = new Socket(serverAddr, SERVERPORT);
 
            Log.d("TCP Client", "C: Created new socket");
            
            
            //mMessageListener.messageReceived("Connected");
            
            try {
 
                //send the message to the server
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
 
                //receive the message which the server sends back
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                Log.d("TCP Client", "C: Listening");
                
                
                this.sendMessage("READY");
                
                //in this while the client listens for the messages sent by the server
                while (mRun) {

                    char[] buf = new char[150];
					int a = in.read(buf);
                 
                    if (a!=0 && mMessageListener != null) {
                        //call the method messageReceived from MyActivity class
                    	String msg = new String(buf,0,a);
                    	Log.d("TCP Client", "RECV:" + a + " | " + msg);
                    	
                    	mMessageListener.messageReceived(msg);
                    	
                    	if ( msg.contains("X") )
                    	{
                    		break;
                    	}
                    }
                }
 
            } catch (Exception e) {
 
                Log.e("TCP", "S: Error", e);
 
            } finally {
                //the socket must be closed. It is not possible to reconnect to this socket
                // after it is closed, which means a new socket instance has to be created.
                
            }
 
            
            socket.close();
        } catch (Exception e) {
 
            Log.e("TCP", "C: Error TCP: ", e);
 
        }
    }
 
    /**
     * ASync must implement this interface in order to process data
     */
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}