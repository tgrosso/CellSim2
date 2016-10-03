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

import javax.vecmath.Vector3f;

import static com.bulletphysics.demos.opengl.IGL.*;

public class Simulation extends DemoApplication{
	
	private CollisionDispatcher dispatcher;
	private DefaultCollisionConfiguration collisionConfiguration;
	private BroadphaseInterface broadphase;
	private ConstraintSolver solver;
	
	private ObjectArrayList<SimObject> modelObjects;

	private float baseCameraDistance = 150;
	private boolean needGImpact = false;

	
	private boolean render = true;
	private SimGenerator simValues;
	private long currentTime;
	
	public Simulation(IGL gl, SimGenerator s) {
		super(gl);
		simValues = s;
		modelObjects = new ObjectArrayList<SimObject>();
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
		int numObjects = modelObjects.size();
		for (int i = 0; i < numObjects; i++){
			SimObject bioObj = modelObjects.getQuick(i);
			bioObj.updateObject();
			//writeToLog("Object updated: " + bioObj.getType() + " " + clock.getTimeMicroseconds());
		}

		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// simple dynamics world doesn't handle fixed-time-stepping
		float ms = getDeltaTimeMicroseconds();

		// step the simulation
		if (dynamicsWorld != null) {
			dynamicsWorld.stepSimulation(ms / 1000000f);
			// optional but useful: debug drawing
			dynamicsWorld.debugDrawWorld();
		}

		
		renderme();
		//glFlush();
		//glutSwapBuffers();
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
		//This method adds a wall to the container
		modelObjects.add(obj);
		dynamicsWorld.addRigidBody(obj.getRigidBody());
	}
	
	public void removeSimulationObject(SimObject obj){
		//Ugh!  How do I do this?
		dynamicsWorld.removeRigidBody(obj.getRigidBody());
		modelObjects.remove(obj);
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

}
