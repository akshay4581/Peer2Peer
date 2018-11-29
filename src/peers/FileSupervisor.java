package peers;

import java.util.BitSet;
import java.util.Collections;
import java.util.Properties;

import Logging.Logging;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

import java.io.RandomAccessFile;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.io.IOException;


public class FileSupervisor {

	private int peerID;
	private boolean hasFile;

	private BitSet hasParts;
	private BitSet partsRequested;

	private String fileName;
	private int pieceSize;
	public int sizeOfbitset;
	private int fileSize;
	private String filePath;
	public String partsPath;

	//    public static String _actualFilePath;
	//    private BitSet _partsRequested;   // used for request message
	//    public static String _fileCreationPath;

	public SetupPeer setupPeer;
	public Message m;
	public Peer peer;

	public FileSupervisor(Peer p, Properties common, SetupPeer sp) {
		this.peer = p;
		this.peerID = peer.getPeerID();
		this.hasFile = peer.isHasFile();
		this.fileName = common.getProperty("FileName");
		this.pieceSize = Integer.parseInt(common.getProperty("PieceSize"));
		this.fileSize = Integer.parseInt(common.getProperty("FileSize"));
		this.setupPeer = sp;

		// size of bitSet is same as no of parts the file is divided into
		this.sizeOfbitset = (int) Math.ceil(((double)fileSize / (double)pieceSize));
		System.out.println("size of bitset = "+sizeOfbitset);
		hasParts = new BitSet(sizeOfbitset);
		this.partsRequested = new BitSet(sizeOfbitset);
		partsRequested.set(0, sizeOfbitset, false);

		filePath = "peer_" + peerID + "/files/"+fileName;
		partsPath = "peer_"+peerID + "/files/parts";
		//    	 if peer has file, divide the file into parts
		System.out.println("has file? "+hasFile);
		if (hasFile) {
			try {
				hasParts.set(0, sizeOfbitset, true);
				peer.partsReceived.set(0, sizeOfbitset, true);
				setupPeer.isFileCompleted.set(true);
				System.out.println("has parts = "+hasParts);
				splitFile(sizeOfbitset, filePath,partsPath);
			} catch (Exception e) {
				System.out.println("Exception occured in split_File: " + e);
			}
		}
		//    	otherwise create a folder to store the received parts
		else {
			hasParts.set(0, sizeOfbitset,false);
			File partsDirectory = new File(partsPath);
			partsDirectory.mkdirs();
		}
	}

	/*
	 * this function updates the hasParts bitset field, stores the received part in
	 * the parts folder and checks if file download is complete. If yes, it calls
	 * the merge files function to merge all parts
	 */

	public synchronized void addReceivedPart(int i, byte[] b) {
		if(i>=0 && i<hasParts.size() && !hasParts.get(i)) {
			hasParts.set(i, true);
			storeInDirectory(i, b,partsPath);
			// broadcast to other peers that it has this part now
			// this has to be communicated to setup peer because that has the connected peers list
			setupPeer.havePart(i);
			if (isFileDownloadComplete()) {
				try {
					mergeFiles(partsPath);
					setupPeer.FileHasCompleted();;
				} catch (Exception e) {
					System.out.println("Exception at mergeFiles: " + e);
					e.printStackTrace();
				}
			}
		}
	}

	//TODO remove this function
	public boolean isFileDownloadComplete() {
		for (int i = 0; i < sizeOfbitset; i++) {
			if (!hasParts.get(i))
				return false;
		}
		Logging logrecord = new Logging();
		logrecord.fileComplete();
		return true;
	}

	/*
	 * this function takes the byte array of the received part, creates the piece
	 * file and stores the file in the parts directory
	 */

