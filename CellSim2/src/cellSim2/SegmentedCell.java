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

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;

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
	
	private BufferedWriter outputFile;
	
	private Vector3f origin, initialPosition, lastPosition;
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
	
	private float[] triangleAreas;
	

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
		//maxAngVel = (float)(Math.PI/Math.pow(2, detailLevel));
		//maxDeltaVel = (float)(Math.PI * 1800 * mass);
		//System.out.println("Max Delta Vel: " + maxDeltaVel);
		
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
		body = new SimRigidBody(rbInfo, this);
		numSegments = cellShape.getNumTriangles();
		
		float downForce = (mass * 9.8f) - (volume * 9.8f);
		body.setGravity(new Vector3f(0, -downForce, 0));
		//body.setAngularVelocity(getRandomVector(maxDeltaVel));
		
		sim.setNeedsGImpact(true);
		sim.addSimulationObject(this);
	}
	
	public SegmentedCell(Simulation s, float r, int dl) {
		this(s, new Vector3f(0, 0, 0), r, dl);
	}
	
	public void setInitialPosition(Vector3f o){
		this.origin = o;
		initialPosition = new Vector3f(o);
		lastPosition = new Vector3f(o);
		accumulatedDistance = 0;
		
		trans.setIdentity();
		trans.origin.set(this.origin);
		
		DefaultMotionState motionState = new DefaultMotionState(trans);
		body.setMotionState(motionState);
		System.out.println("my origin: " + this.origin.toString());
	}
	
	public static ArrayList<SegmentedCell> readInFile(String type, String filename, Simulation sim){
		File inFile = new File(filename);
		int numCells = 1;
		int detailLevel = 1;
		float radius = 10;
		float density = 1.01f;
		float volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		float mass = density * volume;
		float maxDeltaVel = (float)(Math.PI * 1800 * mass);
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
						System.out.println("radius: " + radius);
						continue;
					}
					if (var.equals("density")){
						density = Float.parseFloat(value);
						continue;
					}
					if (var.equals("maxDeltaVel")){
						maxDeltaVel = Float.parseFloat(value);
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
			cells.add(nextCell);
		}
		return cells;
	}
	
	public void setCellType(String s){
		cellType = s;
	}
	
	public void collided(SimObject c, ManifoldPoint mp, long collID){
		
	}
	
	public CollisionShape getCollisionShape(){
		return cellShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public void updateObject(){
		if (!body.isActive()){
			System.out.println("Cell " + this.myId + " has been deactivated.");
			body.activate();
		}
		float downForce = (mass * 9.8f) - (volume * 9.8f);
		body.setGravity(new Vector3f(0, -downForce, 0));
		
		if (sim.getDeltaTimeMicroseconds()>0){
			//float maxvel = (float)((Math.PI/30.0)/(sim.getDeltaTimeMicroseconds()/1000000));
			float maxvel = maxDeltaVel;
			int randomAxis = (int)(sim.getNextRandomF() * 3);
			float randomVel = sim.getNextRandomF() * maxvel;
			if (sim.getNextRandomF() >= .5){
				randomVel = -randomVel;
			}
			float[] addVal = new float[]{0f, 0f, 0f};
			addVal[randomAxis] = randomVel;
			Vector3f oldVel = new Vector3f(0, 0, 0);
			body.getAngularVelocity(oldVel);
			oldVel.add(new Vector3f(addVal));
			//System.out.println(oldVel);
			body.setAngularVelocity(oldVel);
		}
			//Find the current position
		this.body.getMotionState().getWorldTransform(trans);
		Vector3f distance = new Vector3f();
		distance.sub(trans.origin, lastPosition);
		accumulatedDistance += distance.length();
		lastPosition = new Vector3f(trans.origin);
	}
	
	private Vector3f getRandomVector(float mag){
		//System.out.println("Getting Random Delta Velocity");
		Vector3f deltaVel = new Vector3f(0f, 0f, 0f);
		float magnitude = (sim.getNextRandomF() * mag);
		float length_s = 2;
		float x = 0, y = 0, z = 0;
		while (length_s >= 1.0f){
			x = (Math.floor((sim.getNextRandomF() * 2)) > 0 ? 1 : -1) * sim.getNextRandomF();
			y = (Math.floor((sim.getNextRandomF() * 2)) > 0 ? 1 : -1) * sim.getNextRandomF();
			z = (Math.floor((sim.getNextRandomF() * 2)) > 0 ? 1 : -1) * sim.getNextRandomF();
			length_s = x*x + y*y + z*z;
		}
		length_s = (float)Math.sqrt(length_s);
		x = x / length_s * magnitude;
		y = y / length_s * magnitude;
		z = z / length_s * magnitude;
		
		deltaVel.set(x, y, z);
		
		//System.out.println("Mag: " + magnitude + " vel: " + deltaVel);
		return (deltaVel);
	}

	
	public void setDensity(float d){
		density = d;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;
	}
	
	public void setMaxDeltaVel(float m){
		maxDeltaVel = m;
	}
	
	public Vector3f getColor3Vector(){
		//draws itself, so no color vector necessary
		return (new Vector3f());
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
	
	public boolean specialRender(IGL gl, Transform t){
		return false;
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
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tCell Type\tCell ID\txPos\tyPos\tzPos\n";
		return s;
	}
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		Vector3f euclidDist = new Vector3f();
		euclidDist.sub(lastPosition, initialPosition);
		String s = "Segmented Cell (" + cellType + "): " + myId + "\n";
		s += ",Accumulated Distance," + accumulatedDistance + ",Euclidean Distance," + euclidDist.length(); 
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
		if (outputFile != null){
			try{
				outputFile.write(sim.getFormattedTime() + "\t" + cellType + "\t" + myId + "\t" + lastPosition.x + "\t" + lastPosition.y + "\t" + lastPosition.z +"\n");
			}
			catch(IOException e){
				sim.writeToLog(sim.getFormattedTime() + "\t" + "Unable to write to cell file" + "\t" + e.toString());
			}
		}
	}

}
