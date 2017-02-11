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


import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.linearmath.Transform;
import javax.vecmath.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * @author Terri Applewhite-Grosso
 *
 */
public class GradientWall extends Wall {
	protected Gradient grad;
	private int drawnSegments;
	private float[] glMat = new float[16];
	private Vector3f startPoint;
	protected float[] wallColor = {1.0f, 0.2f, 0.2f, 1f};
	protected float distanceFromSource = 0f;
	
	
	public GradientWall(Simulation s, float w, float h, float d, Vector3f o, Gradient g) {
		super(s, w, h, d, o);
		grad = g;
		drawnSegments = 200;
		if (w < drawnSegments){
			drawnSegments = (int)w;
		}
		System.out.println("Size: " + size + " origin: " + origin);
		//startPoint = new Vector3f(-(float)(size.x/2.0)+o.x, (float)(size.y/2.0)+o.y, (float)(size.z/2.0)+o.z);
		startPoint = new Vector3f(-(float)(size.x/2.0)+o.x, (float)(size.y/2.0)+o.y, (float)(size.z/2.0));
		//System.out.println(this.id);
	}

	public GradientWall(Simulation s, float w, float h, float d, Vector3f o) {
		this(s, w, h, d, o, null);
	}
	
	public void setGradient(Gradient g){
		//TODO What needs to be initialized here?
		grad = g;
	}
	
	public void setDistFromSource(float dist){
		distanceFromSource = dist;
	}

	public boolean specialRender(IGL gl, Transform t){
		if (grad == null){
			return false;
		}
		
		//get the time 
		long ti = sim.getCurrentTimeMicroseconds();
		int axis = grad.getAxis();
		
		float[] wallSize = new float[3];
		getSize().get(wallSize);
		float blockSize = wallSize[axis]/(float)drawnSegments;
		
		float[] diagonal = new float[3];
		switch(axis){
			case 0:
				diagonal[0] = -blockSize;
				diagonal[1] = -wallSize[1];//height
				diagonal[2] = 0;
				break;
			case 1:
				diagonal[0] = wallSize[0];//width
				diagonal[1] = blockSize;
				diagonal[2] = 0;
				break;
			case 2:
				diagonal[0] = 0;
				diagonal[1] = wallSize[1];
				diagonal[2] = blockSize;
				break;
		}
		Vector3f diagonalVector = new Vector3f(diagonal);
		//System.out.println(diagonalVector);

		float[] nextPos = new float[]{0f, 0f, 0f};
		nextPos[axis] = blockSize;
		Vector3f nextPositionVector = new Vector3f(nextPos);
		//System.out.println(nextPositionVector);
		
		float[] dist = new float[]{0f, 0f, 0f};
		dist[axis] = distanceFromSource;
		Vector3f distVector = new Vector3f(dist);
		
		gl.glPushMatrix();
		
		t.getOpenGLMatrix(glMat);
		gl.glMultMatrix(glMat);
		GL11.glNormal3f( 0f, 0f, -1f); 
		Vector3f vecOne = new Vector3f(startPoint);
		Vector3f vecTwo = new Vector3f(vecOne);
		vecTwo.add(diagonalVector);
		for (int i = 0; i < drawnSegments; i++){
			//Find the concentration at this time and position
			//System.out.println("VecOne: " + vecOne + " VecTwo: " + vecTwo);
			Vector3f gradPos = new Vector3f();
			gradPos.add(vecOne, distVector);
			float[] color = grad.getColor(grad.getConcentration(ti, gradPos));
			GL11.glColor3f(color[0], color[1], color[2]);
			GL11.glBegin(GL11.GL_QUADS);
			//System.out.println(i + "ri: " + ri + " le: " + le + " con: " + con + " mi: " + mi);
			GL11.glVertex3f(vecOne.x, vecOne.y, vecOne.z);
			GL11.glVertex3f(vecTwo.x, vecOne.y, vecTwo.z);
			GL11.glVertex3f(vecTwo.x, vecTwo.y, vecTwo.z);
			GL11.glVertex3f(vecOne.x, vecTwo.y, vecOne.z);
			GL11.glEnd();
			vecOne.add(nextPositionVector);
			vecTwo.add(vecOne, diagonalVector);
			//System.out.println(startPoint + ", " + vecOne + ", " +addOnVector +", "+ vecTwo);
		}
		gl.glPopMatrix();
		
		return true;
		//return false;
	}
	
	@Override
	public String getType(){
		String s = "GradientWall";
		return s;
	}

}
