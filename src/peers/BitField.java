package peers;

public class BitField extends Message{

  public BitField(byte[] bitfield){

    super("BitField", bitfield);

  }
}
