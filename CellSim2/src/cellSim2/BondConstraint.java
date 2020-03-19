package cellSim2;

import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.dynamics.constraintsolver.TranslationalLimitMotor;
import com.bulletphysics.dynamics.constraintsolver.RotationalLimitMotor;

public class BondConstraint extends Generic6DofConstraint{
	//public static BufferedWriter validatingData;
	private static long id=0;
	private String startTime;
	private Vector3f startAPos, startBPos;
	private float initialLength;
	private long myId;
	private Simulation sim;
	private long creationTime;
	float timeToStable, bondLength, aValue, cValue;
	float lastStretch;
	float adjustment;
	SurfaceSegment surfA, surfB;
	int proteinA, proteinB;
	int numMolecules;
	boolean positionSet, active;
	Transform ta, tb;
	TranslationalLimitMotor tlm;
	RotationalLimitMotor[] rlm;
	

	public BondConstraint(Simulation s, float stab, float length, SurfaceSegment ssa, SurfaceSegment ssb, int proa, int prob, RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
		super(rbA, rbB, frameInA, frameInB, true);
		this.setLinearLowerLimit(new Vector3f(-length, -length, -length));
		//this.setLinearLowerLimit(new Vector3f(-length, -length, -length));
		this.setLinearUpperLimit(new Vector3f(length, length, length));
		//this.setAngularLowerLimit(angularLower);
		RotationalLimitMotor rot = this.getRotationalLimitMotor(1);
		rot.loLimit = (float)((-Math.PI*.9/2));
		rot.hiLimit = (float)((Math.PI*.9/2));
		rot = this.getRotationalLimitMotor(2);
		rot.loLimit = (float)(-Math.PI/2);
		rot.hiLimit = (float)(Math.PI/2);
		rot = this.getRotationalLimitMotor(0);
		rot.loLimit = (float)(-Math.PI);
		rot.hiLimit = (float)(Math.PI);
		myId = id;
		id++;
		id = id % Long.MAX_VALUE;
		sim = s;
		creationTime = sim.getCurrentTimeMicroseconds();
		startTime = sim.getFormattedTime();
		timeToStable = stab;
		bondLength = length;
		lastStretch = 0;
		adjustment = 1;
		
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
		positionSet = false;
		numMolecules = Math.max(ssa.getMoleculesPerBond(proa), ssb.getMoleculesPerBond(prob));
		ssa.makeBond(proa, numMolecules);
		ssb.makeBond(prob, numMolecules);
		s.addConstraint(this);
		//Get world transform for object a and use to convert both positions to get initial points
		ta = new Transform();
		tb = new Transform();
		/*
		tlm = this.getTranslationalLimitMotor();
		//tlm.damping = .5f;
		rlm = new RotationalLimitMotor[3];
		for (int i = 0; i < 3; i++){
			rlm[i] = this.getRotationalLimitMotor(i);
		}*/
		startAPos = new Vector3f();
		startBPos = new Vector3f();
		initialLength = 0;
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
		
		
		probToBreak = probToBreak * deltaMins;//WHY?
		getCalculatedTransformA(ta);
		getCalculatedTransformB(tb);
		ta.origin.sub(tb.origin);
		float bondStretch = ta.origin.length() / bondLength;
		float deltaStretch = Math.abs(bondStretch-lastStretch);
		float stretchRatio = 0;
		if (lastStretch != 0f){
			stretchRatio = deltaStretch/lastStretch;
		}
		lastStretch = bondStretch;
		//if (bondStretch > 10){
		//	active = false;
		//}
		if (bondStretch > 1){
			adjustment *= 1.001f;
			//System.err.println("Bond Stretch = " + bondStretch);
		}
		else{
			adjustment = 1f;
		}
		
		//If bond is very long, increase the chance of breaking!
		//TODO Add a parameter that either increases or decreases. Maybe with a time constraint
		
		float rand = sim.getNextRandomF();
		//System.out.println("Bond " + myId + ": deltaMins: " + deltaMins + " Prob to break: " + probToBreak + " rand: " + rand);
		if (rand <= probToBreak * adjustment){
			active = false;
		}
		getCalculatedTransformA(ta);
		getCalculatedTransformB(tb);
		if (bondStretch > 200){
			//sim.writeToLog("Bond " + myId + " may be unstable!\t" + bondStretch + "\t" + stretchRatio);
			//sim.startInvestigating();
		}
		
		if (sim.isInvestigating()){
			String s = sim.getFormattedTime() + "\tBond\t" + myId + "\t" + bondStretch + "\t";
			s+= ta.origin + "\t" + tb.origin;
			for (int i = 0; i < 3; i++){
				s+= "\t" + getAngle(i);
			}
			s+= "\n";
			sim.writeInvestigatingData(s);
		}
	}
	
	public boolean isActive(){
		return active;
	}
	
	public void destroy(){
		//Return proteins to the surfaces
		long currentTime = sim.getCurrentTimeMicroseconds();
		float lifetime = (currentTime - creationTime)/1000000/60;
		
		int num =(int)(Math.round(1 - (float)(lifetime/timeToStable) * numMolecules));
		if (lifetime >= timeToStable){
			num = 0;
		}
		surfA.removeBond(proteinA, num);
		surfB.removeBond(proteinB, num);
	}
	
	public static String getDataHeaders(){
		String s = "ID\tStartTime\tStartA\tStartB\tStartLen\tObjectA\tA-ID\tProteinA\tObjectB\tB-ID\tProteinB\tLifetime (minutes)\tIs Active\n";
		return s;
	}
	
	public String getOutput(){
		float life = (float)(sim.getCurrentTimeMicroseconds() - creationTime)/(1000000*60);
		String s = myId + "\t" + startTime + "\t" + startAPos + "\t" + startBPos + "\t" + initialLength + "\t";
		s += surfA.getParent().getType() +"-"+surfA.getParent().getID()+ "\t" + surfA.getID() + "\t";
		s += sim.getProteinName(proteinA) + "\t" + surfB.getParent().getType() +"-"+surfB.getParent().getID() + "\t";
		s += surfB.getID() + "\t";
		s += sim.getProteinName(proteinB) + "\t" + life + "\t" + isActive();
		if (isActive()){
			getCalculatedTransformA(ta);
			getCalculatedTransformB(tb);
			s += ("\t" + ta.origin + "\t" + tb.origin);
		}
		return (s + "\n");
	}
	
	public long getID(){
		return myId;
	}
	
	public static void closeWriter(){
	}

}
