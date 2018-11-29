package peers;

public class Request extends Message{

  public Request(byte[] pieceIndex){

    super("Request", pieceIndex);

  }
  public Request(int pieceIndex){

    this(getPieceIndexBytes(pieceIndex));

  }
}
