/**
	 * Copyright (C) 2016 Terri Applewhite-Grosso and Nancy Griffeth
	 * Package: cellSim2
	 * File: TestRunner.java
	 * Jul 14, 2016
	 *
	 *   Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
	 *
	 *   The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
	 *
	 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
	 * 
	 * 
	 */

package cellSim2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.HashMap;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

import java.io.IOException;
import java.io.InputStreamReader;
//import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
//import java.util.Iterator;

import org.lwjgl.opengl.GL11;


/**
 * @author Terri Applewhite-Grosso
 *
 */
public class SegmentedCell implements SimObject{
	
	private static int cell_ids = 0;
	private static HashMap<String, HashMap<Integer, Interaction[]>> interactions = new HashMap<String, HashMap<Integer, Interaction[]>>();
	
	//converts concentration to nanoMolar - 
	//molecules/cubic microns * 1 mole / Avogardro's molecules * cubic micron/10 -15 L * 1 nanomole/10-9 moles
	private int myId;
	private boolean visible = true;
	private boolean toRemove = false;
	private boolean bound = false;
	private boolean finalWritten = false;
	private String cellType = "Unnamed Type";
	private HashMap<Integer, TraffickingInfo> traffickRates;
	private int visibleProtein = -1;
	private boolean boundProtein = false;
	private float[] baseColor = {1.0f, 1.0f, 0};
	
	private BufferedWriter outputFile;
	
	private Vector3f origin, initialPosition, lastPosition, lastAV, lastDeltaAV, lastLV;
	private float minY = 0;
	private float accumulatedDistance;
	private float radius;
	private float density = 1.01f;
	private float volume, mass;
	private float maxDeltaVel=1; //units radians per second
	private float maxLinVel, maxAngVel;
	private int numSegments;
	private int detailLevel;
	private static GImpactMeshSphere cellShape;
	//private static SphereShape cellShape;
	protected SimRigidBody body;
	protected Transform trans;
	private Simulation sim;
	private ObjectArrayList<SimObject> collidedObjects;
	
