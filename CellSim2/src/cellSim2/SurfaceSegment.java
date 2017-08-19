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

import java.util.Arrays;

import javax.vecmath.Vector3f;

/**
 * @author Terri Applewhite-Grosso
 *
 */

import java.util.HashMap;

public class SurfaceSegment {

	private SimObject parent;
	private int myId;
	private int[] receptorIds;
	private long[] unboundReceptors;
	private long[] boundReceptors;
	private HashMap<Integer, TraffickingInfo> traffickingRates;
	private HashMap<Integer, Ligand[]> ligands;
	
	public SurfaceSegment(SimObject p, int id) {
		parent = p;
		myId = id;
		receptorIds = new int[0];
		unboundReceptors = new long[0];
		boundReceptors = new long[0];
		traffickingRates = new HashMap<Integer, TraffickingInfo>();
		ligands = new HashMap<Integer, Ligand[]>();
	}
	
	public void addReceptor(int proId, long unbound){
		int[] newids = Arrays.copyOf(receptorIds, receptorIds.length+1);
		long[] newUnbound = Arrays.copyOf(unboundReceptors, receptorIds.length+1);
		long[] newBound = Arrays.copyOf(boundReceptors, receptorIds.length+1);
		newids[receptorIds.length] = proId;
		newUnbound[receptorIds.length] = unbound;
		newBound[receptorIds.length] = 0L;
		receptorIds = newids;
		unboundReceptors = newUnbound;
		boundReceptors = newBound;
	}
	
	public void addLigand(int recId, Ligand l){
		//Integer id = new Integer(recId);
		if (ligands.containsKey(recId)){
			Ligand[] li = ligands.get(recId);
			Ligand[] newLi = Arrays.copyOf(li,  li.length+1);
			newLi[li.length] = l;
			ligands.put(recId, newLi);
		}
		else{
			Ligand[] li = new Ligand[1];
			li[0] = l;
			ligands.put(recId, li);
		}
	}
	
	public void addTraffickingInfo(int recId, TraffickingInfo ti){
		//Will simply replace the one that is in the map if one exists
		traffickingRates.put(recId, ti);
	}
	
	public void removeTraffickingInfo(int recId){
		if (traffickingRates.containsKey(recId)){
			traffickingRates.remove(recId);
		}
	}
	

	public void update(Simulation sim, Vector3f position){
		//for each protein
		for (int i = 0; i < receptorIds.length; i++){
			int pro = receptorIds[i];
			TraffickingInfo ti;
			//See if there are local trafficking rates
			if (traffickingRates.containsKey(pro)){
				//if so use those rates for this protein
				ti = traffickingRates.get(pro);
				//Then check to see if all values are within .1% of parent value
				if (ti.withinRange(parent.getTraffickInfo(pro, myId))){
					traffickingRates.remove(pro);
				}
			}
			else{
				//if not, get the traffickingInfo from the parent
				ti = parent.getTraffickInfo(pro, myId);
			}
			//Find out the ligands that bind to this protein.
			if (ligands.containsKey(pro)){
				Ligand[] lig = ligands.get(pro);
				for (int j = 0; j < lig.length; j++){
					Ligand li = lig[j];
					//if the ligand forms structural bonds, skip it. They only happen with collisions
					if (li.formsStructuralBonds()){
						continue;
					}
					//Find the concentration of this ligand at this position
					long time = sim.getCurrentTimeMicroseconds();
					float conc = li.getConcentration(time, position);
					//Calculate the new number of bound and unbound molecules
					float deltaTime = sim.getDeltaTimeMicroseconds();
					float unbound = (float)unboundReceptors[i];
					float bound = (float)boundReceptors[i];
					float deltaUnbound = (((-1)* li.getForwardRate() *unbound * conc) + (li.getReverseRate() * bound) -(ti.getUnboundIntRate()*unbound) + ti.getSecretionRate()) * sim.getDeltaTimeMicroseconds();
					float deltaBound = ((li.getForwardRate() * unbound * conc)-(li.getReverseRate()*bound)-(ti.getBoundIntRate()*bound)) * sim.getDeltaTimeMicroseconds();
					unboundReceptors[i] = (long)(unboundReceptors[i] + deltaUnbound);
					boundReceptors[i] = (long)(boundReceptors[i] + deltaBound);
				}
			}
		}
	}
	
	public void makeBond(long numMolecules){
		
	}

}
