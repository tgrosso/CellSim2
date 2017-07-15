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

/**
 * @author Terri Applewhite-Grosso
 *
 */
public class ProteinPair {

	/**
	 * This class describes the interactions between a ligand and a receptor
	 * A list of these Pairs is maintained in the Generator
	 */
	
	Protein ligand;
	Protein receptor;
	float forwardBindingRate = 0f;
	float reverseBindingRate = 0f;
	boolean formsBonds = false;
	float bondLength = 0f;
	float bondLife = 0f;
	//public ArrayList<Protein> targets;
	//Might use a different internal class to define targets
	
	public ProteinPair(Protein li, Protein re, float forward, float reverse) {
		ligand = li;
		receptor = re;
		forwardBindingRate = forward;
		reverseBindingRate = reverse;
	}
	
	public void makeBond(float length, float life){
		formsBonds = true;
		bondLength = length;
		bondLife = life;
	}
	
	public Protein getLigand(){
		return ligand;
	}
	
	public Protein getReceptor(){
		return receptor;
	}
	
	public float getForwardRate(){
		return forwardBindingRate;
	}
	
	public float getReverseRate(){
		return reverseBindingRate;
	}
	
	public boolean formsStructuralBonds(){
		return formsBonds;
	}
	
	public float getMaxBondLife(){
		return bondLife;
	}
	
	public float getMaxBondLength(){
		return bondLength;
	}
}
