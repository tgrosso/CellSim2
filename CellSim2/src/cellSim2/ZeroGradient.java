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

import java.io.BufferedWriter;
import java.io.PrintStream;
import java.io.IOException;

import javax.vecmath.Vector3f;

public class ZeroGradient implements Gradient{

	protected float concentration;
	protected int proteinId;
	protected float[] baseColor = {0.0f, 1.0f, 0.0f, 1.0f};
	protected int axis;
	protected BufferedWriter outputFile;
	
	/**
	 * Represents a solution of ligand with a constant concentration throughout
	 * Concentration is given in microMolar units
	 */
	//If no concentration given, default value is 0
	public ZeroGradient(int protein, float con) {
		concentration = con;
		proteinId = protein;
		axis = Gradient.X_AXIS;
	}
	
	public ZeroGradient(int protein) {
		this(protein, 0.0f);
	}

	@Override
	public float getConcentration(long time, Vector3f position){
		return concentration;
	}
	
	@Override
	public int getProtein(){
		return proteinId;
	}
	
	@Override
	public float getMaxConcentration(){
		return concentration;
	}
	
	@Override
	public float getMinConcentration(){
		return concentration;
	}
	
	@Override
	public void setBaseColor(float r, float g, float b){
		baseColor[0] = r;
		baseColor[1] = g;
		baseColor[2] = b;
	}
	
	@Override
	public float[] getColor(float con){
		return baseColor;
	}
	
	@Override
	public int getAxis(){
		return axis;
	}
	
	@Override
	public void setAxis(int a){
		axis = a;
	}
	
	@Override
	public void setMaxConcentration(float c){
		concentration = c;
	}
	
	@Override
	public void setMinConcentration(float c){
		concentration = c;
	}
	
	@Override
	public void print(PrintStream p){
		p.println("ZeroGradient");
		p.println("\tProtein ID: "+ getProtein());
		p.println("\tConcentration: " + concentration);
	}
	
	@Override
	public String getDataHeaders(){
		String s = "Time Since Sim Start\tProtein\tConcentration\n";
		return s;
	}
	
	public void setOutputFile(BufferedWriter bw){
		outputFile = bw;
	}
	
	public void writeOutput(Simulation sim){
		String s = sim.getFormattedTime() + "\t" + sim.getProteinName(proteinId) + "\t" +concentration + "\n";
		try{
			outputFile.write(s);
		}
		catch(IOException e){
			sim.writeToLog(sim.getFormattedTime() + "\tCould not write concentrations from Zero Gradient " + sim.getProteinName(proteinId)+"\t" + e.toString()+"\n");
		}
	}
}
