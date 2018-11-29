package peers;

public class Have extends Message{
  Have(byte[] pieceIndex){

    super("Have", pieceIndex);

  }

  public Have(int pieceIndex){

    this(getPieceIndexBytes(pieceIndex));

  }
}
