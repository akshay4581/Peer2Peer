package peers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
public class Handshake  {
    
	private String header = "P2PFILESHARINGPROJ";
    private byte[] zeroBits = new byte[10];
    private byte[] bytePeerId = new byte[4];
    
    
    //Default constructor which initializes the necessary variables
    public Handshake() {
    	
    }
    
    //The constructor which take in peer ID and sets this value into peer id byte array
    public Handshake(int peerId){
    	ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(peerId);
        bytePeerId = b.array();
    }

    public int getPeerId()
    {
        return ByteBuffer.wrap(bytePeerId).order(ByteOrder.BIG_ENDIAN).getInt();
    }
    
    public void read()
    {

        //validating just whether the handshake received is serialized or not
    }
    
    public void write(DataOutputStream out)
    {
        byte[] peerId = header.getBytes(Charset.forName("US-ASCII"));
       try{
        out.write (peerId, 0, peerId.length);
        out.write(zeroBits, 0, zeroBits.length);
        out.write(bytePeerId, 0, bytePeerId.length);

    }
    catch ( Exception e){
        e.printStackTrace();
    }
}

    public boolean msgIsHandShake(DataInputStream in) {


        try {
            byte[] header1 = new byte[header.length()];
            int read1 = in.read(header1, 0, header.length());
            int read2 = in.read(zeroBits, 0, zeroBits.length);
            int read3 = in.read(bytePeerId, 0, bytePeerId.length);
            if (new String(header1, "US-ASCII").equals(header) && read2 == zeroBits.length && read3 == bytePeerId.length) {
                return true;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }}



