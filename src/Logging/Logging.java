package Logging;

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * 
  Log class to create a log for each peer
  
*/

public class Logging {

	static Logger peerLog;
	static String title;
	static String loggerName = "CNT5106C";

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %5$s%6$s%n");
	}

//	public Logging() {
//		Logger bitTorrent = Logger.getLogger("CNT5106C");
//		peerLog = bitTorrent;
//	}
	public Logging() {
		Logger bitTorrent = Logger.getLogger(loggerName);
		peerLog = bitTorrent;
	}

	public void mes (int peerId, String message) {
		String msg = title+message+peerId;
		peerLog.log(Level.INFO, msg);
	}
	
	public void changeOfOptimisticallyUnchokedNeighbors (int unchokeNeighbours) {
        final String msg = title + " chose " + Integer.toString(unchokeNeighbours)+" as optimistically unchoked neighbor";
        peerLog.log(Level.INFO, msg);
    }
	
	public static void setPeerLog(int peerId) {
		title = ": Peer" + " " + Integer.toString(peerId);
		String filename = "log_peer_" + Integer.toString(peerId) + ".log";

		try {
			Handler loggerHandler = new FileHandler(filename);

			Formatter formatter = (Formatter) Class.forName("java.util.logging.SimpleFormatter").newInstance();

			loggerHandler.setFormatter(formatter);
			loggerHandler.setLevel(Level.parse("INFO"));
			peerLog.addHandler(loggerHandler);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void closeLogger() {
		for (Handler h : peerLog.getHandlers()) {
			h.close();
		}
	}


//	public void getingConnected(int peerId) {
//		String msg = title + " connected from " + peerId;
//		peerLog.log(Level.INFO, msg);
//	}



    public void receivedHave (int peerId, int pieceIdx) {
        String msg = title + " received the 'have' message from " + Integer.toString(peerId)+ "for the piece " + Integer.toString(pieceIdx);
        peerLog.log (Level.INFO,msg);
    }

//    public void receivedInterested (int peerId) {
//        String msg = title + " interested from "+ Integer.toString(peerId);
//        peerLog.log (Level.INFO,msg);
//    }

//    public void receivedNotInterested (int peerId) {
//        final String msg = title + " not interested message from "+Integer.toString(peerId);
//        peerLog.log (Level.INFO,msg);
//    }

    public void pieceDownloaded (int peerId, int pieceIdx, int currNumberOfPieces) {
        final String msg = title + Integer.toString(pieceIdx) +"piece downloaded from peer "+ Integer.toString(peerId);
        peerLog.log(Level.INFO,msg);
    }

    public void fileComplete () {
        final String msg = title + " Complete file downloaded";
        peerLog.log (Level.INFO,msg);
    }
	
// This method is used to make a list of peers as a single string seperated with a comma
	private String peerString(HashSet<Integer> preferredNeighbours) {

		StringBuilder s = new StringBuilder();
		Iterator<Integer> value = preferredNeighbours.iterator();
		while (value.hasNext()){
			Integer pid = (Integer) value.next();
      s.append(Integer.toString(pid));

      if(value.hasNext()){
				s.append(",");
			}
			else{
				break;
			}
		}
		return s.toString();
  }

	public void changeOfPrefereedNeighbors(HashSet<Integer> preferredNeighbours) {

		String neighbours = peerString(preferredNeighbours);
		String message = title + " now chose " + neighbours+" as preferred neighbors";
		peerLog.log(Level.INFO, message);

	}

}
