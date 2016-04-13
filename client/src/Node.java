import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rasheed
 * This class represents the client nodes and handles sending messages to the server.
 */
public class Node implements Runnable {

	public static final int NUMBER_OF_NODES = 7;
	protected int nodeId;
	protected int nodeType; //node type (0=normal 1=initiator, 2=faulty, 3=faulty+initiator)
	protected int currentValue;
	protected int round;
	private int initiatorValue;
	private int  sourceNode;
	private String messageText;
	private List<Integer> values = new ArrayList<>();
	private int initiatorProcess;



	public Node() {}
	
	public Node(int nodeId, int nodeType) {
		this.nodeId = nodeId;
		this.nodeType = nodeType;
	}

	public static void main(String[] args) {

		//select number of faulty machines 1 or 2
		Random randomGenerator = new Random();
		ArrayList<Integer> faultyProcesses= new ArrayList<>();
		int initiator = randomGenerator.nextInt(7);
		System.out.println("Initiator Process is: " + initiator);

		//should be faultyProcessSelection method
		//select which of the 7 machines will be faulty
		int numberOfFaultyProcesses = randomGenerator.nextInt(2) + 1;
		if (numberOfFaultyProcesses == 1) {

			faultyProcesses.add(randomGenerator.nextInt(7));

		}
		else {
			int firstMachine = randomGenerator.nextInt(7);
			int secondMachine = randomGenerator.nextInt(7);

			while (firstMachine == secondMachine) {
				secondMachine = randomGenerator.nextInt(7);
			}

			faultyProcesses.add(firstMachine);
			faultyProcesses.add(secondMachine);
		}


		System.out.println("Number of faulty processes: " + numberOfFaultyProcesses);
		System.out.println("Faulty Processes: ");
		for (int i : faultyProcesses) {
			System.out.println("Process: " + i + " ");
		}

		//create thread pool and start the 10 nodes
		ExecutorService pool = Executors.newFixedThreadPool(NUMBER_OF_NODES);
		for(int i = 0; i < NUMBER_OF_NODES; i++) {
			int type = 0;
			if (i == initiator) {
				type = 1;
			}
			if (faultyProcesses.contains(i)) {
				type = 2;
			}
			pool.execute(new Node(i, type));
		}
		pool.shutdown(); //shutdown pool after nodes terminate
	}

	@Override
	public void run() {

		try (
				Socket nodeSocket = new Socket("localhost", 4499);
				PrintWriter out = new PrintWriter(nodeSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
		) {
			//start message handler thread for reading incoming messages
//			Thread handler = new Thread(new MessageHandler(in, nodeId));
//			handler.start();

			//send initial identifier message to Server
			out.println(nodeId);
			Thread.sleep(2000);  //pause for other nodes to connect


			if (round == 0) {
				if (nodeType == 1) {
					initiatorProcess = nodeId;
				}
				if (nodeType == 1 || nodeType == 3) {
					for (int i = 0; i < 7; i++) {
						if (nodeId != i) {
							initiatorValue = 1;
							System.out.println("PROCESS #" + nodeId + " is sending " + initiatorValue + " to process " + i + " ROUND# " + round);
							/**
                             * Message format SourceProcess:DestinationProcess:Value
							 */
							out.println(nodeId + ":" + i + ":" + initiatorValue);

						}
					}
				}

				Thread.sleep(5000);
				out.println(nodeId + ":EOR");

				readMessages(in, 0);
				System.out.println("Thread: " + nodeId + " Out of READ");
//				Thread.sleep(3000);

			}
			Thread.sleep(5000);
			this.round++;
			om(out, in);
			Thread.sleep(5000);
			System.out.println("PROCESS " + nodeId + " VALUES SIZE: " + values.size());

			for (int v: values) {
				System.out.print(v + " ");
			}

			out.println(nodeId + ":" + -1 + ":FINISHED");
//			handler.join();
		} catch (IOException e) {
			e.printStackTrace();

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void om(PrintWriter out, BufferedReader in) throws InterruptedException {
		//m is number of faulty processes 2
		int m = 2;
		Random rand = new Random();
		while(m > 0) {

			for (int i = 0; i < 7; i++) {
//				System.out.println("IN FOR LOOP: i = " + i + "nodeId = " + nodeId);
				//send to every process excluding itself and initiator
//				System.out.println("Initiator process = " + initiatorProcess);
				if (i != nodeId &&  i != initiatorProcess) {
					if (nodeType == 2) {
						int maliciousValue = rand.nextInt(2);

						/**
                         * Message format SourceProcess:DestinationProcess:Value
                         */
						out.println(nodeId + ":" + i + ":" + maliciousValue);
						System.out.println("PROCESS# " + nodeId + " is sending " + maliciousValue + " to process " + i + " ROUND# " + round);

					}
					else {
						System.out.println("PROCESS# " + nodeId + " is sending " + initiatorValue + " to process " + i + " ROUND# " + round);

						/**
						 * Message format SourceProcess:DestinationProcess:Value
						 */
						out.println(nodeId + ":" + i + ":" + initiatorValue);
					}
				}
			}
			Thread.sleep(5000);
			out.println(nodeId + ":EOR");

			readMessages(in, m);
			Thread.sleep(5000);
			m--;
			this.round++;
			System.out.println("GOING FOR NEXT ROUND!!!!");

		}

		System.out.println("Process " + nodeId + "\t" + initiatorValue);


	}

	private void readMessages(BufferedReader in, int m) {
//		System.out.println("IN READ MESSAGES m = " + m);
		String incommingMessage;
		try {
			while (!(incommingMessage = in.readLine()).equals("EOR")) {
//			incommingMessage = in.readLine();
				/**
				 * message from server is in the following format:
				 * message:SourceProcess
				 */
				//parsing the incoming message
				messageText = incommingMessage.split(":")[0];

//				if(messageText.equals("TERMINATE")){
//					break; //break out of loop and terminate thread after server sends TERMINATE message
//				}

				initiatorValue = Integer.parseInt(messageText);
				values.add(initiatorValue);
				if(m == 1) { //this is for round m - 1, need to change to dynamic

					//take the majority as the initiator value
					initiatorValue = majority();

				}

				sourceNode = Integer.parseInt(incommingMessage.split(":")[1]);


				System.out.println("PROCESS# " + nodeId + " receiving message '" + messageText + "' from PROCESS# " + sourceNode + " ROUND# " + round);
				//System.out.println("Node: " + nodeId + " initiator value = " + initiatorValue);
			}
//			System.out.println(nodeId + " terminated");
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	}

	private int majority() {
		Collections.sort(values);
		return values.get(values.size()/2);
	}
}
