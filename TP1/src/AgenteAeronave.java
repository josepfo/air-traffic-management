import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.*;
import java.util.*;

public class AgenteAeronave extends Agent{
	
	

	private double coordX;
	private double coordY;
	
	
	private double destCoordX;
	private double destCoordY;
	
	private double zonaProtegida;
	private double zonaAlerta;
	
	private int nrPassageiros;
	
	
	//private  direcao 
	
	private double velocidade;
	private double distPercorrer;
	private double distPercorrida;
	

	

	@Override
	protected void setup() {
		
	}
	
	@Override
	protected void takeDown() {
		
	}


}
