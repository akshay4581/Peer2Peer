package peers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

class OutputWriter extends DataOutputStream implements ObjectOutput{

	OutputWriter(OutputStream out) {
		super(out);
	}
	public void writeObject(Object obj){
	        if(obj instanceof Handshake) {
	        	System.out.println("in obj instance handshake block");
	        	((Handshake) obj).write(this);
	        }
			else
				try {
					((Message) obj).write(this);
				} catch (IOException e) {
					e.printStackTrace();
				}
	}
}
