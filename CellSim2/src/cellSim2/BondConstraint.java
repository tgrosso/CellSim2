package cellSim2;

import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;

import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.dynamics.constraintsolver.TranslationalLimitMotor;
import com.bulletphysics.dynamics.constraintsolver.RotationalLimitMotor;

public class BondConstraint extends Generic6DofConstraint{
	private static long id=0;
	private long myId;
	private Simulation sim;
	private long creationTime;
	float timeToStable, bondLength, aValue, cValue;
	SurfaceSegment surfA, surfB;
	int proteinA, proteinB;
	boolean active;
	Transform ta, tb;
	TranslationalLimitMotor tlm;
	RotationalLimitMotor[] rlm;
	

	public BondConstraint(Simulation s, float stab, float length, SurfaceSegment ssa, SurfaceSegment ssb, int proa, int prob, RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		super(rbA, rbB, frameInA, frameInB, true);
		this.setLinearLowerLimit(new Vector3f(-length, -length, -length));
		this.setLinearUpperLimit(new Vector3f(length, length, length));
		myId = id;
		id++;
		sim = s;
		creationTime = sim.getCurrentTimeMicroseconds();
		timeToStable = stab;
		bondLength = length;
		
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
		ta = new Transform();
		tb = new Transform();
		tlm = this.getTranslationalLimitMotor();
		tlm.damping = .5f;
		rlm = new RotationalLimitMotor[3];
		for (int i = 0; i < 3; i++){
				rlm[i] = this.getRotationalLimitMotor(i);
				rlm[i].damping = .5f;
		}
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
		getCalculatedTransformA(ta);
		getCalculatedTransformB(tb);
		ta.origin.sub(tb.origin);
		float bondStretch = ta.origin.length() / bondLength;
		if (bondStretch > 1){
			probToBreak *= bondStretch;
		}
		//If bond is very long, increase the chance of breaking!
		//TODO Add a parameter that either increases or decreases. Maybe with a time constraint
		
		float rand = sim.getNextRandomF();
		//System.out.println("Bond " + myId + ": deltaMins: " + deltaMins + " Prob to break: " + probToBreak + " rand: " + rand);
		if (rand <= probToBreak){
			active = false;
		}
		
		/*System.out.println("tlm damping " + tlm.damping);
		for (int i = 0 ;i < 3; i++){
			System.out.println("rlm damping " + rlm[i].damping);
		}*/
		
	}
	
	public boolean isActive(){
		return active;
	}
	
	public void destroy(){
		//Return proteins to the surfaces
		long currentTime = sim.getCurrentTimeMicroseconds();
		float lifetime = (currentTime - creationTime)/1000000/60;
		
		int numMolecules =(int)(Math.round(1 - (float)(lifetime/timeToStable) * Protein.MOLS_PER_BOND));
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
