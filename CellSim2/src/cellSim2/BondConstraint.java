package cellSim2;

import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class BondConstraint extends Generic6DofConstraint{
	
	private Simulation sim;
	private long creationTime;
	float timeToStable;
	SurfaceSegment surfA, surfB;
	int proteinA, proteinB;
	boolean active;
	

	public BondConstraint(Simulation s, float stab, SurfaceSegment ssa, SurfaceSegment ssb, int proa, int prob, RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		super(rbA, rbB, frameInA, frameInB, true);
		sim = s;
		creationTime = sim.getCurrentTimeMicroseconds();
		timeToStable = stab * 60 * 1000000;//convert from minutes to microseconds
		surfA = ssa;
		surfB = ssb;
		proteinA = proa;
		proteinB = prob;
		active = true;
		ssa.makeBond(proa, Protein.MOLS_PER_BOND);
		ssb.makeBond(prob, Protein.MOLS_PER_BOND);
		s.addConstraint(this);
		//System.out.println("Bond created");
	}
	
	public void update(){
		long currentTime = sim.getCurrentTimeMicroseconds();
		float lifetime = (float)(currentTime - creationTime);
		float probToBreak = .9f;
		if (lifetime < timeToStable){
			probToBreak = ((0.8f * lifetime * lifetime)/(timeToStable * timeToStable)) - ((1.6f * lifetime)/timeToStable) + .9f;
		}
		//System.out.println("Bond Constraint. Lifetime: " + lifetime + " timeToStable: " + timeToStable + " prob to break: " + probToBreak);
		if (sim.getNextRandomF() >= probToBreak){
			active = false;
		}
	}
	
	public boolean isActive(){
		return active;
	}
	
	public void destroy(){
		//Return proteins to the surfaces
		long currentTime = sim.getCurrentTimeMicroseconds();
		long lifetime = currentTime - creationTime;
		
		float numMolecules = 1 - (float)(lifetime/timeToStable) * Protein.MOLS_PER_BOND;
		if (lifetime >= timeToStable){
			numMolecules = 0;
		}
		surfA.removeBond(proteinA, numMolecules);
		surfB.removeBond(proteinB, numMolecules);
	}
	
	public static String getDataHeaders(){
		String s = "ObjectA\tProteinA\tObjectB\tProteinB\tLifetime (minutes)\tIs Active\n";
		return s;
	}
	
	public String finalOutput(){
		float life = (float)(sim.getCurrentTimeMicroseconds() - creationTime)/(1000000*60);
		String s = surfA.getParent().toString() + "\t" + sim.getProteinName(proteinA) + "\t" + surfB.getParent().toString() + "\t" + sim.getProteinName(proteinB) + "\t" + life + "\t" + isActive() + "\n";
		return s;
	}
	

}
