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
import java.util.ArrayList;

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
	private float[] unboundReceptors;
	private float[] boundReceptors;
	private float[] maxReceptors;
	private HashMap<Integer, TraffickingInfo> traffickingRates;
	private int visibleReceptor = -1;
	
	public SurfaceSegment(SimObject p, int id) {
		parent = p;
		myId = id;
		receptorIds = new int[0];
		unboundReceptors = new float[0];
		boundReceptors = new float[0];
		maxReceptors = new float[0];
		traffickingRates = new HashMap<Integer, TraffickingInfo>();
	}
	
	public void addReceptor(int proId, float unbound){
		int index = getProteinIndex(proId);
		if (index >= 0){
			//This protein is already on this segment. Just add on
			unboundReceptors[index] += unbound;
			maxReceptors[index] += unbound;
			return;
		}
		int[] newids = Arrays.copyOf(receptorIds, receptorIds.length+1);
		float[] newUnbound = Arrays.copyOf(unboundReceptors, receptorIds.length+1);
		float[] newBound = Arrays.copyOf(boundReceptors, receptorIds.length+1);
		float[] newMax = Arrays.copyOf(maxReceptors, receptorIds.length+1);
		newids[receptorIds.length] = proId;
		newUnbound[receptorIds.length] = unbound;
		newBound[receptorIds.length] = 0L;
		newMax[receptorIds.length] = unbound;
		receptorIds = newids;
		unboundReceptors = newUnbound;
		boundReceptors = newBound;
		maxReceptors = newMax;
		if (receptorIds.length == 1){
			visibleReceptor = 0;
		}
	}
	
	public void addTraffickingInfo(int recId, TraffickingInfo ti){
		//Will simply replace the one that is in the map if one exists
		traffickingRates.put(recId, ti);
	}
	
	public boolean setVisibleProtein(int id){
		boolean proSet = false;
		for (int i = 0; i < receptorIds.length; i++){
			if (receptorIds[i] == id){
				visibleReceptor = id;
				proSet = true;
			}
		}
		if (!proSet){
			//Don't display if the visible protein is not a receptor
			visibleReceptor = -1;
		}
		return proSet;
	}
	
	public void setNewParent(SimObject o){
		parent = o;
	}
	
	public void removeTraffickingInfo(int recId){
		if (traffickingRates.containsKey(recId)){
			traffickingRates.remove(recId);
		}
	}
	
	public float getProteinPercentage(int proID, boolean bound){
		for (int i = 0; i < receptorIds.length; i++){
			if (proID == receptorIds[i]){
				if (bound){
					return boundReceptors[i] / maxReceptors[i];
				}
				else{
					return unboundReceptors[i] / maxReceptors[i];
				}
			}
		}
		//protein not found!
		return -1;
	}
	
	public int getProteinIndex(int proID){
		for (int i = 0; i < receptorIds.length; i++){
			if (receptorIds[i] == proID){
				return i;
			}
		}
		return -1;
	}
	
	public void update(float min){
		//Update all of the proteins on this segment
		TraffickingInfo tf = new TraffickingInfo();
		for (int i = 0; i < receptorIds.length; i++){
			Integer key = new Integer(receptorIds[i]);
			Protein pro = parent.getProtein(receptorIds[i]);
			if (traffickingRates.containsKey(key)){
				tf = traffickingRates.get(key);
			}
			else{
				tf = parent.getTraffickInfo(receptorIds[i], myId);
			}
			if (tf.isBlank()){
				//Reduce using half life
				unboundReceptors[i] = unboundReceptors[i]*(float)Math.pow(.5, min/pro.getHalfLife());
			}
		}
	}
	
	public void makeBond(long numMolecules){
		
	}
	
	public int getID(){
		return myId;
	}
	public int[] getProteins(){
		return receptorIds;
	}
	
	public String getOutput(){
		return "";
	}
	
	public String getFinalOutput(){
		return "";
	}

}
