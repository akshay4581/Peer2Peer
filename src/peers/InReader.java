package peers;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ObjectInput;

public class InReader extends DataInputStream implements ObjectInput {
	boolean isHandshakeReceived = false;

	public InReader(InputStream in) {
		super(in);

	}

	public Object readObject() {
		try {
			if (!isHandshakeReceived) {
				Handshake handshake = new Handshake();
				if (handshake.msgIsHandShake(this)) {
					isHandshakeReceived = true;
					System.out.println("handshake received successfully");
					return handshake;
				} else {
					System.out.println("handshake is not received properly");
				}

			} else {
				try {
					final int length = readInt();
					final int payloadLength = length - 1; // subtract 1 for the message type
					Byte b = readByte();
					System.out.println("Read byte == "+b);
					Message message = Message.getMessage(payloadLength, Message.getMesByByte(b));
					message.read(this);
					System.out.println("received message is "+message.getMesType());
					return message;
				} catch (Exception e) {
					e.printStackTrace();
					// System.exit(0);
				}
			}
		}

		catch (Exception E) {
			E.printStackTrace();
		}

		return null;
	}
}
