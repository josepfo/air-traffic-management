import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.*;
import java.util.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AgenteAeronave extends Agent{
	
	private String AEOrigem;
	private double coordX;
	private double coordY;
	private String AEDestino; 
	private double destCoordX;
	private double destCoordY;
	private double zonaProtegida;
	private double zonaAlerta;
	private int nrPassageiros;
	private double direcaoX;
	private double direcaoY;
	private double velocidade;
	private double distTotalPercorrer;
	private double distPercorrida;
	private String name = "";
	// variaveis para identificar se já recebeu confirmação de ambas as estações para iniciar viagem
	private boolean autorizacaoPartida;
	private boolean autorizacaoChegada;
	// variavel para identificar quando entra dentro da area de proximidade da estação destino
	private boolean dentroDaAP;
	@Override
	protected void setup() {
		
		this.zonaProtegida = 2500;
		this.zonaAlerta = 1000;
		this.name = this.getLocalName();
		this.autorizacaoChegada = false;
		this.autorizacaoPartida = false;
	
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName());
		sd.setType(getLocalName());
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException e) {
			
			e.printStackTrace();
		}
		
		initAeronave();
		this.addBehaviour(new autorizacaoDescolar());
		this.addBehaviour(new pedidoDescolagem());
		// duvida - ao lançar o autorizacaoDescolar só quero que o movingAeronave comece quando este terminar, como fazer?
		
		//this.addBehaviour(new movingAeronave(this,100));
		// caso esteja dentro da AP pede para fazer aterragem
		if(dentroDaAP) {
			this.addBehaviour(new pedidoAterragem());
		}
	}
	
	private class pedidoAterragem extends OneShotBehaviour{
		@Override
		public void action() {
			
			try {
				
				DFAgentDescription dfd = new DFAgentDescription();
				ServiceDescription sd = new ServiceDescription();
				sd.setType(AEOrigem);
				dfd.addServices(sd);
				
				DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
				
				
				if(results.length == 1) {
					AID provider = results[0].getName();
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
					msg.addReceiver(provider);	
					msg.setContent("PEDIDOATERRAGEM");
					send(msg);
					System.out.println(name+" pediu para efetuar aterragem "+AEOrigem);
					
					//TODO -> implementar no comportamento que está a receber mensagens a reação da estação a este request
				
				}
				
			}catch(FIPAException e) {
			    	e.printStackTrace();
			    }
		  }
			
		
	}
	
	
	private class pedidoDescolagem extends OneShotBehaviour{
		@Override
		public void action() {
			
			try {
			
			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(AEOrigem);
			dfd.addServices(sd);
			
			DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
			
			//pedido feito ao aeroporto local
			if(results.length == 1) {
				AID provider = results[0].getName();
				ACLMessage msgLocal = new ACLMessage(ACLMessage.REQUEST);
				msgLocal.addReceiver(provider);	
				msgLocal.setContent("PEDIDOSAIDA");
				send(msgLocal);
				System.out.println(name+" pediu para usar pista a "+AEOrigem);
			}
			
			
			DFAgentDescription dfd1 = new DFAgentDescription();
			ServiceDescription sd1 = new ServiceDescription();
			sd1.setType(AEDestino);
			dfd1.addServices(sd1);
			
			DFAgentDescription[] results1 = DFService.search(this.myAgent, dfd1);
			
			
			
			
			//pedido feito ao aeroporto destino
			if(results.length == 1) {
				AID provider1 = results1[0].getName();
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.addReceiver(provider1);	
				msg.setContent("PEDIDOENTRADA");
				send(msg);
				System.out.println(name+" pediu para iniciar viagem a "+AEDestino);
			}
			
			}catch (FIPAException e) {
				
				e.printStackTrace();
			}
			
			
			
		}
	}
	
	//comportamento que vai receber os confirms dos dois aeroportos para começar uma viagem
	private class autorizacaoDescolar extends Behaviour {
		private int endFlag = 0;
		
		@Override
		public void action() {
		
			// caso seja verificada as duas autorizações este comportamento termina! 
			if(autorizacaoPartida && autorizacaoChegada) {
				try {
					
					
				/* Enviar um inform ao aeroporto para o AE definir prioridades */
					DFAgentDescription dfd = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(AEDestino);
					dfd.addServices(sd);
					
					DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
					
					if(results.length == 1) {
						
						AID provider = results[0].getName();
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(provider);	
						msg.setContent("INICIARVIAGEM"+";"+name+";"+coordX+";"+coordY+";"+nrPassageiros+";"+direcaoX+";"+direcaoY+";"+velocidade);
						send(msg);
						System.out.println(name+" A INICIAR VIAGEM PARA "+provider);
						
					}
					endFlag = 1;
					//autorizacaoPartida = false;
					//autorizacaoChegada = false;
					
					
				}catch(FIPAException e) {
					e.printStackTrace();
				}
			 }
			
			ACLMessage msg = receive();
			
			if (msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {
				
				
				
				if(msg.getContent().equals("AUTORIZA-SAIDA"))
					autorizacaoPartida = true;
				
						
				if( msg.getContent().equals("AUTORIZA-ENTRADA"))
					autorizacaoChegada = true;
				
			}
		}
		
		
		public boolean done() {
			if(endFlag == 1)
				return true;
			else return false;
		}
		
	}
	
	private class movingAeronave extends TickerBehaviour {
		public movingAeronave(Agent a, long tempo) {
			super(a,tempo);
		}
		
		@Override
		protected void onTick() {
			
			if (destCoordX > coordX && destCoordY > coordY) {
				coordX += velocidade;
				coordY += velocidade;
			}
			if (destCoordX < coordX && destCoordY > coordY) {
				coordX -= velocidade;
				coordY += velocidade;
			}
			if (destCoordX > coordX && destCoordY < coordY) {
				coordX += velocidade;
				coordY -= velocidade;
			}
			if (destCoordX < coordX && destCoordY < coordY) {
				coordX -= velocidade;
				coordY -= velocidade;
			}
			if (destCoordY > coordY)
				coordY += velocidade;
			if (destCoordY < coordY)
				coordY -= velocidade;
			if (destCoordX > coordX)
				coordX += velocidade;
			if (destCoordX < coordX)
				coordX -= velocidade;
			
			if(!dentroDaAP) {
				//se a distancia que falta percorrer for menor ou igual que a zona de aproximação
				if(Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2)) <= zonaProtegida){
					dentroDaAP = true;
				}
			}
			
			if(destCoordX == coordX && destCoordY == coordY) {
				System.out.println(name + " chegou ao destino");
				stop();
			}
			
			//distPercorrida = distTotalPercorrer - (Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2)));
			System.out.println(name+" " + "X " + coordX+ "Y " +coordY);
			//System.out.println("Distância percorrida" +distPercorrida);
		}
	}
		
	

	private void initAeronave() {
		
		switch(this.getLocalName()) {
		case "Aeronave1":
			AEOrigem = "Estacao1";
			coordX = -10000;
			coordY = -500;
			AEDestino = "Estacao2";
			destCoordX = 10000;
			destCoordY = 500;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			break;
			
		case "Aeronave2":
			AEOrigem = "Estacao1";
			coordX = -10000;
			coordY = 500;
			AEDestino = "Estacao3";
			destCoordX = 10000;
			destCoordY = 0;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			break;
			
		case "Aeronave3":
			AEOrigem = "Estacao2";
			coordX = 10000;
			coordY = 500;
			AEDestino = "Estacao1";
			destCoordX = -10000;
			destCoordY = -500;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			break;
		
		case "Aeronave4":
			AEOrigem = "Estacao2";
			coordX = 10000;
			coordY = 500;
			AEDestino = "Estacao3";
			destCoordX = 10000;
			destCoordY = 0;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			break;
		
		case "Aeronave5":
			AEOrigem = "Estacao4";
			coordX = 0;
			coordY = 11000;
			AEDestino = "Estacao1";
			destCoordX = -10000;
			destCoordY = -500;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			break;
			
		case "Aeronave6":
			AEOrigem = "Estacao4";
			coordX = 0;
			coordY = 11000;
			AEDestino = "Estacao3";
			destCoordX = 10000;
			destCoordY = 0;
			nrPassageiros = 250;
			velocidade = 100;
			distTotalPercorrer = Math.sqrt(Math.pow(destCoordX - coordX, 2) + Math.pow(destCoordY - coordY, 2));
			distPercorrida = 0;
			
		}
	}
	
	
	
	
	@Override
	protected void takeDown() {
		super.takeDown();
		
		try {
			DFService.deregister(this);
		}catch(FIPAException e) {
			e.printStackTrace();
		}
	}

	//private class ReceberCoordsComp extends CyclicBehaviour{
		
	//}
}
