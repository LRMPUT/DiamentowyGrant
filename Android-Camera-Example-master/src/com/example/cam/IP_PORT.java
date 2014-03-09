package com.example.cam;

import java.net.InetAddress;

public class IP_PORT {
	public InetAddress IP;
	public int PORT;

	public InetAddress getIP() {
		return IP;
	}

	public void setIP(InetAddress iP) {
		IP = iP;
	}

	public int getPORT() {
		return PORT;
	}

	public void setPORT(int pORT) {
		PORT = pORT;
	}

	IP_PORT(InetAddress ipaddress, int i) {
		IP = ipaddress;
		PORT = i;
	}

}
