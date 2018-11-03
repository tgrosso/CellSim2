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

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.util.ObjectArrayList;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
import com.bulletphysics.ContactAddedCallback;
import com.bulletphysics.demos.basic.BasicDemo;
import com.bulletphysics.demos.opengl.DemoApplication;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.demos.opengl.GLShapeDrawer;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.bulletphysics.dynamics.constraintsolver.Generic6DofConstraint;
import com.bulletphysics.extras.gimpact.GImpactCollisionAlgorithm;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.DebugDrawModes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import javax.vecmath.Vector3f;

import static com.bulletphysics.demos.opengl.IGL.*;

public class Simulation extends DemoApplication{
	private int MAX_CONSTRAINTS = 40000;
	
	private CollisionDispatcher dispatcher;
	private DefaultCollisionConfiguration collisionConfiguration;
	private BroadphaseInterface broadphase;
	private ConstraintSolver solver;
	
	private ObjectArrayList<SimObject> modelObjects;
	private ObjectArrayList<Gradient> gradients;
	private ObjectArrayList<BondConstraint> constraints;

	private float baseCameraDistance = 150;
	private boolean needGImpact = false;

	private ImageGenerator imageGen;
	private boolean render = true, finished = false;
	private SimGenerator simValues;
	//private long oldTime, currentTime, startTime, clockTime, lastDataOutput, deltaTime;
	private long realCurrentTime, realDeltaTime, realStartTime, simCurrentTime, simDeltaTime, simStartTime, lastDataOutput, delayTime;
	private float averageDeltaTime;
	private long numFrames;
	private SimObject[][] processedCollisions;
	
	private Random random;
	
	private BufferedWriter logFile, cellData, wallData, gradientTestFile, constraintFile, segmentData;
	private ObjectArrayList<BufferedWriter> gradientDataFiles;
	
	int testWidth;
	
