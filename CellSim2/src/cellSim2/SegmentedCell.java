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
import java.util.HashSet;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
//import com.bulletphysics.collision.shapes.SphereShape;
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
	private float accumulatedDistance;
	private float radius;
	private float density = 1.01f;
	private float volume, mass;
	private float maxDeltaVel=2; //units radians per second
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
	
	private static float[] glMat = new float[16];
	private static Vector3f aabbMax = new Vector3f(1e30f, 1e30f, 1e30f);
	private static Vector3f aabbMin = new Vector3f(-1e30f, -1e30f, -1e30f);

	private long[] pos;
	private long[] neg;

	/**
	 * Represents the basic cell that moves around in a simulation
	 */
	public SegmentedCell(Simulation s, Vector3f o, float r, int dl) {
		this.myId = cell_ids;
		cell_ids++;
		this.sim = s;
		
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
		
		cellShape = new GImpactMeshSphere(dl);
		cellShape.setLocalScaling(new Vector3f(radius, radius, radius));
		cellShape.updateBound();
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
		body.setAngularVelocity(getRandomVector(.1f));
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
			//System.out.println("triangle " + i + " surface area " + triangleAreas[i]);
		}
		
		triangleVertices = new Vector3f[numSegments][];
		
		triangleVerticesCallback tcb = new triangleVerticesCallback(this);
		cellShape.processAllTriangles(tcb, aabbMin, aabbMax);
		
		triangleCenters = new Vector3f[numSegments];
		for (int i = 0; i < numSegments; i++){
			Vector3f[] p = triangleVertices[i];
			float x = (p[0].x + p[1].x + p[2].x) / 3;
			float y = (p[0].y + p[1].y + p[2].y) / 3;
			float z = (p[0].z + p[1].z + p[2].z) / 3;
			triangleCenters[i] = new Vector3f(x, y, z);
		}
		
		
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
	
	public static ArrayList<SegmentedCell> readInFile(String type, String filename, Simulation sim){
		File inFile = new File(filename);
		int numCells = 1;
		int detailLevel = 1;
		float radius = 10;
		float density = 1.01f;
		float volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		float mass = density * volume;
		//What the hell is this maxDeltaVel?
		float maxDeltaVel = (float)(Math.PI * 1800 * mass);
		ArrayList<String[]> receptorInfo = new ArrayList<String[]>();
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
						if (valueVar.length < 5){
							System.err.println("Badly formatted input. receptor<tab>protein name<tab>secretion rate<tab><unbound internalization rate<tab>bound internalization rate.");
							System.err.println("Input was: " + line);
							continue;
						}
						receptorInfo.add(Arrays.copyOfRange(valueVar, 1, valueVar.length));
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
						System.out.println("Max delta vel " + maxDeltaVel);
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
			long secrete = 0;
			float internUnbound = 0;
			float internBound = 0;
			try{
				secrete = Long.parseLong(rec[1]);
				internUnbound = Float.parseFloat(rec[2]);
				internBound = Float.parseFloat(rec[3]);
			}
			catch(NumberFormatException e){
				System.err.println("Error parsing receptor " + rec[0]);
				System.err.println("Receptor not added");
				continue;
			}
			TraffickingInfo ti = new TraffickingInfo(secrete, internUnbound, internBound);
			for (int j = 0; j < numCells; j++){
				cells.get(j).addReceptor(proId, ti);
			}
		}	
		return cells;
	}
	
	public void addReceptor(int proID, TraffickingInfo ti){
		//Add the TraffickingInfo to the map
		traffickRates.put(proID, ti);
		//Find the steady state secretion rate for the whole cell
		float totalUnbound = ti.getSecretionRate() / ti.getUnboundIntRate();
		
		if (!hasReceptors){
			//no proteins yet!
			hasReceptors = true;
			visibleProtein = proID; //by default, the first protein is the default
			baseColor = getProtein(visibleProtein).getColor();
		}
		for (int i = 0; i < numSegments; i++){
			membraneSegments[i].addReceptor(proID, totalUnbound * triangleAreas[i]/surfaceArea);
		}
		
		int[] lig = sim.getProtein(proID).getLigands();
		for (int i = 0; i < lig.length; i++){
			receptorsBindTo.add(new Integer(lig[i]));
		}
		surfaceProteins.add(new Integer(proID));
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
		if (!hasReceptors){
			//Nothing to bind to!
			return;
		}
		
		int mpThis = 0;//this object is object A in the manifold point
		Vector3f myNormal = new Vector3f();
		Vector3f otherNormal = new Vector3f();
		int whichTriangle = -1;
		float myArea = 0;
		float otherAreaPercent = 0;
		SurfaceSegment mySegment = null;
		SurfaceSegment theirSegment = null;
		Vector3f[] vertices = new Vector3f[0];
		
		//if collID == 0, it's colliding with a wall. Else it's another type of object
		//System.out.println("\nCollision");
		if (collID == 0){
			//Working with a wall
			Wall theWall = (Wall)c;
			myNormal = mp.normalWorldOnB; //Points from Object B to Object A
			int wallSurface = -1;
			if (mp.index0 == -1){
				//walls return -1 for their index value. This means Cell is Object B
				mpThis = 1;
				whichTriangle = mp.index1;
				wallSurface = theWall.getSurface(mp.localPointA);
			}
			else{
				//Need the normal to point from Cell to Wall. If we are here, it's backwards.
				myNormal.negate();
				whichTriangle = mp.index0;
				wallSurface = theWall.getSurface(mp.localPointB);
			}
			otherNormal = theWall.getNormal(wallSurface);
			theirSegment = theWall.getSurfaceSegment(wallSurface);
			if (theirSegment == null){
				//no proteins on this surface
				return; 
			}
			//wallSurface = 0;
			mySegment = membraneSegments[whichTriangle];
			myArea = triangleAreas[whichTriangle];
			otherAreaPercent = Math.min(1.0f,myArea/theWall.getSurfaceArea(wallSurface));
			/*System.out.println("Cell Normal: " + myNormal.toString());
			System.out.println("whichTriangle: " + whichTriangle);*/
			//System.out.println("detail level: " + detailLevel + " myArea: " + myArea);
			/*System.out.println("Wall Surface: " + wallSurface); */
			vertices = theWall.getWorldCoordinates(wallSurface);
			//for (int i = 0; i < vertices.length; i++){
			//	System.out.println("wall vertices(" + i + "):" + vertices[i]);
			//}
			//System.out.println("Wall normal " + otherNormal);
		}
		else{
			//System.out.println("Hit a cell");
			//otherAreaPercent should be 1.0f
			return;
		}
		Transform myTrans = new Transform();
		body.getMotionState().getWorldTransform(myTrans);
		Vector3f[] myVertices = new Vector3f[3];
		for (int i = 0; i < 3; i++){
			myVertices[i] = new Vector3f(triangleVertices[whichTriangle][i]);
			//System.out.println("      Pre-transform vertex " + i + myVertices[i]);
			myTrans.transform(myVertices[i]);
			//System.out.println("      Post-transform vertex " + i + myVertices[i]);
		}
		//System.out.println("Ready to make bonds");
		//So I have both surface segments, the percent of their segment that I'm looking at,
		//the coordinates of their vertices (3 for triangle, 4 for rectangle) in the world
		//frame, and the coordinates of my vertices in world coordinates and the normal from the cell to the other surface
		//Do any of my surface proteins have ligands on the other segment?
		
		int[] myReceptors = mySegment.getProteins();
		int[] theirProteins = theirSegment.getProteins();
		for (int i = 0; i < myReceptors.length; i++){
			Protein p = sim.getProtein(myReceptors[i]);
			//System.out.println("Got protein: " + p.getName());
			
			float numRec = mySegment.getNumMolecules(myReceptors[i], false);
			//System.out.println("I have : " + numRec + " free receptors");
			if (numRec < 0){
				//This receptor is not on my segment - shouldn't ever happen!
				continue;
			}
			int[] ligands = p.getLigands(); //These are the ligands for this receptor
			//System.out.println("I have the ligands for the protein: " + p.getName());
			for (int j = 0; j < ligands.length; j++){
				int lig = ligands[j];
				float bondLength = p.getBondLength(j);
				float bindingRate = p.getBindingRate(j);
				float stableTime = p.getBondLifetime(j);
				//System.out.println(p.getName()+"/"+sim.getProteinName(lig)+": bondlength : " + bondLength + " bindingRate: " + bindingRate + " timeToStable " + stableTime);
				for (int k = 0; k < theirProteins.length; k++){
					if (lig == theirProteins[k]){
						//System.out.println(k + "Their surface has this ligand. Make bonds");
						//How many bonds should we attempt?
						//I need the number of unbound receptors on their surface
						float numLig = theirSegment.getNumMolecules(lig, false) * otherAreaPercent;
						//System.out.println("Their ligands: " + numLig);
						if (numLig < 0){
							System.out.println("Num ligands < 0");
							continue;
						}
						//Find the volume that these would be in
						//float volume = myArea * 0.01f; //in cubic microns
						//TODO Try volume of size of ectodomain -.02 for integrin http://cshperspectives.cshlp.org/content/3/3/a004994.full
						float volume = myArea * bondLength; //in cubic microns
						volume = myArea * .02f;
						float dTime = Math.min((sim.getDeltaTimeMicroseconds() / 1000000 / 60f), 1f/60);
						float ligConc = (float)((numLig * 10)/(volume * 6.02)); //converts concentration to microMolar
						int maxBonds = (int)(dTime * bindingRate * numRec * ligConc/Protein.MOLS_PER_BOND);
						//System.out.println("maxBonds: " + maxBonds + " dTime: " + dTime + " volume: " + volume);
						for (int m = 0; m < maxBonds; m++){
							numRec = mySegment.getNumMolecules(myReceptors[i], false);
							//System.out.println("Calling attemptBond " + m + " of " + maxBonds);
							if (numRec > 0 && numLig > 0){
								if (attemptBond(mySegment, myReceptors[i],  theirSegment, lig,  myVertices, vertices, myNormal, otherNormal, bondLength, stableTime)){
									numLig -= Protein.MOLS_PER_BOND;
								}
							}
						}
					}
				}
			}
		}
		
	}
	
	private boolean attemptBond(SurfaceSegment initiator, int initProtein, SurfaceSegment receiver, int recProtein, Vector3f[] initVerts, Vector3f [] recVerts, Vector3f initNormal, Vector3f recNormal, float maxLength, float bondTime){
		//System.out.println("\n\nAttempting a bond");
		if (!sim.constraintAvailable()){
			System.err.println("Must be a problem. Too many bonds. I am " + getType() + "-" + getID());
			return false;
		}
		//Get a random point on the initiator triangle
		//Note - to find a random point on the triangle:
		//http://adamswaab.wordpress.com/2009/12/11/random-point-in-a-triangle-barycentric-coordinates/
		//Get two vectors on the triangle
		Vector3f T1 = new Vector3f(initVerts[1]);//AB
		T1.sub(initVerts[0]);
		Vector3f T2 = new Vector3f(initVerts[2]);//AC
		T2.sub(initVerts[0]);
		/*
		System.out.println("initial 0: " + initVerts[0] + " initial 1: " + initVerts[1] + " initial 2: " + initVerts[2]);
		System.out.println("T1 :" + T1 + " T2: " + T2);*/
		//Get two random values
		float r = sim.getNextRandomF();
		float s = sim.getNextRandomF();
		while(r==s){ //make sure they are different
			r = sim.getNextRandomF();
		}
		if (r + s >= 1){
			r = 1 - r;
			s = 1 - s;
		}
		T1.scale(r);
		T2.scale(s);
		//System.out.println("r: " + r + " s: " + s + "T1 :" + T1 + " T2: " + T2);
		Vector3f randVec = new Vector3f(initVerts[0]);
		randVec.add(T1);
		randVec.add(T2);
		//sim.writeToLog("Random Triangle Point: " + randVec);
		//Get the normal for the receiver plane
		//System.out.println("recVerts[0]: " + recVerts[0] + " recVerts[1]: " + recVerts[1] + " recVerts[2]: " + recVerts[2]);
		
		//Are the normal from the initiating surface to the plane and the plane normal parallel?
		float dot = initNormal.dot(recNormal); //l dot n
		if (Math.abs(dot) < .005){
			//The line is parallel to the plane. Don't make a bond
			return false;
		}
		Vector3f num = new Vector3f(recVerts[0]);
		num.sub(randVec); //p0 - l0
		float numerator = num.dot(recNormal);//(p0 - l0) dot n
		float d = numerator/dot;
		Vector3f planePoint = new Vector3f(randVec);//l0
		Vector3f dist = new Vector3f(initNormal); // dl
		dist.scale(d);
		planePoint.add(dist);//l0 + dl
		//System.out.println("randVec " + randVec);
		//System.out.println("dot " + dot);
		//System.out.println("num: " + num);
		//System.out.println("numerator: " + numerator);
		//System.out.println("d: " + d);
		//System.out.println("dis t" + dist + " max length " +maxLength);
		//System.out.println("planePoint: " + planePoint);
		
		Vector3f lenVec = new Vector3f(planePoint);
		lenVec.sub(randVec);
		//System.out.println("lenVec length " + lenVec.length());
		//planePoint.sub(randVec);
		if (lenVec.length() <= maxLength){
			//System.out.println("Making bond");
			Transform transA = new Transform();
			initiator.getParent().getRigidBody().getMotionState().getWorldTransform(transA);
			Transform bas = new Transform(transA);
			transA.inverse();
			transA.transform(randVec);
			transA.origin.set(randVec);
			transA.basis.set(bas.basis);
			
			Transform transB = new Transform();
			receiver.getParent().getRigidBody().getMotionState().getWorldTransform(transB);
			bas = new Transform(transB);
			transB.inverse();
			transB.transform(planePoint);
			transB.origin.set(planePoint);
			transB.basis.set(bas.basis);
			
			BondConstraint bc = new BondConstraint(sim, bondTime, initiator, receiver, initProtein, recProtein, initiator.getParent().getRigidBody(), receiver.getParent().getRigidBody(), transA, transB, true);
		}
		else{
			//System.out.println("Not making bond too far apart");
		}
		return true;
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
			
			Vector3f addVel = getRandomVector(maxDeltaVel);
			lastDeltaAV = new Vector3f(addVel);
			//System.out.print("addVel: " + addVel.toString());
			//Normals - check for close to 0
			//System.out.print("mag: " + mag + " Random addVel: " + addVel);
			//addVel.normalize();
			//System.out.print(" normalized: " + addVel);
			//System.out.println("  scaled: " + addVel);
			Vector3f oldVel = new Vector3f(0, 0, 0);
			this.body.getAngularVelocity(oldVel);
			//System.out.print(" Oldvel: " + oldVel.toString());
			oldVel.add(addVel);
			//System.out.println(" new vel: " + oldVel.toString());
			//System.out.println(oldVel);
			this.body.setAngularVelocity(oldVel);
		}
			//Find the current position
		this.body.getMotionState().getWorldTransform(trans);
		Vector3f distance = new Vector3f();
		distance.sub(trans.origin, lastPosition);
		accumulatedDistance += distance.length();
		lastPosition = new Vector3f(trans.origin);
		this.body.getAngularVelocity(lastAV);
		this.body.getLinearVelocity(lastLV);
		
		int numSurfaces = membraneSegments.length;
		//TODO We are going to set the axis to be the x for now! 
		//We probably want the distance from source to be attached to the gradient
		for (int i = 0; i < numSurfaces; i++){
			Vector3f cen = new Vector3f(triangleCenters[i]);
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
		maxDeltaVel = m;
	}
	
	private Vector3f getRandomVector(float mag){
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
			sum = randX1 * randX1 + randX2 * randX2;
		}
		float xval = (float)(2 * randX1 * Math.sqrt(1 - oneSquared - twoSquared));
		float yval = (float)(2 * randX2 * Math.sqrt(1 - oneSquared - twoSquared));
		float zval = (float)(1 - 2 * (oneSquared + twoSquared));
		Vector3f vec = new Vector3f(xval, yval, zval);
		vec.scale(mag);
		return vec;
	}
	
	public boolean hasTraffickInfoForProtein(int id){
		Integer i = new Integer(id);
		return traffickRates.containsKey(i);
	}
	
	public TraffickingInfo getTraffickInfo(int pro, int id){
		if (traffickRates.containsKey(pro)){
			TraffickingInfo ti = traffickRates.get(pro);
			long sec = (long)(ti.getSecretionRate() * triangleAreas[id]/surfaceArea);
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
	
	public boolean specialRender(IGL gl, Transform t){
		gl.glPushMatrix();
		t.getOpenGLMatrix(glMat);
		gl.glMultMatrix(glMat);
		
		drawSegmentsCallback drawCallback = new drawSegmentsCallback(gl, this);
		cellShape.processAllTriangles(drawCallback, aabbMin, aabbMax);
		gl.glPopMatrix();
		
		return true;
	}
	
	private static class drawSegmentsCallback extends TriangleCallback {
		private IGL gl;
		SegmentedCell parent;

		public drawSegmentsCallback(IGL gl, SegmentedCell p) {
			this.gl = gl;
			this.parent = p;
		}
		
		public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
			float[] color = parent.getBaseColor();
			if (parent.visibleProtein > 0){
				SurfaceSegment seg = parent.membraneSegments[triangleIndex];
				float percent = seg.getProteinPercentage(parent.getVisibleProtein(), parent.showingBoundProtein());
				for (int i = 0; i < 3; i++){
					color[i] = color[i] * percent;
				}
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
			gl.glColor3f(0f, 0f, 0f);
			gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			gl.glVertex3f(triangle[1].x, triangle[1].y, triangle[1].z);
			gl.glVertex3f(triangle[2].x, triangle[2].y, triangle[2].z);
			gl.glVertex3f(triangle[0].x, triangle[0].y, triangle[0].z);
			gl.glEnd();
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
		for (int i = 0; i < numSegments; i++){
			s += membraneSegments[i].getOutput(sim);
		}
		return s;
	}
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tCell Type\tCell ID\txPos\tyPos\tzPos\txLastAV\tyLastAV\tzLastAV\txLastDeltaAV\tyLastDeltaAV\tzLastDeltaAV\ttheta\txLastLV\tyLastLV\txLastLV\n";
		return s;
	}
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		Vector3f euclidDist = new Vector3f();
		euclidDist.sub(lastPosition, initialPosition);
		String s = "Segmented Cell (" + cellType + " id:" + myId + ")";
		s += "\tAccumulated Distance\t" + accumulatedDistance + "\tEuclidean Distance," + euclidDist.length();
		s+= "\tx:\tpos:"+ pos[0]+", neg:"+neg[0]+ "\ty:\tpos:"+ pos[1]+", neg:"+neg[1] + "\tz:\tpos:"+ pos[2]+", neg:"+neg[2];
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
		float hyp = (float)Math.sqrt(lastDeltaAV.x * lastDeltaAV.x + lastDeltaAV.z * lastDeltaAV.z);
		float sineTheta = lastDeltaAV.z / hyp;
		float theta = (float)Math.asin(sineTheta);
		if (outputFile != null){
			try{
				outputFile.write(sim.getFormattedTime() + "\t" + cellType + "\t" + myId + "\t" + lastPosition.x + "\t" + lastPosition.y + "\t" + lastPosition.z + "\t" + lastAV.x + "\t" + lastAV.y +"\t" + lastAV.z + "\t" + lastDeltaAV.x +"\t" + lastDeltaAV.y +"\t" + lastDeltaAV.z + "\t" + theta + "\t" + lastLV.x + "\t" + lastLV.y + "\t" + lastLV.z +"\n");
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
	

}
