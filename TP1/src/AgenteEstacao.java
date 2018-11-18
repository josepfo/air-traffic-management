import java.util.*;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class AgenteEstacao extends Agent{

	private double coordX;
	private double coordY;
	
	private int totalPistas;
	private int nrPistasOcupadas;
	private int totalEstacionamentos;
	private int nrEstOcupados;
	
	private double zonaProtegida;
	private double zonaAlerta;
	
	// condi��es meteorologicas favoraveis ou n�o
	private boolean condMeteo;
	
	// lista de prioridades de aeronaves em viagem para esta esta��o
	private List<String> listaPrioridade;
	
	// lista de espera para iniciar viagem para esta esta��o
	private List<String> listpedidosChegada;
	
	// list de espera para iniciar viagem com sa�da nesta esta��o
	private List<String> listpedidosSaida;
	
	private String name = "";
	
	
	
	@Override
	protected void setup() {
			
			this.listaPrioridade = new ArrayList<>();
			this.listpedidosSaida = new ArrayList<>();
			this.listpedidosChegada = new ArrayList<>();
			this.zonaProtegida = 2500;
			this.zonaAlerta = 1000;
			this.name = this.getLocalName();
			this.totalPistas = 5;
			this.nrPistasOcupadas = 0;
			this.totalEstacionamentos = 5;
			this.nrEstOcupados = 0;
			
	
			
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
			
			
			initEstacao();
			// 2 em 2 segs
			this.addBehaviour(new verificarListasEspera(this, 2000));
			this.addBehaviour(new receberPedidos());
			
		
	}
	
	
	private class receberPedidos extends CyclicBehaviour {
		public void action() {
			
			ACLMessage msg = receive();
			
			if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
				
				String aeronave = msg.getSender().getLocalName();
				ACLMessage resp = msg.createReply();
				String pedido = msg.getContent();
				switch(pedido) {
				// recebido de um AA que esteja neste aeroporto
				case "PEDIDOSAIDA":
					if(condMeteo && nrPistasOcupadas < totalPistas) {
						nrEstOcupados--;
						resp.setContent("AUTORIZA-SAIDA");
						resp.setPerformative(ACLMessage.CONFIRM);
						send(resp);
						System.out.println(name +" autorizou uso da pista a " + aeronave);
					}else {
						// caso nao hajam cond no aeroporto origem � adicionado � lista de aeronaves para sair
						listpedidosSaida.add(aeronave);
						resp.setContent("NAO-AUTORIZA-SAIDA");
						resp.setPerformative(ACLMessage.DISCONFIRM);
						send(resp);
						System.out.println(name +" n�o autorizou uso da pista a " + aeronave);
					}
				break;
				//recebido de um AA que queira vir para este aeroporto
				case "PEDIDOENTRADA":
					if((nrEstOcupados < totalEstacionamentos) && condMeteo) {
						nrEstOcupados++;
						resp.setContent("AUTORIZA-ENTRADA");
						resp.setPerformative(ACLMessage.CONFIRM);
						send(resp);
						System.out.println(name +" autorizou inicio de viagem a " + aeronave);
					}else {
						// caso nao hajam cond no aeroporto destino � adicionada � lista de aeronaves para chegar
						listpedidosChegada.add(aeronave);
						resp.setContent("NAO-AUTORIZA-ENTRADA");
						resp.setPerformative(ACLMessage.DISCONFIRM);
						send(resp);
						System.out.println(name +" n�o autorizou inicio de viagem a " + aeronave);
					}
				break;
				case "PEDIDOATERRAGEM":
					 // verificar se � o primeiro na lista e diminuir a velocidade aos outros??
		
				
				}
			}
			if(msg != null && msg.getPerformative() == ACLMessage.INFORM) {
				String aeronave = msg.getSender().getLocalName();
				ACLMessage resp = msg.createReply();
				String[] info = msg.getContent().split(";");
				String content = msg.getContent();
				switch(info[0]) {
					case "INICIARVIAGEM":
						listaPrioridade.add(content);
						
						//calculaPrioridade(); TODO-> M�todo para calculo da prioridade
						
				}
				
			}
		}
	}
	
	private class verificarListasEspera extends TickerBehaviour {
		 public verificarListasEspera(Agent a, long tempo) {
			 super(a,tempo);
		 }
		 
		 @Override
		 protected void onTick() {
			 // se existirem aeronaves em espera iniciar partida desta esta�ao e j� existem condi��es para efetuar descolagem
			 if(!listpedidosSaida.isEmpty() && (nrPistasOcupadas < totalPistas) && condMeteo) {
				
				try {
					
					
					 // aceder �s p�ginas amarelas para buscar o AID da aeronave
					DFAgentDescription dfd = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(listpedidosSaida.get(0));
					dfd.addServices(sd);
						
					DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
					AID provider = results[0].getName();
					 
					if(results.length == 1) {
						
						ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
						msg.addReceiver(provider);
						msg.setContent("AUTORIZA-SAIDA");
						send(msg);
					}
					
				
					listpedidosSaida.remove(0);
				
				 }catch(FIPAException e) {
					 e.printStackTrace();
				 }
			 }
			 
			 // se existem aeronaves em espera para iniciar chegada a esta esta�ao e j� existem conds
			 if(!listpedidosChegada.isEmpty() && (nrEstOcupados < totalEstacionamentos) && condMeteo) {
				 
				 try{
				
				 // aceder �s p�ginas amarelas para buscar o AID da aeronave
					DFAgentDescription dfd = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(listpedidosChegada.get(0));
					dfd.addServices(sd);
						
					DFAgentDescription[] results = DFService.search(this.myAgent, dfd);
					AID provider = results[0].getName(); 
					
					if(results.length == 1) {
						
						ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
						msg.addReceiver(provider);
						msg.setContent("AUTORIZA-ENTRADA");
						send(msg);
					}
					listpedidosChegada.remove(0);
				
				 }catch(FIPAException e) {
					 e.printStackTrace();
				 }
			 }
		}
					
	}
	
	public void calculaPrioridade() {
		
	}
		
	

	private void initEstacao() {
		switch(this.getLocalName()) {
		case "Estacao1":
			coordX = -10000;
			coordY = -500;
			condMeteo = true;
			
		
			break;
			
		case "Estacao2":
			coordX = -10000;
			coordY = 500;
			condMeteo = true;
			break;
			
		case "Estacao3":
			coordX = 10000;
			coordY = 0;
			condMeteo = true;
			break;
		
		case "Estacao4":
			coordX = 0;
			coordY = 11000;
			condMeteo = false;
		
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


}
