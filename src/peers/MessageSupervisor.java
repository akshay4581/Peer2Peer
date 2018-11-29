package peers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogRecord;

import Logging.Logging;

public class MessageSupervisor {

	private FileSupervisor fileSupervisor;
    private boolean choked;
    private Logging _logger;
    private PeerSupervisor peerSupervisor;
    private AtomicInteger remotePeerID;
    
    
    public MessageSupervisor(AtomicInteger rp_ID, FileSupervisor fs, PeerSupervisor ps) {
    	choked = true;
    	fileSupervisor = fs;
    	peerSupervisor = ps;
    	remotePeerID = rp_ID;
    }
    
//    function to send bitfield upon receiving handshake
    public Message respondWithBitfield(Handshake hs) {
    	//TODO modify code for plague check.. remove bitfield class itself
    	BitSet bitfield = fileSupervisor.partsWithPeer();
    	if(!bitfield.isEmpty())
    		return(new BitField(bitfield.toByteArray()));
    	else
    		return null;
    }
    
    public synchronized Message handleAllMessages(Message msg) {
    	if(msg !=null) {
    		String mType = msg.getMesType();
    		switch(mType){
    			case "Choke":{
//    				TODO logging
    				choked = true;
    				Logging logrecord = new Logging();   				 	
					logrecord .mes(remotePeerID.get(),"Choke");
    				return null;
    			}
    				
    			case "Unchoke":{
    				choked = false;
    				Logging logrecord = new Logging();
    				logrecord.mes(remotePeerID.get(),"unchoked");
    				return requestPiece();
    			}
    				
    			case "Interested":{
    				peerSupervisor.setInterestedForRemotePeer(remotePeerID.get(), true);
    				Logging logrecord = new Logging();
    				logrecord.mes(remotePeerID.get(),"interested from");
    				return null;
    			}
    			
    			case "Uninterested":{
    				peerSupervisor.setInterestedForRemotePeer(remotePeerID.get(), false);
    				Logging logrecord = new Logging();
    				logrecord.mes(remotePeerID.get(),"not interested message from");
    				return null;
    			}
    			
    			case "Have":{
    				Have have = (Have)msg;
    				ByteBuffer payloadBuffer = ByteBuffer.wrap(Arrays.copyOfRange(have.payLoad,0,4));
    				ByteBuffer orderedPayload = payloadBuffer.order(ByteOrder.BIG_ENDIAN);
    				int partItHas = orderedPayload.getInt();
    				peerSupervisor.setHaveOfRemotePeer(remotePeerID.get(),partItHas);
    				Logging log = new Logging();
    				log.receivedHave(remotePeerID.get(), partItHas);
    				if(!fileSupervisor.partsWithPeer().get(partItHas)) {
    					return new Message("Interested");
    				}
    				else
    					return new Message("Notinterested");
    			}
    			
    			case "BitField":{
    				System.out.println("Received bitfield from "+remotePeerID.get());
    				BitField b = (BitField)msg;
    				BitSet bitfield = BitSet.valueOf(b.payLoad);
    				peerSupervisor.setBitfieldOfRemotePeer(remotePeerID.get(),bitfield);
    				bitfield.andNot(fileSupervisor.partsWithPeer());
    				if(!bitfield.isEmpty()) {
    					return new Message("Interested");
    				}
    				else {
    					return new Message("Notinterested");
    					
    				}
    			}
    			
    			case "Request":{
    				Logging log = new Logging();
    				Request r = (Request)msg;
    				ByteBuffer payloadBuffer = ByteBuffer.wrap(Arrays.copyOfRange(r.payLoad,0,4));
    				ByteBuffer orderedPayload = payloadBuffer.order(ByteOrder.BIG_ENDIAN);
    				int partRequested = orderedPayload.getInt();
    				boolean thisHasPart = fileSupervisor.partsWithPeer().get(partRequested);
    				boolean canSendTo = peerSupervisor.canSend(remotePeerID.get());
    				
    				if(partRequested != -1 && thisHasPart && canSendTo) {
    					byte[] temp = fileSupervisor.sendRequestedPart(partRequested, fileSupervisor.partsPath);
    					if(temp != null) {
    						System.out.println("temp is not null");
    						return new Piece(temp);
    					}
    				}
    				return null;
    			}
    			
    			case "Piece":{
				// TODO remove this code and modify all
				if (msg.getMesType().equals("Request")) {
					return null;
				}
				System.out.println(mType);
				Piece piece = (Piece) msg;
				int sentPieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(piece.payLoad, 0, 4))
						.order(ByteOrder.BIG_ENDIAN).getInt();
				;
				// TODO
//              logrecord.getLogRecord().pieceDownloaded(_remotePeerID.get(), piece.getPieceIndex(), _fileManager.partsPeerHas().cardinality());
				fileSupervisor.addReceivedPart(sentPieceIndex, piece.getPieceContent());
				peerSupervisor.partsReceived(remotePeerID.get(), piece.getPieceContent().length);

				// System.out.println("2nd" + _fileManager.partsPeerHas().toString());

				return requestPiece();
    			}
    			default:{
    				return null;
    			}
    		}
    	}
    	return null;
    }
    
	private Message requestPiece() {
		if(!choked) {
			BitSet bitfieldOfRemotePeer = peerSupervisor.partsRemotePeerHas(remotePeerID.get());
			int partToRequest = fileSupervisor.selectPartToRequest(bitfieldOfRemotePeer);
			
			if(partToRequest!=-1) {
				System.out.println("partRequested............"+partToRequest);
				return new Request(partToRequest);
			}
			else
				return new Message("Notinterested");
				
		}
		return null;
	}
	
}
