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
import javax.vecmath.Vector3f;

public interface Gradient {
	//TODO Do Gradients need ids? I have to think about that.
	//For now, no
	public static final int X_AXIS = 0, Y_AXIS = 1, Z_AXIS = 2;
	public float getConcentration(long time, Vector3f position);
	public int getProtein();
	public float[] getColor(float con);
	public int getAxis();
	public float getMaxConcentration();
	public float getMinConcentration();
	public void setAxis(int a);
	public void setBaseColor(float[] c);
	public void setMaxConcentration(float c);
	public void setMinConcentration(float c);
	public void print(PrintStream p);
	public String getDataHeaders();
	public void setOutputFile(BufferedWriter bw);
	public void writeOutput(Simulation sim);
}
