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
/**
 * @author Terri Applewhite-Grosso
 *
 */
package cellSim2;

import javax.vecmath.Vector3f;

public class DiffusionGradient implements Gradient{
	protected float sourceConcentration;
	protected float sinkConcentration;
	protected boolean fixedBoundaries;
	protected int proteinId;
	protected Simulation sim;
	protected float[] baseColor = {0.0f, 1.0f, 0.0f, 1.0f};
	protected float diffusionCoefficient;
	protected int axis;

	/**
	 * Represents a gradient of ligand that diffuses
	 * Note that the Source is defined as being at the zero point of the axis.
	 * The Sink is a given distance away.
	 */
	public DiffusionGradient(Simulation s, int protein, float source, float sink, float coeff) {
		sim = s;
		proteinId = protein;
		sourceConcentration = source;
		sinkConcentration = sink;
		diffusionCoefficient = coeff;
		axis = Gradient.X_AXIS;
		fixedBoundaries = true;
	}
	
	@Override
	public float getConcentration(long time, Vector3f position){
		//TODO Figure out diffusion problem!
		return 0f;
	}
	
	@Override
	public int getProtein(){
		return proteinId;
	}
	
	@Override
	public float getMaxConcentration(){
		//TODO What should I do if the MaxConcentration is zero? Fix at input time?
		//TODO Should not have negative concentrations. Source must be greater than sink
		
		//TODO For now, just dealing with fixed temperatures
		//Here's the math: http://tutorial.math.lamar.edu/Classes/DE/HeatEqnNonZero.aspx
		//Use two terms of heat equation solution
		
		return sourceConcentration;
	}
	
	@Override
	public float getMinConcentration(){
		return sinkConcentration;
	}
	
	@Override
	public void setBaseColor(float r, float g, float b){
		baseColor[0] = r;
		baseColor[1] = g;
		baseColor[2] = b;
	}
	
	@Override
	public float[] getColor(float con){
		float ratio = con/getMaxConcentration();
		float[] newColor = new float[4];
		for (int i = 0; i < 3; i++){
			newColor[i] = baseColor[i] * ratio;
		}
		newColor[3] = 1.0f;
		return newColor;
	}
	
	@Override
	public int getAxis(){
		return axis;
	}
	
	@Override
	public void setAxis(int a){
		axis = a;
	}
	
	public void fixBoundaries(boolean fixed){
		fixedBoundaries = fixed;
	}

}
