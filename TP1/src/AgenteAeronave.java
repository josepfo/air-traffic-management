package Agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class AgenteAeronave extends Agent{
	
	private double coordX;
	private double coordY;
	private double destCoordX;
	private double destCoordY;
	private double zonaProtegida;
	private double zonaAlerta;
	private int nrPassageiros;
	private double direcaoX;
	private double direcaoY;
	private double velocidade;
	private double distPercorrer;
	private double distPercorrida;
	private String destino;
	private String origem;
	
	protected void setup() {
		super.setup();
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName());
		sd.setType("Plane");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		//coordX e coordY inicializa-se quando pede cond. meteo. à estação local
		//destCoordX e destCoordY inicializa-se quando pede cond. meteo. à estação destino
		zonaProtegida=50;
		zonaAlerta=250;
		//nrPassageiros pode-se dar no container?
		nrPassageiros=150;
		//direcaoX e direcaoY preciso dos coordX e coordY
		//velocidade pode-se dar no container?
		velocidade=200;
		//distPercorrer depende das coords e das destCoords
		distPercorrida=0;
		//origem e destino pode-se dar no container?
		//Lista de aeroportos?
		
		
		addBehaviour(new EnviarCoords());
		addBehaviour(new Movimento(this, 1000));
	}
	
	private class Movimento extends TickerBehaviour {
		public Movimento(Agent a, long period) {
			super(a, period);
		}
		protected void onTick() {
			coordX=coordX+velocidade*direcaoX;
			coordY=coordY+velocidade*direcaoY;
			distPercorrida+=velocidade;
		}
	}
	
	private class EnviarCoords extends CyclicBehaviour {
		public void action() {
			try {
				DFAgentDescription dfd = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType("Plane");
				dfd.addServices(sd);
				DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
				if (results.length > 0) {
					for (int i = 0; i < results.length; ++i) {
						DFAgentDescription dfd2 = results[i];
						AID provider = dfd2.getName();
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(provider);
						msg.setContent(coordX+";"+coordY+";"+direcaoX+";"+direcaoY+";"+distPercorrer);
						send(msg);
					}
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}
	}
	
	private class ReceberCoords extends CyclicBehaviour{
		public void action() {
			ACLMessage msg = receive();
			if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
				String[] coordsOutro = msg.getContent().split(";");
				double coordXOutro=Double.parseDouble(coordsOutro[0]);
				double coordYOutro=Double.parseDouble(coordsOutro[1]);
				double dist = Math.sqrt(((Math.pow((coordXOutro - coordX), 2)) + (Math.pow((coordYOutro - coordY), 2))));;
				if((coordXOutro!=coordX || coordYOutro!=coordY) && (dist<=zonaAlerta)) {
					double dirXOutro=Double.parseDouble(coordsOutro[2]);
					double dirYOutro=Double.parseDouble(coordsOutro[3]);
					double ang = Math.acos((direcaoX*dirXOutro+direcaoY*dirYOutro)/((Math.sqrt(dirXOutro*dirXOutro+dirYOutro*dirYOutro))*(Math.sqrt(direcaoX*direcaoX+direcaoY*direcaoY))));
					if(Math.abs(dirXOutro)==direcaoX && Math.abs(dirYOutro)==direcaoY) {
						double rx = (direcaoX * Math.cos(Math.toRadians(20))) - (direcaoY * Math.sin(Math.toRadians(20)));
					    double ry = (direcaoX * Math.sin(Math.toRadians(20))) + (direcaoY * Math.cos(Math.toRadians(20)));
					    direcaoX = rx;
					    direcaoY = ry;
					    try {
							DFAgentDescription dfd = new DFAgentDescription();
							ServiceDescription sd = new ServiceDescription();
							sd.setType(destino);
							dfd.addServices(sd);
							DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
							if (results.length > 0) {
								for (int i = 0; i < results.length; ++i) {
									DFAgentDescription dfd2 = results[i];
									AID provider = dfd2.getName();
									ACLMessage msg2 = new ACLMessage(ACLMessage.INFORM);
									msg2.addReceiver(provider);
									msg2.setContent(coordX+";"+coordY+";"+direcaoX+";"+direcaoY+";"+distPercorrer);
									send(msg2);
								}
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}
					}else if(ang==0 || ang==Math.PI) {
						double distOutro=Double.parseDouble(coordsOutro[4]);
						if(distOutro>distPercorrer) {
							velocidade+=50;
							try {
								DFAgentDescription dfd = new DFAgentDescription();
								ServiceDescription sd = new ServiceDescription();
								sd.setType(destino);
								dfd.addServices(sd);
								DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
								if (results.length > 0) {
									for (int i = 0; i < results.length; ++i) {
										DFAgentDescription dfd2 = results[i];
										AID provider = dfd2.getName();
										ACLMessage msg3 = new ACLMessage(ACLMessage.INFORM);
										msg3.addReceiver(provider);
										msg3.setContent(coordX+";"+coordY+";"+direcaoX+";"+direcaoY+";"+distPercorrer);
										send(msg3);
									}
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
						} else {
							velocidade-=50;
							try {
								DFAgentDescription dfd = new DFAgentDescription();
								ServiceDescription sd = new ServiceDescription();
								sd.setType(destino);
								dfd.addServices(sd);
								DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
								if (results.length > 0) {
									for (int i = 0; i < results.length; ++i) {
										DFAgentDescription dfd2 = results[i];
										AID provider = dfd2.getName();
										ACLMessage msg4 = new ACLMessage(ACLMessage.INFORM);
										msg4.addReceiver(provider);
										msg4.setContent(coordX+";"+coordY+";"+direcaoX+";"+direcaoY+";"+distPercorrer);
										send(msg4);
									}
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	protected void takeDown() {
		super.takeDown();
	}
}
