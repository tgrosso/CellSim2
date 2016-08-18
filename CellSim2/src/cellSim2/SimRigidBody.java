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

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.MotionState;

/**
 * @author Terri Applewhite-Grosso
 *
 */
public class SimRigidBody extends RigidBody {

	private SimObject parent;
	/**
	 * @param constructionInfo
	 * @param parent
	 */
	public SimRigidBody(RigidBodyConstructionInfo constructionInfo, SimObject p) {
		super(constructionInfo);
		parent = p;
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param mass
	 * @param motionState
	 * @param collisionShape
	 * @param parent
	 */
	public SimRigidBody(float mass, MotionState motionState, CollisionShape collisionShape, SimObject p) {
		super(mass, motionState, collisionShape);
		parent = p;
	}

	/**
	 * @param mass
	 * @param motionState
	 * @param collisionShape
	 * @param localInertia
	 * @param parent
	 */
	public SimRigidBody(float mass, MotionState motionState, CollisionShape collisionShape, Vector3f localInertia, SimObject p) {
		super(mass, motionState, collisionShape, localInertia);
		parent = p;
	}
	
	public SimObject getParent(){
		return parent;
	}

}