	public Simulation(IGL gl, SimGenerator s) {
		super(gl);
		simValues = s;
		modelObjects = new ObjectArrayList<SimObject>();
		constraints = new ObjectArrayList<BondConstraint>();
		//startTime is the underlying clock time. clockTime - startTime = currentTime
		realStartTime = clock.getTimeMicroseconds();
		realCurrentTime = realStartTime;
		//current time is microseconds since start of simulation
		realDeltaTime = 0L;
		lastDataOutput = -1;
		averageDeltaTime = 0f;
		numFrames = 0;
		delayTime = 4 * 1000000;
		
		simCurrentTime = realStartTime;
		simDeltaTime = (long)(1000000/60f) * simValues.speedUp;
		simStartTime = realCurrentTime;
		
		
		//TODO ImageGenerator is just a stub right now
		imageGen = new ImageGenerator();
		
		random = new Random();
		gradientDataFiles = new ObjectArrayList<BufferedWriter>();
		testWidth = 1200;
				
		//Set up the output files
		try{
			logFile = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "logFile.csv")));
			cellData = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "cellData.csv")));
			cellData.write(SegmentedCell.getDataHeaders());
			wallData = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "wallData.csv")));
			wallData.write(Wall.getDataHeaders());
			for (int i = 0; i < simValues.gradients.size(); i++){
				Gradient g = simValues.gradients.get(i);
				String proteinName = getProteinName(g.getProtein());
				BufferedWriter gradData = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "gradient"+proteinName+"_"+i+".csv")));
				gradientDataFiles.add(gradData);
				gradData.write(simValues.gradients.get(i).getDataHeaders());
				g.setOutputFile(gradData);
			}
			gradientTestFile = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "gradientTest.csv")));
			String str = "Time Since Start\tProtein";
			for (int i = 0; i < testWidth; i+=100){
				str += "\t" + i;
			}
			str += "\n";
			gradientTestFile.write(str);
			constraintFile = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "constraintData.csv")));
			constraintFile.write(BondConstraint.getDataHeaders());
			segmentData = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "segmentData.csv")));
			segmentData.write("Time Since Start\tParent Object\tSegment ID\tProtein\tUnbound Receptors\tBound Receptors\n");
		}
		catch (IOException e){
			System.err.println("Cannot generate output files!");
		}
		
		BulletGlobals.setContactAddedCallback(new SimContactAddedCallback());
		processedCollisions = new SimObject [0][2];
		writeToLog(getFormattedTime() + "\tInitialization Complete");
		
		//Shit - Why am I doing this?
		//TODO Figure out what I am doing this!!
		/*
		Protein integ = getProtein(getProteinId("Integrin"));
		int lam = getProteinId("Laminin");
		int[] ids = integ.getLigands();
		for (int i = 0; i < ids.length; i++){
			if (ids[i] == lam){
				//System.out.println("Integrin/Laminin Bond Length " + integ.getBondLength(i));
			}
		}*/
			
		
	}
	
	@Override
	public void initPhysics(){
		// collision configuration contains default setup for memory, collision setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);

		broadphase = new DbvtBroadphase();

		// the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
		solver = sol;
				
		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

		//Simulation takes place in fluid.  Gravity is set for individual cells
		dynamicsWorld.setGravity(new Vector3f(0f, 0f, 0f));
		clientResetScene();
		simValues.createWalls(this);
		//writeToLog(getFormattedTime() + "\tWalls created");
		//writeToLog(getFormattedTime() + "\t" +simValues.simMax[0] + "," + simValues.simMax[1] + "," + simValues.simMax[2]);
		//writeToLog(getFormattedTime() + "\t" +simValues.simMin[0] + "," + simValues.simMin[1] + "," + simValues.simMin[2]);
		simValues.createCells(this);

		if (needGImpact){
			GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
		}
		
		
		writeToLog(getFormattedTime() + "\tFinished Init Physics");
	}
		
	@Override
	public void displayCallback() {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		renderme();

		// optional but useful: debug drawing to detect problems
		if (dynamicsWorld != null) {
			dynamicsWorld.debugDrawWorld();
		}
	}
	
	@Override
	public void clientMoveAndDisplay() {
		
		float secSinceOutput = (float)((simCurrentTime - lastDataOutput)/(1.0e6));
		if (simCurrentTime > delayTime && (lastDataOutput < 0 || secSinceOutput >= simValues.secBetweenOutput)){
			outputData();
			lastDataOutput = simCurrentTime;
		}
		int numObjects = modelObjects.size();
		
		boolean mobileObjs = false;
		//Vector3f testVec = new Vector3f();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			/*
			if (bioObj.getType().equals("Segmented Cell")){
				SimRigidBody b = bioObj.getRigidBody();
				b.getCenterOfMassPosition(testVec);
				System.out.println(getFormattedTime() + " COM Pos before update: " + testVec.toString());
				//b.getLinearVelocity(testVec);
				//System.out.println(getFormattedTime() + " linV before update: " + testVec.toString());
			}*/
			bioObj.updateObject();
			/*
			if (bioObj.getType().equals("Segmented Cell")){
				SimRigidBody b = bioObj.getRigidBody();
				b.getCenterOfMassPosition(testVec);
				System.out.println("COM Pos after update: " + testVec.toString());
				//b.getLinearVelocity(testVec);
				//System.out.println(getFormattedTime() + " linV after update: " + testVec.toString());
			}*/
			bioObj.clearCollisions();
			if (bioObj.isMobile()){
				mobileObjs = true;
			}
		}
		if (!mobileObjs){
			finished = true;
		}
		
		
		int numConstraints = constraints.size();
		//System.out.println("Num constraints " + numConstraints);
		//int remConstraints = 0;
		for (int i = numConstraints-1; i >=0; i--){
			BondConstraint bc = constraints.getQuick(i);
			bc.update();
			if (!bc.isActive()){
				removeConstraint(bc);
				//remConstraints++;
			}
		}
		//System.out.println("Removed: " + remConstraints + " constraints.");
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		long clockTime = clock.getTimeMicroseconds();
		realDeltaTime = clockTime - realCurrentTime;
		realCurrentTime = clockTime - realStartTime;
		numFrames++;
		averageDeltaTime = (averageDeltaTime * (numFrames-1) + realDeltaTime) / numFrames;
		
		simCurrentTime = simCurrentTime + simDeltaTime;
		
		
		// step the simulation
		if (dynamicsWorld != null) {
			//maxSubSteps * default frame rate (1/60) must be > time step
			/*for (int i = 0; i < numObjects; i++){
				SimObject bioObj = modelObjects.getQuick(i);
				
				if (bioObj.getType().equals("Segmented Cell")){
					//System.out.print("object: " + bioObj.toString());
					SimRigidBody b = bioObj.getRigidBody();
					b.getCenterOfMassPosition(testVec);
					System.out.println("COM Pos before step: " + testVec.toString());
					//b.getLinearVelocity(testVec);
					//System.out.println(getFormattedTime() + " linV before step: " + testVec.toString());
				}
			}*/
			
			float time_step = simDeltaTime / 1000000f;
			int max_substeps = (int)Math.ceil(time_step * 60f);
			dynamicsWorld.stepSimulation(time_step, max_substeps);
			//System.out.println("Simulation stepped");
			/*for (int i = 0; i < numObjects; i++){
				SimObject bioObj = modelObjects.getQuick(i);
				if (bioObj.getType().equals("Segmented Cell")){
					SimRigidBody b = bioObj.getRigidBody();
					b.getCenterOfMassPosition(testVec);
					System.out.println("COM Pos after step: " + testVec.toString());
					//bioObj.getRigidBody().getLinearVelocity(testVec);
					//System.out.println(" linV after step: " + testVec.toString());
				}
			}*/
			// optional but useful: debug drawing
			dynamicsWorld.debugDrawWorld();

		}
		
		//Remove any objects that have moved out of range 
		//We go through them backwards so that we don't mess up the indices
		Vector3f tempMin = new Vector3f();
		Vector3f tempMax = new Vector3f();
		float[] tMin  = new float[3];
		float[] tMax = new float[3];
		for (int i = numObjects-1; i >= 0; i--){
			SimObject bioObj = modelObjects.getQuick(i);
			bioObj.getRigidBody().getAabb(tempMin, tempMax);
			tempMin.get(tMin);
			tempMax.get(tMax);
			boolean inside = true;
			for (int j = 0; j < 3; j++){
				if (tMin[j] > simValues.simMax[j] || tMax[j] < simValues.simMin[j]){
					inside = false;
					break;
				}
			}
			if (!inside){
				bioObj.writeOutput();
				writeToLog(bioObj.finalOutput());
				writeToLog(bioObj.toString() + " removed at " + getFormattedTime());
				removeSimulationObject(bioObj);
				bioObj.destroy();
			}
		}
		

		if (simValues.displayImages){
			renderme();
			//glFlush();
			//glutSwapBuffers();
		}
		
		
		if (simCurrentTime >= simValues.endTime*1e6){
			finished = true;
		}
	}
	
	@Override
	public void myinit(){
		super.myinit();
		setCameraDistance(baseCameraDistance);
		ele = 0f;
		updateCamera();
		writeToLog(getFormattedTime() + "\tMyInit Complete");
	}
	
	@Override
	public void renderme() {
		updateCamera();
		
		Transform m = new Transform();
		if (dynamicsWorld != null) {
			int numObjects = modelObjects.size();
			for (int i = 0; i < numObjects; i++){
				SimObject bioObj = modelObjects.getQuick(i);
				if (bioObj.isVisible()){
					RigidBody rb = bioObj.getRigidBody();
					DefaultMotionState motionState = (DefaultMotionState)rb.getMotionState();
					m.set(motionState.graphicsWorldTrans);
					if (!bioObj.specialRender(gl, m)){
						GLShapeDrawer.drawOpenGL(gl, m, bioObj.getCollisionShape(), bioObj.getColor3Vector(), getDebugMode());
					}
				}
			}
			/*
			gl.glBegin(GL_LINES);
			gl.glColor3f(0f, 0f, 0f);
			gl.glVertex3f(350, -45, 250);
			gl.glVertex3f(-350, -45, 250);
			gl.glVertex3f(0, -45, 250);
			gl.glVertex3f(0, -45, -250);
			gl.glColor3f(1f, 0f, 0f);
			gl.glVertex3f(350, -90, 250);
			gl.glVertex3f(-350, -90, 250);
			//gl.glVertex3f(-250, -90, -350);
			//gl.glVertex3f(-250, -90, 350);
			//gl.glVertex3f(250, -90, -350);
			//gl.glVertex3f(250, -90, 350);
			gl.glVertex3f(-350, -90, -250);
			gl.glVertex3f(350, -90, -250);
			gl.glEnd();
			
			
			gl.glEnable(GL_LIGHTING);
			*/
			Transform ta = new Transform(), tb = new Transform();
			int numConstraints = dynamicsWorld.getNumConstraints();
			for (int i = 0; i < numConstraints; i++){
				TypedConstraint tc = dynamicsWorld.getConstraint(i);
				if (tc instanceof Generic6DofConstraint){
					Generic6DofConstraint gc = (Generic6DofConstraint)tc;
					gc.getCalculatedTransformA(ta);
					gc.getCalculatedTransformB(tb);
					gl.glBegin(GL_LINES);
					gl.glColor3f(0f, 0f, 0f);
					gl.glVertex3f(ta.origin.x, ta.origin.y, ta.origin.z);
					gl.glVertex3f(tb.origin.x, tb.origin.y, tb.origin.z);
					gl.glEnd();
				}
			}
		}
	}

	
	public void addSimulationObject(SimObject obj){
		//This method adds an object to the simulation
		modelObjects.add(obj);
		dynamicsWorld.addRigidBody(obj.getRigidBody());
		if (obj.getType().equals("Cell")){
			obj.setOutputFile(cellData);
		}
		else if(obj.getType().equals("Wall")){
			obj.setOutputFile(wallData);
		}
	}
	
	public void removeSimulationObject(SimObject obj){
		//Ugh!  How do I do this?
		dynamicsWorld.removeRigidBody(obj.getRigidBody());
		modelObjects.remove(obj);
	}
	
	public void addConstraint(BondConstraint c){
		dynamicsWorld.addConstraint(c);
		constraints.add(c);
	}
	
	public boolean constraintAvailable(){
		int num_constraints = constraints.capacity();
		return (num_constraints < MAX_CONSTRAINTS);
	}
	
	public void removeConstraint(BondConstraint c){
		try{
			constraintFile.write(c.getOutput());
		}
		catch(IOException e){
			System.err.println("Could not write to bond constraint file");
		}
		
		dynamicsWorld.removeConstraint(c);
		constraints.remove(c);
		c.destroy();
	}
	
	private void outputData(){
		int numObjects = modelObjects.size();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			bioObj.writeOutput();
			try{
				segmentData.write(bioObj.getSurfaceSegmentOutput());
			}
			catch(IOException e){
				System.err.println("Could not write Surface Segment data");
			}
		}
		try{
			constraintFile.flush();
			segmentData.flush();
			cellData.flush();
		}
		catch(IOException e){
			System.err.println("Could not flush when writing data");
		}
		/*
		int numConstraints = constraints.size();
		for (int i = 0; i < numConstraints; i++){
			BondConstraint bc = constraints.getQuick(i);
			try{
				constraintFile.write(bc.getOutput());
			}
			catch(IOException e){
				System.err.println("Could not write constraint data.");
			}
		}*/
		for (int i = 0; i < simValues.gradients.size(); i++){
			Gradient g = simValues.gradients.get(i);
			g.writeOutput(this);
			//Debugging
			String s = getFormattedTime() + "\t" + getProteinName(g.getProtein());
			for (int j = 0; j < testWidth; j+=100){
				s = s + "\t" + g.getConcentration(simCurrentTime, new Vector3f(j, 0, 0));
			}
			s += "\n";
			try{
				gradientTestFile.write(s);
			}
			catch(IOException e){
				System.err.println("Could not write to gradient test file");
			}
		}
		//writeToLog("Constraints\t" + constraints.size() + "\n");
		
	}
	
	public boolean renderDisplay(){
		return render;
	}
	
	public void setBaseCameraDistance(float d){
		baseCameraDistance = d;
	}
	
	public String getProteinName(int id){
		return simValues.getProteinName(id);
	}
	
	public Protein getProtein(int id){
		return simValues.proteins.get(id);
	}
	
	public int getProteinId(String name){
		int num_pro = simValues.proteins.size();
		for (int i = 0; i < num_pro; i++){
			Protein p = simValues.proteins.get(i);
			if (p.getName().equalsIgnoreCase(name)){
				return i;
			}
		}
		return -1;
	}
	
	public ArrayList<Gradient> getGradients(){
		return simValues.gradients;
	}
	
	public Gradient getGradient(int pro){
		ArrayList<Gradient> grads = getGradients();
		int numGrads = grads.size();
		for (int i = 0; i < numGrads; i++){
			Gradient g = grads.get(i);
			if (g.getProtein() == pro){
				return g;
			}
		}
		return null;
	}
	
	public float getDistanceFromSource(){
		return simValues.distFromSource;
	}
	
	public float getDistanceFromSource(float x){
		return (x - simValues.startX + simValues.distFromSource);
	}
	
	public long getCurrentTimeMicroseconds(){
		return simCurrentTime;
	}
	
	@Override
	public float getDeltaTimeMicroseconds(){
		return (float)simDeltaTime;
	}
	
	public void setNeedsGImpact(boolean b){
		needGImpact = b;
	}
	
	public BufferedWriter getWallFile(){
		return wallData;
	}
	
	public BufferedWriter getCellFile(){
		return cellData;
	}
	
	public String getFormattedTime(){
		long millisec = simCurrentTime / 1000;
		long mil = millisec % 1000;
		long sec = (millisec / 1000) % 60;
		long min = (millisec / (1000 * 60)) % 60;
		long hour = (millisec / (1000 * 60 * 60));
		String time = String.format("%02d:%02d:%02d.%03d", hour, min, sec, mil);
		return time;
	}
	
	public float getNextRandomF(){
		return random.nextFloat();
	}
	
	public boolean getNextRandomBool(){
		return random.nextBoolean();
	}
	
	public boolean readyToQuit(){
		return finished;
	}
	
	public ByteBuffer getImageBuffer(int w, int h){
		return imageGen.getBuffer(simValues.screenWidth, simValues.screenHeight);
	}
	
	public boolean timeToOutputImage(){
		//TODO write this!
		return false;
	}
	
	public void outputImage(){
		//TODO generate image file
	}
	
	public void writeToLog(String s){
		if (logFile != null){
			try{
				logFile.write(getFormattedTime() + "\t" + s);
				logFile.newLine();
				logFile.flush();
			}
			catch(IOException e){
				System.err.println("Error writing to log file! ");
				System.err.println(e.toString());
			}
		}
	}
	
	
	
	public void wrapUp(){
		writeToLog("\n" + getFormattedTime() + "\tFinishing Up");
		writeToLog("Current sim time (microseconds)\t" + simCurrentTime);
		writeToLog("Actual clock time (real microseconds)\t" + clock.getTimeMicroseconds());
		writeToLog("Average frame time (real microseconds)\t" + averageDeltaTime);
		int numObjects = modelObjects.size();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			writeToLog(bioObj.finalOutput());
			try{
				segmentData.write(bioObj.getSurfaceSegmentOutput());
			}
			catch(IOException e){
				System.err.println("Could not write Surface Segment data");
			}
		}
		
		
		try{
			int numConstraints = constraints.size();
			//writeToLog("Constraints\t"+numConstraints);
			if (simCurrentTime < simValues.endTime*1e6){
				writeToLog("Time Not Complete\t" + simCurrentTime);
			}
			for (int i = 0; i < numConstraints; i++){
				BondConstraint bc = constraints.getQuick(i);
				constraintFile.write(bc.getOutput());
			}
			constraintFile.flush();
			constraintFile.close();
	
			logFile.flush();
			logFile.close();
			cellData.flush();
			cellData.close();
			wallData.flush();
			wallData.close();
			segmentData.flush();
			segmentData.close();
			
			for (int i = 0; i < gradientDataFiles.size(); i++){
				gradientDataFiles.get(i).flush();
				gradientDataFiles.get(i).close();
			}
			gradientTestFile.flush();
			gradientTestFile.close();
		}
		catch(IOException e){
			String s = "Unable to close output files";
			
			try{ 
				logFile.write(s);
				logFile.write(e.toString());
				logFile.flush();
				logFile.close();
			}
			
			catch(IOException e1){
				e1.printStackTrace();
			}
		//System.out.println(e.toString());
		}
	}
	
	private class SimContactAddedCallback extends ContactAddedCallback{
		
		public boolean contactAdded(ManifoldPoint cp, CollisionObject colObj0, int partId0, int index0, CollisionObject colObj1, int partId1, int index1){
			SimRigidBody b0 = (SimRigidBody)colObj0, b1 = (SimRigidBody)colObj1;
			SimObject s0 = b0.getParent(), s1 = b1.getParent();
			if (s0.collidedWith(s1) | s1.collidedWith(s0)){
				//This deliberately does NOT short circuit. It adds the other object of pair to both objects
				//Only deal with new contacts if they haven't been dealt with in this time step
				return false;
			}
			//System.out.println("\nContact added: " + cp.positionWorldOnA + " " + cp.positionWorldOnB);
			if (b0.getParent().getType().equals("Wall") && !b1.getParent().getType().equals("Wall")){
				b1.getParent().collided(b0.getParent(), cp, 0);
			}
			else if (b1.getParent().getType().equals("Wall") && !b0.getParent().getType().equals("Wall")){
				b0.getParent().collided(b1.getParent(), cp, 0);
			}
			else{
				b0.getParent().collided(b1.getParent(), cp, 1);
			}
			
			return true;
		}
	}

}
