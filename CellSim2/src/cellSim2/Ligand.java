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

import java.util.ArrayList;
import javax.vecmath.Vector3f;

/**
 * @author Terri Applewhite-Grosso
 *
 */
public class Ligand {

	/**
	 * This class describes the interactions between a ligand and a receptor
	 * An array of these ligands is maintained for each receptor in the generator 
	 */
	
	private Protein ligand;
	private float forwardBindingRate = 0f;
	private float reverseBindingRate = 0f;
	private boolean formsStructBonds = false;
	private float bondLength = 0f;
	private float bondLife = 0f;
	private boolean hasGradient = false;
	private Gradient grad = null;
	//public ArrayList<Protein> targets;
	//Might use a different internal class to define targets
	//Does TraffickingInfo go here? 
	
	public Ligand(Protein li, float forward, float reverse) {
		ligand = li;
		forwardBindingRate = forward;
		reverseBindingRate = reverse;
	}
	
	public void addGradient(Gradient g){
		grad = g;
		hasGradient = true;
	}
	
	public float getConcentration(long time, Vector3f position){
		if (hasGradient){
			return grad.getConcentration(time, position);
		}
		return 0f;
	}
	
	public void setBondInfo(float length, float life){
		formsStructBonds = true;
		bondLength = length;
		bondLife = life;
	}
	
	public Protein getLigand(){
		return ligand;
	}
	
	public float getForwardRate(){
		return forwardBindingRate;
	}
	
	public float getReverseRate(){
		return reverseBindingRate;
	}
	
	public boolean formsStructuralBonds(){
		return formsStructBonds;
	}
	
	public float getMaxBondLife(){
		return bondLife;
	}
	
	public float getMaxBondLength(){
		return bondLength;
	}
}
