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
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.FloatBuffer;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;


public class Wall implements SimObject{
	private static int wall_ids = 0;
	protected SimRigidBody body;
	protected BoxShape wallShape;
	protected Vector3f origin, size;
	protected float[] wallColor = {0.2f, 0.2f, 0.2f, 1f};
	//protected float width, height, depth;
	protected static FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
	protected boolean visible = true;
	protected boolean toRemove = false;
	protected int id;
	protected Simulation sim;
	protected boolean bound = false;
	protected static boolean finalWritten = false;
	protected int[] coatPros;
	protected float[] coatConc;//initial concentrations of coating proteins
	protected long lastProteinUpdate;
	//protected float distanceFromSource = 0f;
	protected BufferedWriter outputFile = null;
	
	public Wall(Simulation s, float w, float h, float d, Vector3f o){
		float mass = 0;
		sim = s;
		size = new Vector3f(w, h, d);
		origin = new Vector3f(o);
		Transform t = new Transform();
		t.setIdentity();
		t.origin.set(origin);
				
		Vector3f localInertia = new Vector3f(0, 0, 0);
		wallShape = new BoxShape(new Vector3f(w/2, h/2, d/2));
		DefaultMotionState motionState = new DefaultMotionState(t);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, wallShape, localInertia);
		body = new SimRigidBody(rbInfo, this);
		
		lastProteinUpdate = sim.getCurrentTimeMicroseconds();
		
		this.id = wall_ids;
		wall_ids++;
	}
	
	public CollisionShape getCollisionShape(){
		return wallShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public void coatWithProtein(int proId, float surfaceConc){
		if (coatPros == null){
			coatPros = new int[1];
			coatConc = new float[1];
			coatPros[0] = proId;
			coatConc[0] = surfaceConc;
		}
		else{
			int[] newPros = new int[coatPros.length+1];
			float[] newConc = new float[coatConc.length+1];
			for (int i = 0; i < coatPros.length; i++){
				newPros[i] = coatPros[i];
				newConc[i] = coatConc[i];
			}
			newPros[coatPros.length] = proId;
			newConc[coatConc.length] = surfaceConc;
			coatPros = newPros;
			coatConc = newConc;
		}
	}
	
	public void updateObject(){
		//TODO Find the proteins that are bound to the wall
		//Update each one for degradation over time
		//Only update the coatings every 5 seconds
		if (sim.getCurrentTimeMicroseconds() - lastProteinUpdate > 5*1000*1000){
			//update the proteins
		}
	}
	
	public Vector3f getColor3Vector(){
		return new Vector3f(wallColor[0], wallColor[1], wallColor[2]);
	}
	
	public void setColor(float red, float green, float blue){
		setColor(red, green, blue, 1.0f);
	}
	
	public void setColor(float red, float green, float blue, float alpha){
		wallColor[0] = red;
		wallColor[1] = green; 
		wallColor[2] = blue;
		wallColor[3] = alpha;
	}
	
	public String toString(){
		return ("I am wall number " + id);
	}
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
		return s;
	}
	
	public String getOutputData(){
		if (coatPros == null){
			return "";
		}
		String s = "";
		for (int i = 0; i < coatPros.length; i++){
			s = s + sim.getFormattedTime() + "\t" + this.id + "\t";
			s = s + sim.getProteinName(coatPros[i]) + "\t" + coatConc[i] + "\n";
		}
		return s;
	}
	
	public void setVisible(boolean v){
		visible = v;
	}
	
	public boolean isVisible(){
		return visible;
	}
	
	public void collided(SimObject c, ManifoldPoint pt, long collId){
		
		//check out the proteins that are bound to this wall
		//Does the object have proteins that bind to these proteins?
		//If so, has the other body made bonds yet?
		//If yes, confirm all the bonds.
		//If not, have the other body make bonds?
	}
	
	public boolean specialRender(IGL gl, Transform t){
		return false;
	}
	
	public int getID(){
		return this.id;
	}
	
	public String getType(){
		String s = "Wall";
		return s;
	}
	
	public float getMass(){
		return (0.0f);
	}
	public Vector3f getSize(){
		return size;
	}
	
	public Vector3f getOrigin(){
		return origin;
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
		//TODO might not need this anymore
		return bound;
	}
	
	public void clearBound(){
		//TODO Might not need this anymore
		bound = false;
	}
	
	public void bind(){
		bound = true;
	}
	
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
	}
	
	public void writeOutput(){
		if (outputFile != null && coatPros != null && coatPros.length > 0){
			//"Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
			for (int i = 0; i < coatPros.length; i++){
				String s = sim.getFormattedTime() + "\t" + getID() + "\t" + sim.getProteinName(i) + "\t" + coatConc[i] + "\n";
				try{
					outputFile.write(s);
				}
				catch(IOException e){
					sim.writeToLog(sim.getFormattedTime() + "\t" + "Unable to write to wall file" + "\t" + e.toString());
				}
			}
		}
	}
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		if (outputFile != null && coatPros != null && coatPros.length > 0){
			//"Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
			String s = "";
			for (int i = 0; i < coatPros.length; i++){
				s = s +sim.getFormattedTime() + "\t" + getID() + "\t" + sim.getProteinName(i) + "\t" + coatConc[i] + "\n";
			}
			return s;
		}
		else{
			return "";
		}
	}
	
	public void wrapup(){
		wall_ids = 0;
	}
}
