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
import com.bulletphysics.util.ObjectArrayList;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;


public class Wall implements SimObject{
	public static int FRONT=0, BACK = 1, TOP = 2, BOTTOM = 3, LEFT = 4, RIGHT = 5;
	private static int wall_ids = 0;
	protected static long updateTime = 5 * 1000 * 1000; //update proteins every 5 seconds
	protected long lastUpdate;
	protected SimRigidBody body;
	protected BoxShape wallShape;
	protected Vector3f origin, size;
	protected float[] wallColor = {0.45f, 0.45f, 0.45f, 1f}; //bare wall is very light gray
	//protected float width, height, depth;
	protected static FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
	protected ObjectArrayList<SimObject> collidedObjects;

	protected boolean visible = true;
	protected float[] glMat = new float[16];
	protected int[][][] pointVec = {{{0, 0, 0}, {1, 0, 1}}, {{0, 0, 1}, {1, 0, 1}}, 
									{{0, 0, 1}, {2, 0, 2}}, {{0, 1, 1}, {2, 0, 2}},
									{{0, 0, 1}, {1, 2, 1}}, {{1, 0, 1}, {1, 2, 1}}};
	protected float[][] drawingVectors;
	protected Vector3f[] normals;
	
	protected boolean toRemove = false;
	protected int id;
	protected Simulation sim;
	protected boolean bound = false;
	protected static boolean finalWritten = false;
	protected SurfaceSegment[] segments;
	protected HashSet<Integer> surfaceProteins;
	protected HashSet<Integer> receptorsBindTo;
	
	protected int visibleProtein = -1;
	protected float[] visibleColor;
	//protected float distanceFromSource = 0f;
	protected BufferedWriter outputFile = null;
	
	public Wall(Simulation s, float w, float h, float d, Vector3f o){
		//Creates a Wall for this simulation with these dimensions and a center at o.
		//Walls have a base gray color. Any other color comes from a surface segment
		float mass = 0;
		sim = s;
		size = new Vector3f(w, h, d);
		origin = new Vector3f(o);
		Transform t = new Transform();
		t.setIdentity();
		t.origin.set(origin);
		visibleColor = wallColor;
				
		Vector3f localInertia = new Vector3f(0, 0, 0);
		wallShape = new BoxShape(new Vector3f(w/2, h/2, d/2));
		DefaultMotionState motionState = new DefaultMotionState(t);
		RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, wallShape, localInertia);
		//System.out.println("Wall friction: " + rbInfo.friction);
		body = new SimRigidBody(rbInfo, this);
		
		lastUpdate =  sim.getCurrentTimeMicroseconds();
		segments = new SurfaceSegment[0];
		surfaceProteins = new HashSet<Integer>();
		receptorsBindTo = new HashSet<Integer>();
		
		//Set up the start and end vectors for each surface
		drawingVectors = new float[2][3];
		drawingVectors[0][0] = o.x + w/2;
		drawingVectors[0][1] = o.y + h/2;
		drawingVectors[0][2] = o.z - d/2;
		drawingVectors[1][0] = o.x - w/2;
		drawingVectors[1][1] = o.y - h/2;
		drawingVectors[1][2] = o.z + d/2;
		
		normals = new Vector3f[6];
		normals[FRONT] = new Vector3f(0, 0, -1);
		normals[BACK] = new Vector3f(0, 0, 1);
		normals[TOP] = new Vector3f(0, 1, 0);
		normals[BOTTOM] = new Vector3f(0, -1, 0);
		normals[LEFT] = new Vector3f(1, 0, 0);
		normals[RIGHT] = new Vector3f(-1, 0, 0);
		
		this.id = wall_ids;
		wall_ids++;
		
		//System.out.println("wall number " + id);
		