	public void storeInDirectory(int index, byte[] b,String path) {
		File partFile = new File(partsPath+"/"+index + ".txt");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(partFile);
			setupPeer.havePart(index);
			fos.write(b);
		} catch (Exception e) {
			System.out.println("Exception occured at fos: " + e);
		} finally {
			try {
				fos.close();
			} catch (Exception e) {
				System.out.println("Exception at fos.close: " + e);
			}
		}

	}

	/*
	 * this function is called when a peer has the complete file. i.e, when hasFile
	 * field equals 1 it splits the original file into specified number of pieces
	 * for sharing with other peers
	 */

	public void splitFile(int sizeOfbitset, String filePath, String path) throws Exception {
		RandomAccessFile dataFile = new RandomAccessFile(filePath+".txt", "r");
		long numParts = sizeOfbitset; //16
		long wholeFileSize = dataFile.length(); //152138
		long bytesPerPiece = this.pieceSize; //9508
		long temp = (numParts-1)*bytesPerPiece;
		long remainingBytes =wholeFileSize-temp;
		//wholeFileSize % numParts; //10

		int maxReadBufferSize = 8 * 1024; // 8KB
		for (int n = 0; n < numParts-1; n++) {
			BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(path+"/"+ n));
			if (bytesPerPiece > maxReadBufferSize) {
				long totalReads = bytesPerPiece / maxReadBufferSize;
				long remainingReads = bytesPerPiece % maxReadBufferSize;
				for (int i = 0; i < totalReads; i++) {
					readWrite(dataFile, bw, maxReadBufferSize);
				}
				if (remainingReads > 0) {
					readWrite(dataFile, bw, remainingReads);
				}
			} else {
				readWrite(dataFile, bw, bytesPerPiece);
			}
			bw.close();
		}
		if (remainingBytes > 0) {
			BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(path+"/"+ (numParts-1)));
			readWrite(dataFile, bw, remainingBytes);
			bw.close();
		}
		dataFile.close();
	}

	public void readWrite(RandomAccessFile datafile, BufferedOutputStream bw, long byteBufferSize) throws IOException {
		byte[] byteBuffer = new byte[(int) byteBufferSize];
		int val = datafile.read(byteBuffer);
		if (val != -1) {
			bw.write(byteBuffer);
		}
	}

	/*
	 * this function is called when the peer has received all the parts. It checks
	 * the directory containing all the piece files and merges them into a single
	 * file based on file index
	 */

	public void mergeFiles(String partsPath) throws Exception {

		PrintWriter pw = new PrintWriter("datafile.txt");
		int numPartFiles = new File(partsPath).listFiles().length;
		for (int i = 0; i < numPartFiles; i++) {
			BufferedReader br = new BufferedReader(new FileReader(partsPath+"/"+i+".txt"));
			String line = br.readLine();
			while (line != null) {
				pw.println(line);
				line = br.readLine();
			}
			br.close();
		}
		pw.flush();
		pw.close();
	}

	public BitSet partsWithPeer() {
		return (BitSet)hasParts.clone();
	}

	public synchronized int selectPartToRequest(BitSet bitfieldOfRemotePeer) {
		if(!bitfieldOfRemotePeer.isEmpty()) {

		}
		bitfieldOfRemotePeer.andNot(partsWithPeer()); //remove already has parts
		bitfieldOfRemotePeer.andNot(partsRequested); //remove already requested parts

		List<Integer> partsToRequest = new ArrayList<Integer>();
		for (int i = bitfieldOfRemotePeer.nextSetBit(0); i >= 0; i = bitfieldOfRemotePeer.nextSetBit(i+1)) {
			partsToRequest.add(i);
		}
		if(partsToRequest.size()!=0) {
			Collections.shuffle(partsToRequest);
			int selected = partsToRequest.get(0); 
			partsRequested.set(selected);

			new java.util.Timer().schedule(
					new java.util.TimerTask() {
						@Override
						public void run() {
							synchronized (partsRequested) {
								partsRequested.clear(selected);
								//  LogHelper.getLogger().debug("clearing requested parts for pert " + partId);
							}
						}
					},
					3000
					);
			return selected;
		}
		return -1;
	}

	public byte[] sendRequestedPart(int part,String path) {

		if(part>=0 && part<hasParts.size() && hasParts.get(part) && path.trim().compareTo("")!=0) {
			ByteBuffer b = ByteBuffer.allocate(4);
			//TODO check if necessary b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
			b.putInt(part);
			byte[] partIndexByteArray = b.array();

			File partFile = new File(path+"/"+Integer.toString(part));
			System.out.println("part file being sent = "+partFile.getName());
			FileInputStream in = null;
			try {
				in = new FileInputStream(partFile);
				byte[] fileRelated = new byte[(int)partFile.length()];
				byte[] ans = new byte[fileRelated.length+4];
				in.read(fileRelated);
				for(int i=0;i<4;i++) {
					ans[i] = partIndexByteArray[i];
				}
				for(int i=4;i<ans.length;i++) {
					ans[i] = fileRelated[i-4];
				}
				System.out.println("sample content ="+ans[10]);
				return ans;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}		
		return null;		
	}


}