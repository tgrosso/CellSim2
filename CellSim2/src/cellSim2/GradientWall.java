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


import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.demos.opengl.GLShapeDrawer;
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
	protected float[] wallColor = {1.0f, 0.2f, 0.2f, 1f};
	protected float distanceFromSource = 0f;
	protected CompoundShape gradWallShape;
	protected Vector3f[] segColors;
	protected long lastUpdate = 0;
	protected long secBetweenUpdates = 10 * 1000000;
	
	
	public GradientWall(Simulation s, float w, float h, float d, Vector3f o, Gradient g) {
		super(s, w, h, d, o);
		grad = g;
		drawnSegments = 100;
		if (w < drawnSegments){
			drawnSegments = (int)w;
		}
		//System.out.println("GW 50: Making gradient wall");
		//TODO Make this work for other axes. For now it's just x!
		segColors = new Vector3f[drawnSegments];
		float segWidth = w / drawnSegments;
		Vector3f halfex = new Vector3f(segWidth/2, h/2, d/2);
		BoxShape sectionShape = new BoxShape(halfex);
		gradWallShape = new CompoundShape();
		for (int i = 0; i < drawnSegments; i++){
			Vector3f p = new Vector3f(o.x-w/2+i*segWidth+segWidth/2, o.y, o.z);
			//System.out.println(i + ": " + p);
			Transform t = new Transform();
			t.setIdentity();
			t.origin.set(p);
			gradWallShape.addChildShape(t, sectionShape);
			segColors[i] = new Vector3f();
		}
		//updateColors();
	}

	public GradientWall(Simulation s, float w, float h, float d, Vector3f o) {
		this(s, w, h, d, o, null);
	}
	
	public void setGradient(Gradient g){
		//TODO What needs to be initialized here?
		grad = g;
	}
	
	public void setDistFromSource(float dist){
		this.distanceFromSource = dist;
		updateColors();
	}
	
	private void updateColors(){
		if (grad == null){
			return;
		}
		long now = sim.getCurrentTimeMicroseconds();
		float startDist = this.origin.x - this.size.x/2;
		Transform t = new Transform();
		Vector3f pos = new Vector3f();
		if (lastUpdate < 1 ||now - lastUpdate > secBetweenUpdates){
			//System.out.println("startDist: " + startDist + " dist from source: " + distanceFromSource);
			//update the Colors for each wall segment
			for (int i = 0; i < drawnSegments; i++){
				gradWallShape.getChildTransform(i, t);
				float dist = (t.origin.x - startDist) + this.distanceFromSource;
				pos.x = dist;
				pos.y = t.origin.y;
				pos.z = t.origin.z;
				//System.out.println("x pos" + t.origin.x + " dist: " + dist);
				segColors[i] = new Vector3f(grad.getColor(grad.getConcentration(now, pos)));
			}
			lastUpdate = now;
		}
	}
	
	@Override
	public boolean specialRender(IGL gl, Transform t, int mode){
		if (grad == null){
			return false;
		}
		//System.out.println("GW 112: Special render");
		updateColors();
		Transform tran = new Transform(t);
		//render each of the underlying boxes
		for (int i = 0; i < drawnSegments; i++){
			CollisionShape cs = gradWallShape.getChildShape(i);
			gradWallShape.getChildTransform(i, tran);
			GLShapeDrawer.drawOpenGL(gl, tran, cs, segColors[i], mode);
		}
		return true;
		//return false;
	}
	
	@Override
	public String getType(){
		String s = "GradientWall";
		return s;
	}
	
	@Override
	public String toString(){
		return "I am gradient wall " + getID();
	}

}
