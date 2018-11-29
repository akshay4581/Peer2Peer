package peers;

import java.util.LinkedList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Properties;
import java.util.Scanner;
import Logging.*;

public class StartProcess {
	
	public static LinkedList<Peer> allPeers = new LinkedList<Peer>();
	public static Properties commonConfig = new Properties();
	
	public static void main(String[] args) throws Exception{
		
		Scanner input = new Scanner(System.in);
		int peerID = input.nextInt();
		input.close();
		
		//create log for each peerID
		Logging lobj= new Logging();
		lobj.setPeerLog(peerID);
		
//		Reading common.cfg
		FileReader common = new FileReader("Common.cfg"); 
		try {
			commonConfig.load(common);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
//		Reading PeerInfo.cfg
		File peerInfo = new File("PeerInfo.cfg"); 
		  BufferedReader br = new BufferedReader(new FileReader(peerInfo));
		  String line;
		  while ((line = br.readLine()) != null){
			  String[] eachLine = line.split(" ");
			  System.out.println(line);
			  allPeers.add(new Peer(Integer.parseInt(eachLine[0]), eachLine[1], Integer.parseInt(eachLine[2]), Integer.parseInt(eachLine[3])));
		  }
		  br.close();
		  
		  Peer currentPeer = Peer.getDataUsingID(peerID, allPeers);
		  SetupPeer currentPeerSetup = new SetupPeer(commonConfig, allPeers, currentPeer);
		  currentPeerSetup.startThreadForPeerManager();
		  new Thread(currentPeerSetup).start();
		  int temp = 100;
		  while(temp>=0) {
			  temp--;
		  }
		  currentPeerSetup.establishConnections(currentPeer.getPeerID(),allPeers);  	  
	}
}
