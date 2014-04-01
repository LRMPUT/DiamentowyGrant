package org.dg.tcp;

import java.net.InetAddress;

public class ConnectionIPPort {
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

	public ConnectionIPPort(InetAddress ipaddress, int i) {
		IP = ipaddress;
		PORT = i;
	}

}
