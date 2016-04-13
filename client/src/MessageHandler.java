import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by Matthew Beck on 3/8/16.
 * This class handles incoming messages from the server
 */
public class MessageHandler extends Node implements Runnable {
	private BufferedReader in;
	private int nodeId, sourceNode, sourceClock;
	private String messageText;
	
	public MessageHandler(BufferedReader in, int nodeId) {
		this.in = in;
		this.nodeId = nodeId;
	}
	
	@Override
	public void run() {
		
		String incommingMessage;
		try {
			while ((incommingMessage = in.readLine()) != null ) {
				/**
				 * message from server is in the following format:
				 * MESSAGE:SOURCE
				 */
				//parsing the incoming message
				messageText = incommingMessage.split(":")[0];
				if(messageText.equals("TERMINATE")){
					break; //break out of loop and terminate thread after server sends TERMINATE message
				}
				sourceNode = Integer.parseInt(incommingMessage.split(":")[1]);

				System.out.println("PROCESS #" + nodeId + " receiving message '" + messageText + "' from PROCESS #" + sourceNode);
			}
			System.out.println(nodeId + " terminated");
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}
}	
