package peers;

import java.util.HashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;

public class Message{
  public int mLen;
  public byte mType;
  public byte[] payLoad;

  final static Byte[] valueOfType = {0,1,2,3,4,5,6,7};
  final static HashMap<String, Byte> map =  new HashMap<String, Byte>();

  static{

	map.put("Choke", valueOfType[0]);
	map.put("Unchoke", valueOfType[1]);
    map.put("Interested", valueOfType[2]);
    map.put("Notinterested", valueOfType[3]);
    map.put("Have", valueOfType[4]);
    map.put("BitField", valueOfType[5]);
    map.put("Request", valueOfType[6]);
    map.put("Piece", valueOfType[7]);


  }

  public String getMesType(){
    return getMesByByte(this.mType);
  }

  protected Message(String type){

    this(type, null);

  }
  Message(String type, byte[] payload){
    if(payload == null){
    	mLen = 1;
    }
    else if(payload.length == 0){
    	mLen = 1;
    }
    else{
    	mLen = payload.length + 1;
    }

    mType = map.get(type);

    this.payLoad = payload;
    if(type == "Piece")
    	System.out.println("Piece message has payload = "+(mLen-1));
  }

  Byte getTypeOfMes(String type){

    return map.get(type);

  }

  public static String getMesByByte(byte a){
    String key = "";

    for(Map.Entry<String, Byte> entry : map.entrySet()){
      if (entry.getValue() == a){
        key = entry.getKey();
        break;
      }
    }
//    System.out.println("key retrieved = "+key);
    return key;
  }

  public static Message getMessage(int length, String type){
    switch(type){

      case "Choke":
        return new Message("Choke");

      case "Unchoke":
    	 return new Message("Unchoke");

      case "Notinterested":
    	  return new Message("Notinterested");

      case "Interested":
    	Message m = new Message("Interested");
    	return m;

      case "BitField":
      {
        if(length > 0){
          return new BitField(new byte[length]);
        }
        else{
          return new BitField(new byte[0]);
        }
      }

      case "Have":
        return new Have(new byte[length]);

      case "Request":
        return new Request(new byte[length]);

      case "Piece":
    	return new Piece (new byte[length]);

      default:
    	Message m1 = new Message("Interested");
      	return m1;
    }
  }

  public void read(DataInputStream in) throws IOException{
    if((payLoad != null) && (payLoad.length) > 0){

      in.readFully(payLoad, 0, payLoad.length);

    }
  }

  public void write(DataOutputStream out) throws IOException{
    out.writeInt(mLen);
    out.writeByte(mType);

    if(payLoad != null){
      out.write(payLoad, 0, payLoad.length);

    }
  }

  public static byte[] getPieceIndexBytes(int pieceIndex){

    return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(pieceIndex).array();

  }

  public int getPieceIndex(){

    return ByteBuffer.wrap(Arrays.copyOfRange(payLoad, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();

  }
}
