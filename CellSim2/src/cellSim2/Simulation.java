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

import com.bulletphysics.util.ObjectArrayList;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.collision.narrowphase.ManifoldPoint;
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
import java.util.Date;
import java.util.Random;

import javax.vecmath.Vector3f;

import static com.bulletphysics.demos.opengl.IGL.*;

public class Simulation extends DemoApplication{
	
	private CollisionDispatcher dispatcher;
	private DefaultCollisionConfiguration collisionConfiguration;
	private BroadphaseInterface broadphase;
	private ConstraintSolver solver;
	
	private ObjectArrayList<SimObject> modelObjects;
	//private ObjectArrayList<Gradient> gradients;

	private float baseCameraDistance = 150;
	private boolean needGImpact = false;

	private ImageGenerator imageGen;
	private boolean render = true, finished = false;
	private SimGenerator simValues;
	private long currentTime, startTime, clockTime, lastDataOutput;
	
	private Random random;
	
	private BufferedWriter logFile, cellData, wallData, gradientTestFile;
	private ObjectArrayList<BufferedWriter> gradientDataFiles;
	
	int testWidth;
	
	public Simulation(IGL gl, SimGenerator s) {
		super(gl);
		simValues = s;
		modelObjects = new ObjectArrayList<SimObject>();
		//startTime is the underlying clock time. clockTime - startTime = currentTime
		startTime = clock.getTimeMicroseconds();
		clockTime = startTime;
		//current time is microseconds since start of simulation
		currentTime = clockTime - startTime;
		lastDataOutput = -1;
		
		//TODO ImageGenerator is just a stub right now
		imageGen = new ImageGenerator();
		
		random = new Random();
		gradientDataFiles = new ObjectArrayList<BufferedWriter>();
		testWidth = 1200;
		
		//Set up the output files
		try{
			logFile = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "logFile.csv")));
			cellData = new BufferedWriter(new FileWriter(new File(simValues.getOutputDir(), "cellData.csv")));
			cellData.write(Cell.getDataHeaders());
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
		}
		catch (IOException e){
			System.err.println("Cannot generate output files!");
		}
		writeToLog(getFormattedTime() + "\tIntialization Complete");
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
		
		simValues.createWalls(this);

		if (needGImpact){
			GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
		}
		
		clientResetScene();

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
		float secSinceOutput = (float)((currentTime - lastDataOutput)/(1.0e6));
		if (lastDataOutput < 0 || secSinceOutput >= simValues.secBetweenOutput){
			outputData();
			lastDataOutput = currentTime;
		}
		int numObjects = modelObjects.size();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			bioObj.updateObject();
		}

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// simple dynamics world doesn't handle fixed-time-stepping
		long oldTime = clockTime;
		clockTime = clock.getTimeMicroseconds();
		long deltaTime = clockTime - oldTime;
		currentTime = clockTime - startTime;

		// step the simulation
		if (dynamicsWorld != null) {
			dynamicsWorld.stepSimulation(deltaTime / 1000000f);
			// optional but useful: debug drawing
			dynamicsWorld.debugDrawWorld();
		}

		if (simValues.displayImages){
			renderme();
			//glFlush();
			//glutSwapBuffers();
		}
		
		if (currentTime >= simValues.endTime*1e6){
			finished = true;
		}
	}
	
	@Override
	public void myinit(){
		super.myinit();
		setCameraDistance(baseCameraDistance);
		ele = 0f;
		updateCamera();
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
			}*/
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
	
	private void outputData(){
		int numObjects = modelObjects.size();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			bioObj.writeOutput();
		}
		for (int i = 0; i < simValues.gradients.size(); i++){
			Gradient g = simValues.gradients.get(i);
			g.writeOutput(this);
			//Debugging
			String s = getFormattedTime() + "\t" + getProteinName(g.getProtein());
			for (int j = 0; j < testWidth; j+=100){
				s = s + "\t" + g.getConcentration(currentTime, new Vector3f(j, 0, 0));
			}
			s += "\n";
			try{
				gradientTestFile.write(s);
			}
			catch(IOException e){
				System.err.println("Could not write to gradient test file");
			}
		}
		
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
	
	public long getCurrentTimeMicroseconds(){
		return currentTime;
	}
	
	public BufferedWriter getWallFile(){
		return wallData;
	}
	
	public String getFormattedTime(){
		long millisec = currentTime / 1000;
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
				logFile.write(getFormattedTime() + ", " + s);
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
		writeToLog("current time (microseconds)" + currentTime);
		writeToLog("\n" + getFormattedTime() + "\tFinishing Up");
		
		try{
			logFile.flush();
			logFile.close();
			cellData.flush();
			cellData.close();
			wallData.flush();
			wallData.close();
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

}
