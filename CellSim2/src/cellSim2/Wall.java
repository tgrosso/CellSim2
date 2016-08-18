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


import java.nio.FloatBuffer;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;


public class Wall implements SimObject{
	private static int wall_ids = 0;
	protected SimRigidBody body;
	protected BoxShape wallShape;
	protected Vector3f origin;
	protected float[] wallColor = {0.2f, 0.2f, 0.2f, 1f};
	protected float width, height, depth;
	protected static FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
	protected boolean visible = true;
	protected boolean toRemove = false;
	protected int id;
	protected Simulation sim;
	protected boolean bound = false;
	protected static boolean finalWritten = false;
	
	public Wall(Simulation s, float w, float h, float d, Vector3f o){
		float mass = 0;
		sim = s;
		width = w;
		height = h;
		depth = d;
		origin = o;
		Transform t = new Transform();
		t.setIdentity();
		t.origin.set(origin);
				
		Vector3f localInertia = new Vector3f(0, 0, 0);
		wallShape = new BoxShape(new Vector3f(width/2, height/2, depth/2));
		DefaultMotionState motionState = new DefaultMotionState(t);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, wallShape, localInertia);
		body = new SimRigidBody(rbInfo, this);
		
		this.id = wall_ids;
		wall_ids++;
	}
	
	public CollisionShape getCollisionShape(){
		return wallShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public void updateObject(){
		//TODO Find the proteins that are bound to the wall
		//Update each one for degradation over time
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
	
	public String finalOutput(){
		if (finalWritten){
			return "";
		}
		finalWritten = true;
		return "Need to write final output from CMWall";
	}
	
	public void wrapup(){
		wall_ids = 0;
	}
}
