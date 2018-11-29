package peers;

import java.util.BitSet;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import java.util.LinkedList;
import java.util.ListIterator;

public class Peer {
	private int peerID;
	private String hostName;
	private int listeningPort;
	private boolean hasFile;
	public AtomicBoolean interested = new AtomicBoolean();
	AtomicInteger downloadRate = new AtomicInteger(-1);
	BitSet partsReceived;
	
//	PeerInfo constructor to set values to the class variables
	public Peer(int pID, String hosName, int liPort, int has_File){
		
		setPeerID(pID);
		setHostName(hosName);
		setListeningPort(liPort);
		setHasFile(has_File);
		
		partsReceived = new BitSet();
		
//		if the peer has file set all bits in partsReceived to true(1) or set all bits in partsReceived to false
//		if(hasFile == true) {
//			System.out.println("yes system has file..");
//			System.out.println("partsReceived"+partsReceived);
//			partsReceived.set(0, partsReceived.length()-1, true);
//		}
//		else {
//			partsReceived.set(0,partsReceived.length()-1,false);
//		}
	}
	//Getters and Setters for all the class variables
	public int getPeerID() {
		return peerID;
	}

	public void setPeerID(int peerID) {
		this.peerID = peerID;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getListeningPort() {
		return listeningPort;
	}

	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}

	public boolean isHasFile() {
		return hasFile;
	}

	public void setHasFile(int hf) {
		if(hf == 1)
			this.hasFile = true;
		else
			this.hasFile = false;
	}

	public BitSet getPartsReceived() {
		return partsReceived;
	}

	public void setPartsReceived(BitSet partsReceived) {
		this.partsReceived = partsReceived;
	}
	
	public void setDownloadRate(int dr) {
		this.downloadRate.set(dr);
	}
	
	public int getDownloadRate() {
		return downloadRate.get();
	}

	public static Peer getDataUsingID(int peerID, LinkedList<Peer> allPeers) {
		ListIterator<Peer> listIterator = allPeers.listIterator();
		while (listIterator.hasNext()) {
			Peer next = listIterator.next();
			if(peerID == next.peerID)
				return next;
		}
		return null;
	}
	
	static HashSet<Integer> getPeerIds(LinkedList<Peer> peers) {
        HashSet<Integer> peerIds = new HashSet<Integer>();
        if(peers!=null && !peers.isEmpty()){
        	for(Peer p : peers) {
        		peerIds.add(p.getPeerID());
        	}
        }
        return peerIds;
     }
	
}
