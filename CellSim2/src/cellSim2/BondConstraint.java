package cellSim2;

import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class BondConstraint extends Generic6DofConstraint{
	private static long id=0;
	private long myId;
	private Simulation sim;
	private long creationTime;
	float timeToStable, aValue, cValue;
	SurfaceSegment surfA, surfB;
	int proteinA, proteinB;
	boolean active;
	

	public BondConstraint(Simulation s, float stab, SurfaceSegment ssa, SurfaceSegment ssb, int proa, int prob, RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		super(rbA, rbB, frameInA, frameInB, true);
		myId = id;
		id++;
		sim = s;
		creationTime = sim.getCurrentTimeMicroseconds();
		timeToStable = stab;
		//System.out.println("Time to stable: " + timeToStable);
		cValue = .05f;
		if (cValue * timeToStable >= 7){
			cValue = 6.9f /timeToStable;
		}
		if (timeToStable > 10){
			aValue = (float)(.7 - timeToStable*cValue);
		}
		else{
			double eS = Math.exp(timeToStable);
			aValue = (float)((.7 - timeToStable*cValue)*eS/(eS-1));
		}
		//System.out.println("Time to stable: " + timeToStable + " aValue: " + aValue + " cValue: "+cValue);
		surfA = ssa;
		surfB = ssb;
		proteinA = proa;
		proteinB = prob;
		active = true;
		ssa.makeBond(proa, Protein.MOLS_PER_BOND);
		ssb.makeBond(prob, Protein.MOLS_PER_BOND);
		s.addConstraint(this);
		//System.out.println("Bond created");
		//System.out.println("   Surface a: " + ssa.getParent().getType() + "-" + ssa.getParent().getID());
		//System.out.println("   Surface b: " + ssb.getParent().getType() + "-" + ssb.getParent().getID());
	}
	
	public void update(){
		long currentTime = sim.getCurrentTimeMicroseconds();
		float deltaMins = sim.getDeltaTimeMicroseconds()/1000000f/60;
		float lifetime = (float)(currentTime - creationTime);
		float t = lifetime/1000000f/ 60; //convert microsecs to minutes
		float probToBreak = cValue;
		if (t < (timeToStable*3/4) || t < 80){
			probToBreak = (float)(aValue / Math.exp(t) + cValue);
		}
		probToBreak = probToBreak * deltaMins;
		float rand = sim.getNextRandomF();
		//System.out.println("Bond " + myId + ": deltaMins: " + deltaMins + " Prob to break: " + probToBreak + " rand: " + rand);
		if (rand <= probToBreak){
			active = false;
		}
	}
	
	public boolean isActive(){
		return active;
	}
	
	public void destroy(){
		//Return proteins to the surfaces
		long currentTime = sim.getCurrentTimeMicroseconds();
		float lifetime = (currentTime - creationTime)/1000000/60;
		
		float numMolecules = 1 - (float)(lifetime/timeToStable) * Protein.MOLS_PER_BOND;
		if (lifetime >= timeToStable){
			numMolecules = 0;
		}
		surfA.removeBond(proteinA, numMolecules);
		surfB.removeBond(proteinB, numMolecules);
	}
	
	public static String getDataHeaders(){
		String s = "ID\tObjectA\tProteinA\tObjectB\tProteinB\tLifetime (minutes)\tIs Active\n";
		return s;
	}
	
	public String getOutput(){
		float life = (float)(sim.getCurrentTimeMicroseconds() - creationTime)/(1000000*60);
		String s = myId + "\t" + surfA.getParent().getType() +"-"+surfA.getParent().getID()+ "\t" + sim.getProteinName(proteinA) + "\t" + surfB.getParent().getType() +"-"+surfB.getParent().getID() + "\t" + sim.getProteinName(proteinB) + "\t" + life + "\t" + isActive() + "\n";
		return s;
	}
	

}