	private float surfaceArea;
	private float[] triangleAreas;
	private Vector3f[][] triangleVertices;
	private SurfaceSegment[] membraneSegments;
	private Vector3f[] triangleCenters;
	protected HashSet<Integer> surfaceProteins;
	protected HashSet<Integer> receptorsBindTo;
	private boolean hasReceptors;
	private boolean[] segCollided;
	
	
	private static float[] glMat = new float[16];
	private static Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
	private static Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);
	private Vector3f posValues, negValues;

	private long[] pos;
	private long[] neg;
	

	/**
	 * Represents the basic cell that moves around in a simulation
	 */
	public SegmentedCell(Simulation s, Vector3f o, float r, int dl) {
		this.myId = cell_ids;
		cell_ids++;
		this.sim = s;
		posValues = new Vector3f(0, 0, 0);
		negValues = new Vector3f(0, 0, 0);
		
		this.origin = o;
		initialPosition = new Vector3f(o);
		lastPosition = new Vector3f(o);
		accumulatedDistance = 0;
		this.radius = r;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;
		detailLevel = dl;
		surfaceArea = (float)(4 * Math.PI * r * r);
		
		trans = new Transform();
		trans.setIdentity();
		trans.origin.set(this.origin);
		float randTheta = (float)(sim.getNextRandomF() * Math.PI);
		float si = (float)Math.sin(randTheta);
		float cs = (float)Math.cos(randTheta);
		Matrix3f rot = new Matrix3f(cs, 0, si, 0, 1, 0, -si, 0, cs);
		trans.set(rot);
		//System.out.println("Random Theta: " + randTheta);
		
		cellShape = new GImpactMeshSphere(dl);
		//cellShape.outputSomeData();
		cellShape.setLocalScaling(new Vector3f(radius, radius, radius));
		cellShape.updateBound();
		minY = cellShape.getLowestY() * radius;
		//System.out.println("minY " + minY);
		Vector3f localInertia = new Vector3f(0, 0, 0);
		cellShape.calculateLocalInertia(mass, localInertia);
		
		DefaultMotionState motionState = new DefaultMotionState(trans);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, cellShape, localInertia);
		//System.out.println("Friction: " + rbInfo.friction);
		body = new SimRigidBody(rbInfo, this);
		//System.out.println(body.toString());
		body.setCollisionFlags(CollisionFlags.CUSTOM_MATERIAL_CALLBACK);
		numSegments = cellShape.getNumTriangles();
		
		float downForce = (mass * 9.8f) - (volume * 9.8f);
		body.setGravity(new Vector3f(0, -downForce, 0));
		//body.setAngularVelocity(getRandomVector(.1f));
		lastAV = new Vector3f(0, 0, 0);
		body.getAngularVelocity(lastAV);
		lastLV = new Vector3f(0, 0, 0);
		body.getLinearVelocity(lastLV);
		hasReceptors = false;
		
		traffickRates = new HashMap<Integer, TraffickingInfo>();
		
		//Set the triangle areas
		//TODO - When cells change shape this will have to be updated regularly
		triangleAreas = new float[numSegments];
		triangleAreaCallback areaCallback = new triangleAreaCallback(this);
		cellShape.processAllTriangles(areaCallback, aabbMin, aabbMax);
		surfaceArea = 0;
		for (int i = 0; i < numSegments; i++){
			surfaceArea += triangleAreas[i];
			//s.writeInvestigatingData("triangle " + i + " surface area " + triangleAreas[i] + "\n");
		}
		//System.out.println("Seg Cell 172: Segment 0 fraction = " + triangleAreas[0]/surfaceArea);
		
		triangleVertices = new Vector3f[numSegments][];
		
		triangleVerticesCallback tcb = new triangleVerticesCallback(this);
		cellShape.processAllTriangles(tcb, aabbMin, aabbMax);
		
		//System.out.println("Cell dl: " + detailLevel);
		triangleCenters = new Vector3f[numSegments];
		for (int i = 0; i < numSegments; i++){
			Vector3f[] p = triangleVertices[i];
			float x = (p[0].x + p[1].x + p[2].x) / 3;
			float y = (p[0].y + p[1].y + p[2].y) / 3;
			float z = (p[0].z + p[1].z + p[2].z) / 3;
			triangleCenters[i] = new Vector3f(x, y, z);
			//System.out.println("   " + " segment: " + i + " center: " + triangleCenters[i] );
		}
		
		//triangleVerticesOutputCallback tvoc = new triangleVerticesOutputCallback(this, trans);
		//cellShape.processAllTriangles(tvoc, aabbMin, aabbMax);
		
		
		
		//System.out.println("Total surface area = " + surfaceArea);
		surfaceProteins = new HashSet<Integer>();
		receptorsBindTo = new HashSet<Integer>();
		
		membraneSegments = new SurfaceSegment[numSegments];
		for (int i = 0; i < numSegments; i++){
			membraneSegments[i] = new SurfaceSegment(this, i);
		}
		
		collidedObjects = new ObjectArrayList<SimObject>();
		
		sim.setNeedsGImpact(true);
		sim.addSimulationObject(this);
		//System.out.println("Cell added to simulation");
		
		pos = new long[3];
		neg = new long[3];
		for (int i = 0; i < 3; i++){
			pos[i] = 0;
			neg[i] = 0;
		}
		segCollided = new boolean[numSegments];
		for (int i = 0; i < numSegments; i++){
			segCollided[i] = false;
		}
		
		maxLinVel = (float)(.9* radius * 2 / (s.getDeltaTimeMicroseconds() /1000000)); 
		//This means it would be moving more than its diameter in a time frame
		maxAngVel = (float)(.9* (Math.PI / 2) / (s.getDeltaTimeMicroseconds()/1000000));
		
	}
	
	public SegmentedCell(Simulation s, float r, int dl) {
		this(s, new Vector3f(0, 0, 0), r, dl);
	}
	
	public void setInitialPosition(Vector3f o){
		//System.out.println("Setting initial position");
		body.getAngularVelocity(lastAV);
		//System.out.println("Last AV: " + lastAV.toString());
		body.getLinearVelocity(lastLV);
		//System.out.println("Last LV: " + lastLV.toString());
		
		this.origin = new Vector3f(o);
		trans.setIdentity();
		trans.origin.set(this.origin);
	
		DefaultMotionState motionState = new DefaultMotionState(trans);
		body.setMotionState(motionState);
		
		motionState.getWorldTransform(trans);
		initialPosition = new Vector3f(trans.origin);
		//System.out.println("Initial position: " + initialPosition.toString());
		this.body.getCenterOfMassPosition(initialPosition);
		//System.out.println("Initial COM position: " + initialPosition.toString());
		lastPosition = new Vector3f(trans.origin);
		body.setAngularVelocity(lastAV);
		body.setLinearVelocity(lastLV);
		lastDeltaAV = new Vector3f();
		accumulatedDistance = 0;
		
		//Vector3f testVec = new Vector3f();
		//body.getAngularVelocity(testVec);
		//System.out.println("Angular Vel after setting initial position: " + testVec.toString());
		//body.getLinearVelocity(testVec);
		//System.out.println("Linear Vel after setting initial position: " + testVec.toString());
		
		//System.out.println("my origin: " +initialPosition.toString() + " Dist to source: " + sim.getDistanceFromSource(initialPosition.x));
	}
	
	public static HashMap<String, String[]> readCellData(String type, String filename){
		HashMap<String, String[]> cdata = new HashMap<String, String[]>();
		cdata.put("Name", new String[]{"Unnamed Cell Type"});
		cdata.put("numCells", new String[]{"5"});
		cdata.put("cellDetailLevel", new String[]{"1"});
		cdata.put("radius", new String[]{"10"});
		cdata.put("density", new String[]{"1.01"});
		cdata.put("maxDeltaVel", new String[]{"1"});
		File inFile = new File(filename);
		try{
			FileInputStream fis = new FileInputStream(inFile);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String line;
		
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.contains("\\\\")){
					String[] valueVar = line.split("\t");
					if (valueVar.length < 2){
						continue;
					}
					String var = valueVar[0];
					String[] value = Arrays.copyOfRange(valueVar, 1, valueVar.length);
					if (var.equals("receptor")){
						if (cdata.containsKey("receptor")){
							String[] oldVals = cdata.get(var);
							String[] newVals = new String[oldVals.length+1];
							for (int i = 0; i < oldVals.length; i++){
								newVals[i] = oldVals[i];
							}
							newVals[oldVals.length] = value[0];
							//add value[0] (name of protein) to list
							oldVals = newVals;
						}
						else{
							cdata.put("receptor", new String[]{value[0]});
						}
					}
					else if (var.equals("interaction")){
						if (cdata.containsKey("interaction")){
							String[] oldVals = cdata.get(var);
							String[] newVals = new String[oldVals.length+1];
							for (int i = 0; i < oldVals.length; i++){
								newVals[i] = oldVals[i];
							}
							newVals[oldVals.length] = value[0];
							oldVals = newVals;
							//add value[0] (name of protein) to list
						}
						else{
							cdata.put("interaction", new String[]{value[0]});
						}
					}
					else if (cdata.containsKey(var)){
						cdata.put(var, value);
					}
					else{//Not a known value
						System.err.println(var + " is not a valid cell value.");
					}
				}
			}
			br.close();
			isr.close();
			fis.close();
		}
		catch(IOException e){
						System.err.println("Error reading in cell input file: " + filename);
						System.err.println("Error: " + e.toString());
						System.err.println("Cell Data Not Added");
						return new HashMap<String, String[]>();
		}
		return cdata;
	}
	
	public static ArrayList<SegmentedCell> readInFile(String type, String filename, Simulation sim){
		File inFile = new File(filename);
		int numCells = 1;
		int detailLevel = 1;
		float radius = 10;
		float density = 1.01f;
		float volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		float mass = density * volume;
		
		float maxDeltaVel = 1;
		ArrayList<String[]> receptorInfo = new ArrayList<String[]>();
		ArrayList<String[]> interactionInfo = new ArrayList<String[]>();
		try{
			FileInputStream fis = new FileInputStream(inFile);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader br = new BufferedReader(isr);
			String line;
		
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.contains("\\\\")){
					String[] valueVar = line.split("\t");
					if (valueVar.length < 2){
						System.err.println("Badly formatted input. Variable<tab>value.");
						System.err.println("Input was: " + line);
						continue;
					}
					String var = valueVar[0];
					String value = valueVar[1];
					if (value == null || var == null){
						System.err.println("Badly formatted input. Variable <tab>value.");
						System.err.println("Input was: " + line);
						continue;
					}
					if (var.equals("receptor")){
						if (valueVar.length < 6){
							System.err.println("Badly formatted input. receptor<tab>protein name<tab>secretion rate<tab><unbound internalization rate<tab>bound internalization rate<tab>molecules per bond.");
							System.err.println("Input was: " + line);
							continue;
						}
						receptorInfo.add(Arrays.copyOfRange(valueVar, 1, valueVar.length));
						continue;
					}
					if (var.equals("interaction")){
						if (valueVar.length < 7){
							System.err.println("Badly formatted input. interaction<tab>receptor protein<tab>target protein<tab>affected rate<tab>activation percent<tab>saturation percent<tab>maximum response");
							System.err.println("Input was: " + line);
							continue;
						}
						interactionInfo.add(Arrays.copyOfRange(valueVar, 1, valueVar.length));
						continue;
					}
					if (!Defaults.variableExists("Cell", var)){
						System.err.println("Varible " + var + " does not exist for cells.");
						continue;
					}
					if (var.equals("numCells")){
						numCells = Integer.parseInt(value);
						continue;
					}
					if (var.equals("cellDetailLevel")){
						detailLevel = Integer.parseInt(value);
						continue;
					}
					if (var.equals("radius")){
						radius = Float.parseFloat(value);
						//System.out.println("radius: " + radius);
						continue;
					}
					if (var.equals("density")){
						density = Float.parseFloat(value);
						continue;
					}
					if (var.equals("maxDeltaVel")){
						maxDeltaVel = Float.parseFloat(value);
						//System.out.println("Max delta vel " + maxDeltaVel);
						continue;
					}
				}
			}
			br.close();
			isr.close();
			fis.close();
		}
		catch(IOException e){
			System.err.println("Error reading in cell input file: " + filename);
			System.err.println("Error: " + e.toString());
			System.err.println("Cells not added");
			return new ArrayList<SegmentedCell>();
		}
		ArrayList<SegmentedCell> cells = new ArrayList<SegmentedCell>();
		for (int i = 0; i < numCells; i++){
			SegmentedCell nextCell = new SegmentedCell(sim, radius, detailLevel);
			nextCell.setDensity(density);
			nextCell.setCellType(type);
			nextCell.setOutputFile(sim.getCellFile());
			nextCell.setMaxDeltaVel(maxDeltaVel);
			//System.out.println("Just made cell: position: " + nextCell.origin.toString());
			cells.add(nextCell);
		}
		//Add receptors to each cell
		for (int i = 0; i < receptorInfo.size(); i++){
			String[] rec = receptorInfo.get(i);
			String proName = rec[0];
			int proId = sim.getProteinId(proName);
			if (proId<0){
				System.err.println("Error adding receptor: " + proName + " does not exist");
				continue;
			}
			float secrete = 0;
			float internUnbound = 0;
			float internBound = 0;
			int bpm = 1;
			try{
				//The rates in the text file are per minute - convert to per microseconds
				secrete = Float.parseFloat(rec[1])/6e7f;
				internUnbound = Float.parseFloat(rec[2])/6e7f ;
				internBound = Float.parseFloat(rec[3])/6e7f;
				bpm = Integer.parseInt(rec[4]);
			}
			catch(NumberFormatException e){
				System.err.println("Error parsing receptor " + rec[0]);
				System.err.println("Receptor not added");
				continue;
			}
			TraffickingInfo ti = new TraffickingInfo(secrete, internUnbound, internBound);
			for (int j = 0; j < numCells; j++){
				cells.get(j).addReceptor(proId, ti, bpm);
			}
		}
		//add interactions
		for (int i = 0; i < interactionInfo.size(); i++){
			String[] inter = interactionInfo.get(i);
			String recName = inter[0];
			int recId = sim.getProteinId(recName);
			if (recId<0){
				System.err.println("Error adding interaction: " + recName + " does not exist");
				continue;
			}
			String tarName = inter[1];
			int tarId = sim.getProteinId(tarName);
			if (tarId<0){
				System.err.println("Error adding interaction: " + tarName + " does not exist");
				continue;
			}
			int rate = -1;
			try{
				rate = Integer.parseInt(inter[2]);
			}
			catch(NumberFormatException e){
				rate = Interaction.getRateId(inter[2]);
				if (rate < 0){
					System.err.println("Error parsing interaction " + inter[0]);
					System.err.println("Interaction not added");
					continue;
				}
			}
			float affectPer = 0;
			float satPer = 0;
			float maxResp = 1;
			try{
				affectPer = Float.parseFloat(inter[3]);
				satPer = Float.parseFloat(inter[4]);
				maxResp = Integer.parseInt(inter[5]);
			}
			catch(NumberFormatException e){
				System.err.println("Error parsing interaction " + inter[0]);
				System.err.println("Interaction not added");
				continue;
			}
			if (rate < 0 || rate > 2){
				System.err.println("Error parsing secretion rate " + inter[0]);
				System.err.println("Interaction not added");
				continue;
			}
			Interaction in = new Interaction(recId, tarId, rate, affectPer, satPer, maxResp);
			sim.writeToLog(type + ": Adding Interaction - " + in.toString());
			
		}	
		return cells;
	}
	
		
	public void addReceptor(int proID, TraffickingInfo ti, int mpb){
		//Add the TraffickingInfo to the map
		traffickRates.put(proID, ti);
		//System.out.println("Seg Cell 451: ");
		//System.out.println("  secretion: " + ti.getSecretionRate());
		//System.out.println("  unboundInt: " + ti.getUnboundIntRate());
		//System.out.println("   boundInt: " + ti.getBoundIntRate());
		//Find the steady state secretion rate for the whole cell
		float totalUnbound = ti.getSecretionRate() / ti.getUnboundIntRate();
		//System.out.println(ti);
		//System.out.println("   unbound: " + totalUnbound);
		
		if (!hasReceptors){
			//no proteins yet!
			hasReceptors = true;
			visibleProtein = proID; //by default, the first protein is the default
			baseColor = getProtein(visibleProtein).getColor();
		}
		for (int i = 0; i < numSegments; i++){
			//System.out.println("Seg Cell 392: Adding receptor to cell");
			membraneSegments[i].addReceptor(proID, totalUnbound * triangleAreas[i]/surfaceArea, mpb);
		}
		
		int[] lig = sim.getProtein(proID).getLigands();
		for (int i = 0; i < lig.length; i++){
			receptorsBindTo.add(new Integer(lig[i]));
		}
		surfaceProteins.add(new Integer(proID));
	}
	
	public void addInteraction(Interaction i){
		
	}
	
	public HashSet<Integer> getSurfaceProteins(){
		return surfaceProteins;
	}
	
	public HashSet<Integer> getReceptorsBindTo(){
		return receptorsBindTo;
	}
	
	public Gradient getGradient(int pro){
		return sim.getGradient(pro);
	}
	
	public void setCellType(String s){
		cellType = s;
	}
	
	public void collided(SimObject c, ManifoldPoint mp, long collID){
		//System.out.println("Collided. Segmented Cell");
		if (!hasReceptors){
			//Nothing to bind to!
			return;
		}
		
		HashSet<Integer> otherProteins = c.getSurfaceProteins();
		if (otherProteins == null || otherProteins.size() < 1){
			//Nothing to bind to!
			return;
		}
		//Find the indexes of the surface segments
		int mySegment = (collID==0) ? mp.index0 : mp.index1;
		int theirSegment = (collID==0) ? mp.index1 : mp.index0;
		if (theirSegment == -1){
			//The other object is a wall, find the appropriate surface
			Wall theWall = (Wall)c;
			Vector3f theirPoint = (collID ==0) ? mp.localPointB : mp.localPointA;
			theirSegment = theWall.getSurface(theirPoint);
		}
		else{
			//System.out.println("Cell " + myId + ": I've collided with another cell!");
		}
		//Get the relevant surface segments
		SurfaceSegment mySurface = getSurfaceSegment(mySegment);
		SurfaceSegment theirSurface = c.getSurfaceSegment(theirSegment);
		if (theirSurface == null){
			//They don't have receptors here!
			return;
		}
		/*if (mp.index0 != -1 && mp.index1 != -1){
			System.out.println("Not a wall!");
			System.out.println("The collID is : " + collID);
			System.out.println("index 0: " + mp.index0 + " index 1: " + mp.index1);
			System.out.println("mySegment: " + mySegment + " theirSegment: " + theirSegment);
		}*/
		
		//Go through each of the receptors on my surface
		for (Integer p: surfaceProteins){
			Protein pro = sim.getProtein(p);
			//Get the ligands for this protein
			int[] ligands = pro.getLigands();
			for (int i = 0; i < ligands.length; i++){
				int proId = ligands[i];
				if (otherProteins.contains(proId)){
					//My receptor binds to the ligand on the other's surface
					//System.out.println("My receptor: " + pro.getName() + " Their ligand: " + sim.getProtein(proId).getName());
					//Try to make bonds
					if (Utilities.makeBonds(sim, p, proId, mySurface, theirSurface)>0){
						segCollided[mySegment] = true;
					}
				}
			}
		}
	}

	
	
	
	public boolean collidedWith(SimObject s){
		if (collidedObjects.indexOf(s) == -1){
			collidedObjects.add(s);
			return false;
		}
		return true;
	}
	
	public void clearCollisions(){
		collidedObjects = new ObjectArrayList<SimObject>();
	}
	
	public CollisionShape getCollisionShape(){
		return cellShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public void updateObject(){
		if (!this.body.isActive()){
			//System.out.println("Cell " + this.myId + " has been deactivated.");
			this.body.activate();
		}
		//System.out.println("Detail Level: " + detailLevel + " num segments: " + numSegments);
		//System.out.println("Starting update: " + lastPosition.toString());
		float downForce = (mass * 9.8f) - (volume * 9.8f);
		this.body.setGravity(new Vector3f(0, -downForce, 0));
		long now = sim.getCurrentTimeMicroseconds();
		long delta = (long)sim.getDeltaTimeMicroseconds();
	
		if (delta > 0){
			//Only move it around if the angular velocity is very small
			Vector3f oldVel = new Vector3f(0, 0, 0);
			this.body.getAngularVelocity(oldVel);
			float len = oldVel.length();
			if (len < .5f){
				Vector3f addVel = getRandomVector(maxDeltaVel);
				lastDeltaAV = new Vector3f(addVel);
				oldVel.add(addVel);
				this.body.setAngularVelocity(oldVel);
			}
		}
			//Find the current position and velocities
		this.body.getMotionState().getWorldTransform(trans);
		Vector3f distance = new Vector3f();
		distance.sub(trans.origin, lastPosition);
		accumulatedDistance += distance.length();
		lastPosition = new Vector3f(trans.origin);
		this.body.getAngularVelocity(lastAV);
		this.body.getLinearVelocity(lastLV);
		
		
		//Clamp the velocities
		float maxAng = Math.max(Math.max(Math.abs(lastAV.x), Math.abs(lastAV.y)), Math.abs(lastAV.z));
		if (maxAng > 2 * Math.PI){
			//sim.startInvestigating();
			//sim.writeInvestigatingData("Cell " + myId + " has a high angular velocity. \t" + lastAV.toString());
		}
		//this.body.setAngularVelocity(lastAV);
		float maxLin = Math.max(Math.max(Math.abs(lastLV.x), Math.abs(lastLV.y)), Math.abs(lastLV.z));
		if (maxLin > maxLinVel){
			//sim.startInvestigating();
			//sim.writeInvestigatingData("Cell " + myId + " has a high linear velocity. \t" + lastLV.toString());
		}
		//this.body.setLinearVelocity(lastLV);
		if (sim.isInvestigating()){
			writeOutput();
		}
		
		//TODO We are going to set the axis to be the x for now! 
		//We probably want the distance from source to be attached to the gradient
		//System.out.println("Cell center: " + trans.origin);
		for (int i = 0; i < membraneSegments.length; i++){
			Vector3f cen = new Vector3f(triangleCenters[i]);
			trans.transform(cen);
			cen.x = sim.getDistanceFromSource(cen.x);
			membraneSegments[i].update(now, delta, cen);
		}
		
	}
	
	public void setDensity(float d){
		density = d;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;
	}
	
	public void setMaxDeltaVel(float m){
		//maxDeltaVel = (float)(m/Math.pow(detailLevel, .25));
		maxDeltaVel = m;
	}
	
	private Vector3f getRandomVector(float mag){
		//http://mathworld.wolfram.com/SpherePointPicking.html
		float m = sim.getNextRandomF() * mag;
		float sum = 2;
		float randX1 = 0;
		float randX2 = 0;
		float oneSquared = 0;
		float twoSquared = 0;
		while (sum >= 1.0f){
			randX1 = sim.getNextRandomF() * 2 - 1.0f;
			randX2 = sim.getNextRandomF() * 2 - 1.0f;
			oneSquared = randX1 * randX1;
			twoSquared = randX2 * randX2;
			sum = oneSquared + twoSquared;
		}
		float xval = (float)(2 * randX1 * Math.sqrt(1 - oneSquared - twoSquared));
		float zval = (float)(2 * randX2 * Math.sqrt(1 - oneSquared - twoSquared));
		float yval = (float)(1 - 2 * (oneSquared + twoSquared));
		if (xval > 0){
			posValues.x++;
		}
		if (yval > 0){
			posValues.y++;
		}
		if (zval > 0){
			posValues.z++;
		}
		
		if (xval < 0){
			negValues.x++;
		}
		if (yval < 0){
			negValues.y++;
		}
		if (zval < 0){
			negValues.z++;
		}
		Vector3f vec = new Vector3f(xval, yval, zval);
		//System.out.print("Vec length: " + vec.length());
		vec.scale(m);
		//System.out.println(" Final length: " + vec.length());
		return vec;
	}
	
	public boolean hasTraffickInfoForProtein(int id){
		Integer i = new Integer(id);
		return traffickRates.containsKey(i);
	}
	
	public TraffickingInfo getTraffickInfo(int pro, int id){
		if (traffickRates.containsKey(pro)){
			TraffickingInfo ti = traffickRates.get(pro);
			float sec = (ti.getSecretionRate() * triangleAreas[id]/surfaceArea);
			return new TraffickingInfo(sec, ti.getUnboundIntRate(), ti.getBoundIntRate());
		}
		else{
			return new TraffickingInfo();
		}
	}
	
	public Vector3f getColor3Vector(){
		//draws itself, so no color vector necessary
		return (new Vector3f(baseColor));
	}
	
	public SurfaceSegment getSurfaceSegment(int index){
		return membraneSegments[index];
	}
	
	public float getSegmentArea(int index){
		return triangleAreas[index];
	}
	
	public Vector3f getSegmentWorldNormal(int index){
		Vector3f norm = new Vector3f(triangleCenters[index]);
		Transform myTrans = new Transform();
		body.getMotionState().getWorldTransform(myTrans);
		myTrans.transform(norm);
		norm.sub(myTrans.origin);
		norm.normalize();
		//System.out.println("my final normal: " + norm);
		return norm;
	}
	
	public Vector3f[] getWorldCoordinates(int surface){
		Transform myTrans = new Transform();
		body.getMotionState().getWorldTransform(myTrans);
		Vector3f[] myVertices = new Vector3f[3];
		for (int i = 0; i < 3; i++){
			myVertices[i] = new Vector3f(triangleVertices[surface][i]);
			myTrans.transform(myVertices[i]);
		}
		return myVertices;
	}
	
	public void setVisible(boolean v){
		visible = v;
	}
	
	
	public boolean isVisible(){
		return visible;
	}
	
	public int getID(){
		return myId;
	}
	
	
	public float getMass(){
		return mass;
	}
	
	public float getRadius(){
		return radius;
	}
	
	public float[] getBaseColor(){
		return baseColor;
	}
	
	public String getType(){
		return "Segmented Cell";
	}
	
	public float getMinY(){
		return minY;
	}
	
	public void destroy(){
		body.destroy();
	}
	
	public void markForRemoval(){
		toRemove = true;
	}
	
	public boolean isMarked(){
		return toRemove;
	}
	
	public Protein getProtein(int id){
		return sim.getProtein(id);
	}
	
	public boolean specialRender(IGL gl, Transform t, int m){
		gl.glPushMatrix();
		t.getOpenGLMatrix(glMat);
		gl.glMultMatrix(glMat);
		
		drawSegmentsCallback drawCallback = new drawSegmentsCallback(gl, this);
		cellShape.processAllTriangles(drawCallback, aabbMin, aabbMax);
		gl.glPopMatrix();
		
		//Let's draw ours and then theirs and see what happens.
		
		return true;
	}
	
	private static class drawSegmentsCallback extends TriangleCallback {
		private IGL gl;
		SegmentedCell parent;
		private int visibleID;
		private Protein visibleProtein;

		public drawSegmentsCallback(IGL gl, SegmentedCell p) {
			this.gl = gl;
			this.parent = p;
			this.visibleID = this.parent.getVisibleProtein();
			if (visibleID >= 0){
				this.visibleProtein = this.parent.getProtein(visibleID);
			}
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			float[] color = parent.getBaseColor();
			
			if (visibleID >= 0){
				SurfaceSegment seg = parent.membraneSegments[triangleIndex];
				float percent = seg.getProteinPortion(visibleID, parent.showingBoundProtein());
				//System.out.println("Segment: " + seg + " percent: " + percent);
				color = visibleProtein.getColor(percent);
			}
			
			gl.glBegin(GL11.GL_TRIANGLES);
			gl.glColor3f(color[0], color[1], color[2]);
			gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			//gl.glColor3f(color[0]);
			gl.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
			//gl.glColor3f(1, 0, 0);
			gl.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
			gl.glEnd();
			gl.glBegin(GL11.GL_LINES);
			gl.glLineWidth(3);
			gl.glColor3f(0f, 0f, 0f);
			gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			gl.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
			gl.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
			gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			gl.glEnd();
			/*
			gl.glPointSize(4.0f);
			gl.glColor3f(1.0f,  1.0f, 1.0f);
			gl.glBegin(GL11.GL_POINTS); //starts drawing of points
		    gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			gl.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
			gl.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
		    gl.glEnd();//end drawing of points
		    */
			
		}
	}

	public boolean isMobile(){
		return true;
	}
	
	public boolean isBound(){
		return bound;
	}
	
	public void clearBound(){
		bound = false;
	}
	
	public void bind(){
		bound = true;
	}
	
	public int getVisibleProtein(){
		return visibleProtein;
	}
	public boolean showingBoundProtein(){
		return boundProtein;
	}
	
	public String getSurfaceSegmentOutput(){
		String s = "";
		//for (int i = 0; i < numSegments; i++){
		for (int i = 0; i < 5; i++){
			s += membraneSegments[i].getOutput(sim);
		}
		return s;
	}
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tCell Type\tCell ID\txPos\tyPos\tzPos\txLastAV\tyLastAV\tzLastAV\txLastDeltaAV\tyLastDeltaAV\tzLastDeltaAV\txLastLV\tyLastLV\txLastLV\tCollidedSegments\n";
		return s;
	}
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		String s = "";
		s += "Segmented Cell\t" + cellType + "\t" + myId + "\n";
		finalWritten = true;
		return s;
	}
	
	public void wrapup(){
		cell_ids = 0;
	}
	
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
	}
	
	public void writeOutput(){
		//also should write segment data
		String cols = "";
		for (int i = 0; i < numSegments; i++){
			if (segCollided[i]){
				cols += (i + " ");
				segCollided[i] = false;
			}
		}
		
		if (outputFile != null){
			try{
				outputFile.write(sim.getFormattedTime() + "\t" + cellType + "\t" + myId + "\t" + lastPosition.x + "\t" + lastPosition.y + "\t" + lastPosition.z + "\t" + lastAV.x + "\t" + lastAV.y +"\t" + lastAV.z + "\t" + lastDeltaAV.x +"\t" + lastDeltaAV.y +"\t" + lastDeltaAV.z + "\t" + lastLV.x + "\t" + lastLV.y + "\t" + lastLV.z +"\t" + cols +"\n");
			}
			catch(IOException e){
				sim.writeToLog(sim.getFormattedTime() + "\t" + "Unable to write to cell file" + "\t" + e.toString());
			}
		}
	}
	
	private float findTriangleArea(Vector3f[] vertices){
		//Find the lengths of the sides
		float a = (float)Math.sqrt(Math.pow((vertices[1].x - vertices[0].x), 2) +
				Math.pow((vertices[1].y - vertices[0].y), 2) + Math.pow((vertices[1].z - vertices[0].z), 2));
		float b = (float)Math.sqrt(Math.pow((vertices[2].x - vertices[1].x), 2) +
				Math.pow((vertices[2].y - vertices[1].y), 2) + Math.pow((vertices[2].z - vertices[1].z), 2));
		float c = (float)Math.sqrt(Math.pow((vertices[2].x - vertices[0].x), 2) +
				Math.pow((vertices[2].y - vertices[0].y), 2) + Math.pow((vertices[2].z - vertices[0].z), 2));
		//Use Heron's forumla
		float s = (float)(.5 * (a + b + c));
		
		float area = (float)(Math.sqrt(s * (s-a) * (s-b) * (s-c)));
		
		return area;
	}

	
	private static class triangleAreaCallback extends TriangleCallback {
		SegmentedCell parent;

		public triangleAreaCallback(SegmentedCell p) {
			this.parent = p;
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			//System.out.println("Processing triangle areas");
			//find the area
				
			float area = parent.findTriangleArea(triangle);
			parent.triangleAreas[triangleIndex] = area;
			//System.out.println("triangle " + triangleIndex + " area " + area);
			
		}
	}
	
	private static class triangleVerticesCallback extends TriangleCallback {
		SegmentedCell parent;

		public triangleVerticesCallback(SegmentedCell p) {
			this.parent = p;
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			parent.triangleVertices[triangleIndex] = new Vector3f[3];
			for (int i = 0; i < 3; i++){
				parent.triangleVertices[triangleIndex][i] = new Vector3f(triangle[i]);
			}
			
		}
		
	}
	
	private static class triangleVerticesOutputCallback extends TriangleCallback {
		SegmentedCell parent;
		Transform myTrans;

		public triangleVerticesOutputCallback(SegmentedCell p, Transform t) {
			this.parent = p;
			myTrans = new Transform(t);
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			String s = "Triangle " + triangleIndex + ": ";
			Vector3f[] myVertices = new Vector3f[3];
			for (int i = 0; i < 3; i++){
				myVertices[i] = new Vector3f(triangle[i]);
				myTrans.transform(myVertices[i]);
				s += myVertices[i];
				if (i < 2){
					s+= ", ";
				}
			}
			System.out.println(s);	
		}
		
	}

}
