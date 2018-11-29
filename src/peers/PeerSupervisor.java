package peers;

import java.util.LinkedList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import Logging.Logging;

public class PeerSupervisor implements Runnable{
	
	int all;
	Peer peer;
	LinkedList<Peer> otherPeers;
	Properties commonConfig;
	Random random;	
	OptimisticUnchoke optUnchoke;
	SetupPeer peerSetUp;
	AtomicBoolean peerFileCompleted = new AtomicBoolean(false);
	LinkedList<Peer> preferredPeers;
	
	PeerSupervisor(Peer p, LinkedList<Peer> neighbours,Properties common,SetupPeer sp,int a){
		this.peer = p;
		this.otherPeers = new LinkedList<>(neighbours);
		this.commonConfig = common;
		this.peerSetUp = sp; 
		this.optUnchoke = new OptimisticUnchoke(sp);
		this.all = a;
		System.out.println("size of bitfield taken as ------"+all);
		
		if(peer.isHasFile())
			peerFileCompleted.set(true);
		else
			peerFileCompleted.set(false);
			
	}
	
	public void run() {
        Thread t = new Thread(optUnchoke);
        t.start();
        while(true) {
        try {
            Thread.sleep(Integer.parseInt(StartProcess.commonConfig.getProperty("UnchokingInterval"))*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        LinkedList<Peer> interestedPeers = getInterestedPeers(otherPeers);
           // System.out.println(interestedPeers.size());
        HashSet<Integer> interestedPeersIds = new HashSet<Integer>();
        interestedPeersIds = Peer.getPeerIds(interestedPeers);

        LinkedList<Peer> preferredPeers  = getPreferredPeers(interestedPeers,peerFileCompleted.get());
        HashSet<Integer> preferredPeersIds = new HashSet<Integer>();
        preferredPeersIds = Peer.getPeerIds(preferredPeers);
            if(preferredPeersIds!=null && preferredPeersIds.size()>0){
            	Logging logrecord = new Logging();
            	logrecord.changeOfPrefereedNeighbors(preferredPeersIds);
            }

            this.preferredPeers = preferredPeers;
           // System.out.println(preferredPeers.size());

        LinkedList<Peer> peersToChoke = getToBeChoked(preferredPeers);
        HashSet<Integer> peersToChokeIds= new HashSet<Integer>();
        peersToChokeIds = Peer.getPeerIds(peersToChoke);
        peerSetUp.chokePeers(peersToChokeIds);                //choking peers
        peerSetUp.unchokePeers(preferredPeersIds);

        LinkedList<Peer> optimisticallyUcPeers = getOptimisticallyUcPeers(preferredPeers,interestedPeers);
        HashSet<Integer> optimisticallyUnchokePeerIds = new HashSet<Integer>();
        optimisticallyUnchokePeerIds = Peer.getPeerIds(optimisticallyUcPeers);
           if(optimisticallyUcPeers!=null){
        	   optUnchoke.setPossiblyUnchokable(optimisticallyUcPeers);
           }    
        }
    }

	synchronized LinkedList<Peer> getInterestedPeers(LinkedList<Peer> peers) {
        LinkedList<Peer> interestedPeers= new LinkedList<Peer>();
        for(Peer p : peers) {
            if(p.interested.get()){ 
            	interestedPeers.add(p);
            	System.out.println("added "+p.getPeerID()+" to interedted list");
            }
        }
        return interestedPeers;
    }
	
	synchronized LinkedList<Peer> getPreferredPeers(LinkedList<Peer> interestedPeers,boolean peerFileCompleted) {
        LinkedList<Peer> preferredPeers = new LinkedList<Peer>();
        if( interestedPeers!=null && !interestedPeers.isEmpty()) {
            if (peerFileCompleted) {
                Collections.shuffle(interestedPeers);
            }
            else {
                Collections.sort(interestedPeers, new Comparator<Peer>() {
                    @Override
                    public int compare(Peer o1, Peer o2) {
                        if (o1.downloadRate.get() > o2.downloadRate.get()) {
                            return 1;
                        } else {
                            return -1;
                        }

                    }
                });
            }
          //take the top 2 from this sorted PeerInfo list

            int i = 1;
            for (Peer p : interestedPeers) {
                preferredPeers.add(p);
                System.out.println("added "+p.getPeerID()+" to preferred list");
                if (i == 2 || i == interestedPeers.size()) {
                    break;
                }
                i++;
            }
        }
        return preferredPeers;
     }
	
	synchronized LinkedList<Peer> getOptimisticallyUcPeers(LinkedList<Peer> preferredPeers, LinkedList<Peer> interestedPeers) {
		if(interestedPeers.size()>2){
			LinkedList<Peer> peers = new LinkedList<Peer>(interestedPeers);
            peers.removeAll(preferredPeers);
            return peers;}
        else
            return null;
    }
	
	synchronized LinkedList<Peer> getToBeChoked(LinkedList<Peer> preferredPeers) {
		LinkedList<Peer> ans = new LinkedList<Peer>(otherPeers);
		ans.removeAll(preferredPeers);
		return ans;
	}

	public synchronized BitSet partsRemotePeerHas(int remotePeerID) {
        Peer remotePeer = getPeerWithPeerID(remotePeerID);
        if (remotePeer != null)
//        	TODO if below syntax can be changed
            return (BitSet) remotePeer.partsReceived.clone(); 
        return new BitSet();  
    }
	
	private Peer getPeerWithPeerID(int remoteID) {
		while(!otherPeers.isEmpty()) {
			for(Peer p : otherPeers) {
				if(p.getPeerID() == remoteID)
					return p;
			}
		}
		return null;
	}
	
	public synchronized void setInterestedForRemotePeer(int remoteID, boolean val) {
		Peer remotePeer = getPeerWithPeerID(remoteID);
		remotePeer.interested.set(val);
		System.out.println("interedted set for "+remotePeer.getPeerID());
	}
	
	public synchronized void setHaveOfRemotePeer(int remoteID, int partIndex) {
		Peer remotePeer = getPeerWithPeerID(remoteID);
		remotePeer.partsReceived.set(partIndex, true);
		updateFileDownlooadStatusOfNeighbors();
	}
	
	public synchronized void setBitfieldOfRemotePeer(int remoteID,BitSet bitfield) {
		Peer remotePeer = getPeerWithPeerID(remoteID);
		remotePeer.partsReceived = bitfield;
		updateFileDownlooadStatusOfNeighbors();
	}
	
	private synchronized void updateFileDownlooadStatusOfNeighbors() {
		for(Peer p : otherPeers) {
			System.out.println("bitfield of other = "+p.getPeerID()+" "+p.partsReceived);
			if(p.partsReceived.cardinality()< all)
				return;
		}
		System.out.println("setting that neighbors are complete");
		peerSetUp.NeighboursHaveCompleted();
	}

	public synchronized boolean canSend(int remoteID) {
		Peer remotePeer = getPeerWithPeerID(remoteID);
		System.out.println("preferred peers = "+preferredPeers);
		System.out.println("possible unchoke = "+optUnchoke.possiblyUnchokable);
		if(preferredPeers != null && optUnchoke.possiblyUnchokable!=null) {
			//UNSURE
			boolean can = preferredPeers.contains(remotePeer) || optUnchoke.possiblyUnchokable.contains(remotePeer);
			return can;
		}
		else
			return false;
	}
	
	public synchronized void partsReceived(int remoteID, int length) {
		Peer remotePeer = getPeerWithPeerID(remoteID); 
		remotePeer.downloadRate.addAndGet(length);
	}	
	
	synchronized boolean stillInterested(int remoteID, BitSet bitset) {
		Peer remotePeer = getPeerWithPeerID(remoteID);
        if (remotePeer != null) {
            BitSet pBitset = (BitSet) remotePeer.partsReceived.clone();
            pBitset.andNot(bitset);
            return ! pBitset.isEmpty();
        }
        return false;
    }
	
//	*********************************************************
	class OptimisticUnchoke implements Runnable{
		
		LinkedList<Peer> possiblyUnchokable = new LinkedList<Peer>();
		HashSet<Integer> optUnchokablePeerIDs = new HashSet<Integer>();
		SetupPeer sp;
		
		public OptimisticUnchoke(SetupPeer obj) {
			this.sp = obj;
		}
		
		public synchronized void setPossiblyUnchokable(LinkedList<Peer> interestedPeers) {
			possiblyUnchokable.clear();
			possiblyUnchokable = interestedPeers;
			System.out.println("possibly unchokable = "+possiblyUnchokable);
			optUnchokablePeerIDs = Peer.getPeerIds(possiblyUnchokable);
		}
		public void run(){
			try {
				Thread.sleep(Integer.parseInt(commonConfig.getProperty("OptimisticUnchokingInterval"))*1000);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
				
			if(!possiblyUnchokable.isEmpty() && possiblyUnchokable.size()>0) {
				Collections.shuffle(possiblyUnchokable);
				int peerSelected = possiblyUnchokable.get(0).getPeerID();
				Logging logrecord = new Logging();
				logrecord.changeOfOptimisticallyUnchokedNeighbors(peerSelected);
				if(sp.socketsList!=null) {
					Iterator<ComSupervisor> it = sp.socketsList.iterator();
					while(it.hasNext()) {
		                //System.out.println("to send unchoke in unoptimistic unchoke thread");
						ComSupervisor newComSupervisor = (ComSupervisor)it.next();
		                Collection<Integer> peersToUnchoke = new Vector<Integer>();
		                if(newComSupervisor.remotePeerId.get() == peerSelected) {
		                   peersToUnchoke.add(peerSelected);
		                }
		                peerSetUp.unchokePeers(peersToUnchoke);
		            }
				}
			}
		}
	}
//	******************************************************************************
}
