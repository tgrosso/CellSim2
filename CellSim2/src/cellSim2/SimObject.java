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
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.linearmath.Transform;
import javax.vecmath.Vector3f;
import java.io.BufferedWriter;


public interface SimObject {
		
		public void collided(SimObject c, ManifoldPoint mp, long collID);
		public CollisionShape getCollisionShape();
		public SimRigidBody getRigidBody();
		public void updateObject();
		public Vector3f getColor3Vector();
		public void setVisible(boolean v);
		public boolean isVisible();
		public int getID();
		public float getMass();
		public String getType();
		public void destroy();
		public void markForRemoval();
		public boolean isMarked();
		public boolean specialRender(IGL gl, Transform t);
		public boolean isBound();
		public void clearBound();
		public void bind();
		public String finalOutput();
		public void wrapup();
		public void setOutputFile(BufferedWriter bw);
		public void writeOutput();
		public TraffickingInfo getTraffickInfo(int protein, int id);

}
