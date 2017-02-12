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

import java.io.BufferedWriter;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import java.io.IOException;

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
	
	private BufferedWriter outputFile;
	
	private Vector3f origin;
	private float radius;
	private float density = 1.1f;
	private float volume, mass;
	private int numSegments;
	private static GImpactMeshSphere cellShape;
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
		this.radius = r;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;
		
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
		
		sim.setNeedsGImpact(true);
		sim.addSimulationObject(this);
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
		//TODO!!!
	}
	
	public void setDensity(float d){
		density = d;
		volume = (float)(4.0/3.0 * Math.PI * radius * radius * radius);
		mass = density * volume;

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
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		return "Need to write final output for Segmented Cell";
	}
	
	public void wrapup(){
		cell_ids = 0;
	}
	
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
	}
	
	public void writeOutput(){
		if (outputFile != null){
			try{
				outputFile.write("Writing output from Segmented Cell");
			}
			catch(IOException e){
				sim.writeToLog(sim.getFormattedTime() + "\t" + "Unable to write to cell file" + "\t" + e.toString());
			}
		}
	}

}
