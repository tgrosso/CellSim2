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

/**
 * @author Terri Applewhite-Grosso
 *
 */


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

//import org.lwjgl.util.glu.Sphere;

import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;



public class Cell implements SimObject{
	protected static float radius;
	protected static float density;
	protected static float volume;
	protected static float mass;
	protected static float maxVelChange = 0.3f;
	protected static int cell_ids = 0;
	protected int id;
	protected Vector3f origin;
	private static SphereShape cellShape;
	protected SimRigidBody body;
	protected Transform trans;
	protected float cellFriction = .3f;
	protected static float[] baseCellColor = {1.0f, 1.0f, 1.0f};
	private float[] cellColor;
	protected float cameraDistance = 20f;
	protected boolean visible = true;
	protected int visibleProtein = -1;
	protected boolean boundProtein = false;
	protected boolean toRemove = false;
	protected Simulation sim;
	protected String objectType = "Cell";
	protected boolean bound = false;
	protected HashMap<Integer, TraffickingInfo> traffickRates;
	
	protected int baseProb = 40; //% probability that molecule will bind
	//protected int currentProb = baseProb; //For Uniform response, the probability that molecule will bind to the whole cell
	//protected int deltaProb = 10;
	
	private static boolean finalWritten = false;
	private BufferedWriter outputFile;
	
	public Cell(Simulation s, Vector3f o){
		this.origin = new Vector3f(o);
		this.sim = s;
		
		radius = 5.0f;
		density = 1.2f;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;
		cellShape = new SphereShape(radius);
		
		trans = new Transform();
		trans.setIdentity();
		trans.origin.set(origin);
		
		Vector3f localInertia = new Vector3f(0, 0, 0);
		cellShape.calculateLocalInertia(mass, localInertia);
		//System.out.println(localInertia);

		DefaultMotionState motionState = new DefaultMotionState(trans);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, cellShape, localInertia);
		rbInfo.friction = cellFriction;
		body = new SimRigidBody(rbInfo, this);
		
		this.id = cell_ids;
		cell_ids++;
		
		cellColor = new float[3];
		for (int i = 0; i < 3; i++){
			cellColor[i] = (float)(baseProb/100.0) * baseCellColor[i];
		}
		
		traffickRates = new HashMap<Integer, TraffickingInfo>();
	}
	
	public void updateObject(){
		//probability of binding moves towards the baseline
		if (!body.isActive()){
			//System.out.println("Cell " + this.id + " has been deactivated.");
			body.activate();
		}
		Vector3f oldVel = new Vector3f();
		body.getLinearVelocity(oldVel);
		oldVel.add(getRandomVector(maxVelChange));
		//Apply a random force to the cell
		body.setLinearVelocity(oldVel);

		//set the current origin
		body.getMotionState().getWorldTransform(trans);
		this.origin.set(trans.origin);
		//System.out.println("Cell " + this.id + " Origin: " + this.origin + " and visible?" + isVisible());
		//System.out.println("   and cell color is " + getColor3Vector());
	}
	
	public Vector3f getOrigin(){
		return this.origin;
	}
	
	public float getRadius(){
		return radius;
	}
	
	protected Vector3f getRandomVector(float mag){
		//System.out.println("Getting Random Delta Velocity");
		Vector3f deltaVel = new Vector3f(0f, 0f, 0f);
		float magnitude  = sim.getNextRandomF() * mag;
		float horAngle, verAngle, yMag, xMag, zMag;
		double h;
		//Simply get a random velocity vector
		//Get random horizontal angle between 0 and 2 * PI
		horAngle = (float)(sim.getNextRandomF() * 2 * Math.PI);
		//Get random vertical angle between -PI/2 to +PI/2
		verAngle = (float)(sim.getNextRandomF() * Math.PI - (Math.PI/2));
				
		yMag = (float)(magnitude * Math.sin(verAngle));
		h = magnitude * Math.cos(verAngle);
		xMag = (float)(Math.cos(horAngle)* h);
		zMag = (float)(Math.sin(horAngle) * h);
				
		deltaVel.set(xMag, yMag, zMag);
		
		//System.out.println(deltaVel);
		return (deltaVel);
	}
	
	public void setCellGravity(){
		//set the gravity for the cell - it is modified by boyancy
		//a = g(rho_water * volume - mass)/mass + rho_water * Volume
		//This assumes rho_water = 1
		float acceleration = (float)(9.8) * (volume - mass)/(mass + volume);
		body.setGravity(new Vector3f(0,acceleration,0));
	}
	
	private void setInitialVel(){
		float magnitude = sim.getNextRandomF() * maxVelChange;
		float hor_angle = (float)(sim.getNextRandomF() * 2 * Math.PI);
		float ver_angle = (float)((sim.getNextRandomF() * Math.PI) - Math.PI/2.0);
		float y_mag = (float)(magnitude * Math.sin(ver_angle));
		double h = magnitude * Math.cos(ver_angle);
		float x_mag = (float)(Math.cos(hor_angle)* h);
		float z_mag = (float)(Math.sin(hor_angle) * h);
		body.setLinearVelocity(new Vector3f(x_mag, y_mag, z_mag));
	}
	
	
	public CollisionShape getCollisionShape(){
		return cellShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public TraffickingInfo getTraffickInfo(int pro, int id){
		Integer i = new Integer(pro);
		if (traffickRates.containsKey(i)){
			return traffickRates.get(i);
		}
		else{
			return new TraffickingInfo();
		}
	}
	
	public Vector3f getColor3Vector(){
		return new Vector3f(cellColor[0], cellColor[1], cellColor[2]);
	}
	
	public void setVisible(boolean v){
		visible = v;
	}
	
	public boolean isVisible(){
		return visible;
	}
	
	public void collided(SimObject c, ManifoldPoint pt, long collId){

	}
	
	public boolean specialRender(IGL gl, Transform t){
		return false;
	}
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tCell\tID\tCenter of Mass-x\tCenter of Mass-y\tCenter of Mass-z\n";
		return s;
	}
	
	public String getOutputData(){
		//Find position
		this.body.getMotionState().getWorldTransform(trans);		
		String s = sim.getFormattedTime() +  "\tCell\t" + this.id + "\t" + trans.origin.x + "\t"  + trans.origin.y + "\t" + trans.origin.z + "\n";
		return s;
	}
	
	public void writeOutput(){
		if (outputFile != null){
			try{
				outputFile.write(getOutputData());
			}
			catch(IOException e){
				sim.writeToLog(sim.getFormattedTime() + "\tCould not write to Cell Data File\t" + e.toString() + "\n");
			}
		}
	}
	
	public String toString(){
		String s = "I am cell " + this.id;
		return s;
	}
	
	public int getID(){
		return this.id;
	}
	
	public void setType(String s){
		objectType = s;
	}
	
	public String getType(){
		return objectType;
	}
	
	public float getMass(){
		return mass;
	}
	
	public Simulation getSim(){
		return sim;
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
	
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
	}
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		return "Need to write final output from CMCell";
	}
	
	public void wrapup(){
		cell_ids = 0;
	}
	
	public Protein getProtein(int id){
		return sim.getProtein(id);
	}

}
