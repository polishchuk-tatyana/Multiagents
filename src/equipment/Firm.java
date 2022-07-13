package equipment;
import jade.core.Agent;
import jade.core.AID; 
import jade.core.behaviours.*; 
import jade.lang.acl.ACLMessage; 
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService; 
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription; 
import java.util.*;

public class Firm extends Agent{
	private ArrayList manufacturers; //список изготовителей
	private ArrayList productivities;//список производительности продукта от каждого изготовителя
	private ArrayList reliabilities;//список надежности продукта от каждого изготовителя
	//minimal level of productivity, which is required for the firm 
	private int minProductivity;
	//minimal level of reliability, which is required for the firm 
	private int minReliability;
	private int price;
	protected void setup() { 
		// Create the list of programmers
		manufacturers = new ArrayList(); 
		productivities = new ArrayList(); 
		reliabilities = new ArrayList(); 
		System.out.println("Hallo! Firm-agent "+getAID().getName()+" is ready."); 
		Object[] args = getArguments(); 
		if (args != null && args.length >=3) { 
			price = Integer.parseInt((String) args[0]); 
			minProductivity = Integer.parseInt((String) args[1]); 
			minReliability = Integer.parseInt((String) args[2]);
		}
		System.out.println("Price is "+ price);
		System.out.println("Productivity is "+ minProductivity); 
		System.out.println("Reliability is "+ minReliability);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID()); 
		ServiceDescription sd = new ServiceDescription(); 
		sd.setType("Firm"); sd.setName("JADE-man-firm");
		dfd.addServices(sd); 
		try {
			DFService.register(this, dfd);
			} 
		catch (FIPAException fe) {
			fe.printStackTrace();
			} 
		//get request from the manufacturer
		addBehaviour(new RequestsServer());
		//approve manufacturer request
		addBehaviour(new Approve()); 
	}
	
	// Put agent clean-up operations here 
	protected void takeDown() { 
		// Deregister from the yellow pages
		try { 
			DFService.deregister(this); 
			} 
		catch (FIPAException fe) { 
			fe.printStackTrace();
			} 
		// Printout a dismissal message 
		System.out.println("Firm-agent "+getAID().getName()+" terminating");
	}
	
	private class RequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt); 
			if (msg != null) { 
				// PROPOSE Message received. Process it
				int productivity = Integer.parseInt(msg.getContent()); 
//				int reliability = Integer.parseInt(msg.getContent()); here//
				ACLMessage reply = msg.createReply();
				//Check if productivity and reliability are OK for the firm
				//in condition
				if (productivity >= minProductivity ) { 
					boolean accept_flag = true; 
					if (accept_flag) {
						reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL); 
						reply.setContent(String.valueOf(price));
					}
					else { 
						reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
						reply.setContent("Already sold");
						}
				}
				else { 
					// The seniority is too low.
					reply.setPerformative(ACLMessage.REJECT_PROPOSAL); 
					reply.setContent("Parameters is too low");
					}
				myAgent.send(reply); 
			}
			else { 
				block();
			}
		}
	}
	
	private class Approve extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) { 
				// ACCEPT_PROPOSAL Сообщение получено. Обработать его
				ACLMessage reply = msg.createReply();
				if (!manufacturers.contains(msg.getSender())) { 
					manufacturers.add(msg.getSender());
					productivities.add(Integer.parseInt(msg.getContent()));
//					reliabilities.add(Integer.parseInt(msg.getContent())); here//
					} 
				reply.setContent(""); 
				reply.setPerformative(ACLMessage.INFORM); 
				myAgent.send(reply);
				System.out.println("Order from"+ ((AID) manufacturers.get(0)).getName() + " is done");
			}
			else {
				block(); 
				} 
		}
	}
}
