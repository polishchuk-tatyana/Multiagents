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
import jade.domain.FIPAAgentManagement.SearchConstraints; 

public class Manufacturer extends Agent {
	
	//announcement variables
	private Integer productivity;//продуктивность
	private Integer reliability;//надежность
	private AID[] firmAgents;
	private boolean isParticipated = false;
	
	//initialization agent
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hallo! Manufacturer-agent "+getAID().getName()+" is ready.");
		// Get the productivity and reliability as start-up arguments
		Object[] args = getArguments(); 
		if (args != null && args.length >= 2) { 
			productivity = Integer.parseInt((String) args[0]); 
			reliability = Integer.parseInt((String) args[1]); 
			//add a TickerBehaviour that schedules a request to firm agents every 30 seconds
			addBehaviour(new TickerBehaviour(this, 60000) { 
				protected void onTick() {
					if (!isParticipated) { 
					System.out.println("Waiting for firm-buyer with productivity - "+ productivity + "and reliability - "+reliability);
					// Update the list of project agents 
					DFAgentDescription template = new DFAgentDescription(); 
					ServiceDescription sd = new ServiceDescription(); 
					sd.setType("Firm"); template.addServices(sd); 
					try { 
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following firm agents:");
						firmAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							firmAgents[i] = result[i].getName(); 
							System.out.println(firmAgents[i].getName());
							}
						}
					catch (FIPAException fe) { 
						fe.printStackTrace();
						}
					myAgent.addBehaviour(new RequestPerformer());
					}
				}
			});
			addBehaviour(new GetOrderResponse());
		}
		else 
		{ 
			// Make the agent terminate 
			System.out.println("Manufacturer-agent "+getAID().getName()+" was refused"); isParticipated = false;
			doDelete();
			} 
	}
	private class GetOrderResponse extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE); 
			ACLMessage msg = myAgent.receive(mt); 
			if (msg != null) { 
				System.out.println("Manufacturer-agent "+getAID().getName()+" was refused");
				} 
			block();
			} }
	// Put agent clean-up operations here
	protected void takeDown() { 
		// Printout a dismissal message
		System.out.println("Manufacturer-agent "+getAID().getName()+" terminating.");
		}
	
	private class RequestPerformer extends Behaviour { 
		private AID firm;
		private int bestPrice;
		// The counter of replies from firm agents
		private int repliesCnt = 0; 
		// The template to receive replies
		private MessageTemplate mt; 
		private int step = 0;
		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all projects
				ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
				for (int i = 0; i < firmAgents.length; ++i) {
					cfp.addReceiver(firmAgents[i]);
					} 
				cfp.setContent(productivity.toString());
				//cfp.setContent(reliability.toString()); here//
				cfp.setConversationId("Manufact-Firm");
				cfp.setReplyWith("cfp"+System.currentTimeMillis());
				myAgent.send(cfp);
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Manufact-Firm"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith())); 
				step = 1; break;
			case 1: 
				// Receive all proposals/refusals from firm agents 
				ACLMessage reply = myAgent.receive(mt); 
				if (reply != null) {
					//Firm satisfied with this manufacturer
					//Фирма довольна изготовителем
					if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
						int price = Integer.parseInt(reply.getContent()); 
						if (firm == null || price >= bestPrice) {
							// This is the best offer at present
							bestPrice = price;
							firm = reply.getSender();
							}
					}
					repliesCnt++; 
					if (repliesCnt >= firmAgents.length) {
						// We received all 	replies 
						step = 2; 
						}
				}
				else { 
					block();
					}
				break;
			case 2:
				// send the request one firm that accept proposal
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL); 
				order.addReceiver(firm); 
				order.setContent(productivity.toString()); 
//				order.setContent(reliability.toString());  here//
				order.setConversationId("Manufact-Firm"); 
				order.setReplyWith("order"+System.currentTimeMillis()); 
				myAgent.send(order); 
				// Prepare the template to get the purchase order reply 
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Manufact-Firm"), MessageTemplate.MatchInReplyTo(order.getReplyWith())); 
				step = 3;
				break; 
			case 3: 
				//receive reply from firm
				reply = myAgent.receive(mt);
				if (reply != null) { 
					if (reply.getPerformative() == ACLMessage.INFORM) 
					{ 
						// Selling successful. We can terminate
						System.out.println(reply.getSender().getName() + " has bought product of "+getAID().getName()); 
						isParticipated = true;
					}
					else 
					{ 
						System.out.println("Attempt failed: already bought" + reply.getSender().getName());
					} 
					step = 4;
				}
				else
				{ 
					block();
					} 
				break;
			}
		}
		public boolean done() {
			if (step == 2 && firm == null) {
				System.out.println("Attempt failed: "+ productivity +" and " + reliability +" is too small for firm order");
				}
			return ((step == 2 && firm == null) || step == 4);
		}
	}
}
