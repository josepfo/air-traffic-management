import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.*;

public class AgenteEstacao extends Agent{

	private double coordX;
	private double coordY;
	
	private int totalPistas;
	private int nrPistasOcupadas;
	private int totalEstacionamentos;
	private int nrEstOcupados;
	
	private double zonaProtegida;
	private double zonaAlerta;
	
	// condições metereologicas favoraveis ou não
	private boolean condMeteo;
	
	// lista de prioridades de aeronaves em viagem para esta estação
	private List<AID> listaPrioridade;
	
	// lista de espera para iniciar viagem para esta estação
	private List<AID> listpedidosViagem;
	
	
	
		@Override
	protected void setup() {
		
	}
	
	@Override
	protected void takeDown() {
		
	}


}