		//System.out.println("Start point: " + drawingVectors[0][0] + ", " + drawingVectors[0][1] + ", " + drawingVectors[0][2]);
		//System.out.println("End point: " + drawingVectors[1][0] + ", " + drawingVectors[1][1] + ", " + drawingVectors[1][2]);
		
		Vector3f min = new Vector3f();
		Vector3f max = new Vector3f();
		body.getAabb(min, max);
		//System.out.println("Min: " + min.toString() + " max: " + max.toString()) ;
		
		collidedObjects = new ObjectArrayList<SimObject>();
	}
	
	public CollisionShape getCollisionShape(){
		return wallShape;
	}
	
	public SimRigidBody getRigidBody(){
		return body;
	}
	
	public SurfaceSegment getSurfaceSegment(int surface){
		if (segments.length == 0){
			return null;
		}
		for (int i = 0; i < segments.length; i++){
			if (segments[i].getID() == surface){
				return segments[i];
			}
		}
		return null;
	}
	
	public HashSet<Integer> getSurfaceProteins(){
		return surfaceProteins;
	}
	
	public HashSet<Integer> getReceptorsBindTo(){
		//Which proteins will bind to the coating receptors?
		return receptorsBindTo;
	}
	
	public TraffickingInfo getTraffickInfo(int pro, int id){
		//Walls don't traffick. Return zero values
		return new TraffickingInfo();
	}
	
	public void coatWithProtein(int proId, float surfaceConc, int surface){
		//System.out.println("I am wall " + id + " and I am being coated with protein " + proId);
		//First make the segment
		if (surface < 0 || surface > 5){
			System.err.println("Cannot coat wall " + id + " with protein. Surface value not valid: " + surface);
			return;
		}
		//Find the number of proteins
		//If this is the first protein, it is, by default, the visible one
		if (segments.length == 0){
			visibleProtein = proId;
			visibleColor = sim.getProtein(proId).getColor();
		}
		float surfaceArea = getSurfaceArea(surface);
		boolean surfaceHasSegment = false;
		for (int i = 0; i < segments.length; i++){
			if (segments[i].getID() == surface){
				//Don't add another segment - just add this protein to the segment
				//Walls don't start bonds, so the molsPerBond is not used
				segments[i].addReceptor(proId, surfaceConc*surfaceArea, 1);
				surfaceHasSegment = true;
				break;
			}
		}
		if (!surfaceHasSegment){
			SurfaceSegment seg = new SurfaceSegment(this, surface);
			SurfaceSegment[] newSegments = new SurfaceSegment[segments.length+1];
			//We don't already have a segment on that surface - make a new one
			for (int i = 0; i < segments.length; i++){
				newSegments[i] = segments[i];
			}
			seg.addReceptor(proId, surfaceConc*surfaceArea, 1);
			newSegments[segments.length] = seg;
			segments = newSegments;
		}
		//Add any ligands for this protein to the ligand list
		int[] lig = sim.getProtein(proId).getLigands();
		for (int i = 0; i < lig.length; i++){
			receptorsBindTo.add(new Integer(lig[i]));
		}
		surfaceProteins.add(new Integer(proId));
	}
	
	public float getSurfaceArea(int surface){
		//System.out.println("I'm a wall. Surface: " + surface + " my size is " + size);
		switch (surface){
		case 0:
		case 1://FRONT or BACK - area is width * height
			return size.x * size.y;
		case 2:
		case 3: //TOP or BOTTOM - area is width * depth
			return size.x * size.z;
		default: //LEFT or RIGHT - area is height * depth
			return size.y * size.z;
		}
	}
	
	public float getSegmentArea(int index){
		return getSurfaceArea(index);
	}
	
	public Gradient getGradient(int pro){
		return sim.getGradient(pro);
	}
	
	public Vector3f getNormal(int surface){
		return normals[surface];
	}
	
	public Vector3f getSegmentWorldNormal(int index){
		Transform myTrans = new Transform();
		body.getMotionState().getWorldTransform(myTrans);
		//System.out.println("My basis: " + myTrans.basis);
		Vector3f myNormal = new Vector3f(normals[index]);
		//System.out.println("my Wall Normal: " + myNormal);
		myTrans.transform(myNormal);
		myNormal.sub(myTrans.origin);
		myNormal.normalize();
		//System.out.println("My final normal" + myNormal);
		return myNormal;
	}
	
	public Vector3f[] getWorldCoordinates(int surface){
		Vector3f[] vertices = new Vector3f[4];
		switch(surface){
		case 0:
			//FRONT
			vertices[0] = new Vector3f(size.x/2, size.y/2, -size.z/2);
			vertices[1] = new Vector3f(-size.x/2, size.y/2, -size.z/2);
			vertices[2] = new Vector3f(-size.x/2, -size.y/2, -size.z/2);
			vertices[3] = new Vector3f(size.x/2, -size.y/2, -size.z/2);
			break;
		case 1:
			//BACK
			vertices[0] = new Vector3f(size.x/2, size.y/2, size.z/2);
			vertices[1] = new Vector3f(-size.x/2, size.y/2, size.z/2);
			vertices[2] = new Vector3f(-size.x/2, -size.y/2, size.z/2);
			vertices[3] = new Vector3f(size.x/2, -size.y/2, size.z/2);
			break;
		case 2:
			//TOP
			vertices[0] = new Vector3f(size.x/2, size.y/2, size.z/2);
			vertices[1] = new Vector3f(-size.x/2, size.y/2, size.z/2);
			vertices[2] = new Vector3f(-size.x/2, size.y/2, -size.z/2);
			vertices[3] = new Vector3f(size.x/2, size.y/2, -size.z/2);
			break;
		case 3:
			//BOTTOM
			vertices[0] = new Vector3f(size.x/2, -size.y/2, size.z/2);
			vertices[1] = new Vector3f(-size.x/2, -size.y/2, size.z/2);
			vertices[2] = new Vector3f(-size.x/2, -size.y/2, -size.z/2);
			vertices[3] = new Vector3f(size.x/2, -size.y/2, -size.z/2);
			break;
		case 4:
			//LEFT
			vertices[0] = new Vector3f(size.x/2, size.y/2, size.z/2);
			vertices[1] = new Vector3f(size.x/2, size.y/2, -size.z/2);
			vertices[2] = new Vector3f(size.x/2, -size.y/2, -size.z/2);
			vertices[3] = new Vector3f(size.x/2, -size.y/2, size.z/2);
			break;
		default:
			//RIGHT
			vertices[0] = new Vector3f(-size.x/2, size.y/2, size.z/2);
			vertices[1] = new Vector3f(-size.x/2, size.y/2, -size.z/2);
			vertices[2] = new Vector3f(-size.x/2, -size.y/2, -size.z/2);
			vertices[3] = new Vector3f(-size.x/2, -size.y/2, size.z/2);
		}
		Transform myTrans = new Transform();
		body.getMotionState().getWorldTransform(myTrans);
		for (int i = 0; i < 4; i++){
			myTrans.transform(vertices[i]);
			//System.out.println("Wall Vertices (" + surface + "): " + vertices[i].toString());
		}
		return vertices;
	}
	
	public int getVisibleProtein(){
		return visibleProtein;
	}
	
	public int getNumSegments(){
		if (segments == null){
			return 0;
		}
		return segments.length;
	}
	
	public void updateObject(){
		//TODO Find the proteins that are bound to the wall
		//Update each one for degradation over time
		//Only update the coatings every 5 seconds
		//TODO The frequency of wall updates should be a user parameter
		long deltaTime = sim.getCurrentTimeMicroseconds() - lastUpdate;
		if (deltaTime > updateTime){
			//update the segments
			if (segments != null){
				for (int i = 0; i < segments.length; i++){
					segments[i].update(sim.getCurrentTimeMicroseconds(), deltaTime, new Vector3f());
				}
			}
			lastUpdate = sim.getCurrentTimeMicroseconds();
		}
	}
	
	public Vector3f getColor3Vector(){
		return new Vector3f(wallColor[0], wallColor[1], wallColor[2]);
	}
	
	private void setColor(float value, int index){
		wallColor[index] = value;
	}
	
	public void setWallColor(float red, float green, float blue){
		//setWallColor is for initialization, not setting the current color
		setWallColor(red, green, blue, 1.0f);
	}
	
	public void setWallColor(float red, float green, float blue, float alpha){
		wallColor[0] = red;
		wallColor[1] = green; 
		wallColor[1] = blue; 
		wallColor[1] = alpha; 
		//System.out.println("Wall id " + id + " current color: " + Arrays.toString(wallColor));
	}
	
	public void setVisibleProtein(int proID){
		//Find out if the protein id is in the list of coating proteins
		if (segments == null || segments.length == 0){
			//No coating proteins. Do nothing
			return;
		}
		for (int i = 0; i < segments.length; i++){
			segments[i].setVisibleProtein(proID);
		}
		visibleProtein = proID;
		visibleColor = sim.getProtein(proID).getColor();
	}
	
	public String toString(){
		return ("I am wall number " + id);
	}
	
	public static String getDataHeaders(){
		String s = "Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
		return s;
	}
	
	public String getOutputData(){
		if (segments == null){
			return "";
		}
		String s = "";
		for (int i = 0; i < segments.length; i++){
			s = s + sim.getFormattedTime() + "\t" + this.id + "\t";
			s = s + segments[i].getOutput(sim);;
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
		//Walls don't do collisions. The other object will collide with the wall
		//System.out.println("Collided: Wall");
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
	
	public boolean specialRender(IGL gl, Transform t, int m){
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
	
	public boolean isMobile(){
		return false;
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
	
	public Protein getProtein(int id){
		return sim.getProtein(id);
	}
	
	public int getSurface(Vector3f v){
		//v is a point on the surface of the wall
		//It's the top if y >= height/2  
		if (v.y >= size.y/2){
			return TOP;
		}
		else if (v.y <= -size.y/2){
			return BOTTOM;
		}
		else if (v.x >= size.x/2){
			return LEFT;
		}
		else if (v.x <= -size.x/2){
			return RIGHT;
		}
		else if (v.z >= size.z/2){
			return BACK;
		}
		else{
			return FRONT;
		}
	}
	
	public boolean showingBoundProtein(){
		return false;
	}
	
	public String getSurfaceSegmentOutput(){
		String s = "";
		for (int i = 0; i < segments.length; i++){
			s += segments[i].getOutput(sim);
		}
		return s;
	}
		
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
		//System.out.println("I am wall number: " + id + " and my outputfile is " + outputFile.toString());
	}
	
	public void writeOutput(){
		//sim.writeToLog("I am wall " + id + " and I am writing output. My outputFile is " + outputFile.toString());
		//if (outputFile == null){
		//	System.out.println("I am wall " + id + " and I have no outputfile! :-(");
		//}
		if (outputFile != null && segments != null && segments.length > 0){
			//"Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
			for (int i = 0; i < segments.length; i++){
				String s = sim.getFormattedTime() + "\t" + getID() + "\t" + segments[i].getOutput(sim)+ "\n";
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
		if (outputFile != null && segments != null && segments.length > 0){
			//"Time Since Sim Start\tWall ID\tProtein\tSurface Concentration\n";
			String s = "Wall - ID" + getID() + "\n";
			for (int i = 0; i < segments.length; i++){
				s = s + segments[i].getFinalOutput();
			}
			return s;
		}
		else{
			return "Wall ID " + getID() + "\t No proteins\n";
		}
	}
	
	public void wrapup(){
		wall_ids = 0;
	}
}
