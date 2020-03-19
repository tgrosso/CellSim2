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
	private int[] molsPerBond;
	//private HashMap<Integer, TraffickingInfo> traffickingRates;
	private int visibleReceptor = -1;
	private float lastConcentration;
	private Vector3f worldCenter;
	private int mins;
	private long overall;
	
	public SurfaceSegment(SimObject p, int id) {
		parent = p;
		myId = id;
		receptorIds = new int[0];
		unboundReceptors = new float[0];
		boundReceptors = new float[0];
		maxReceptors = new float[0];
		molsPerBond = new int[0];
		//traffickingRates = new HashMap<Integer, TraffickingInfo>();
		lastConcentration = 0;
		worldCenter = new Vector3f();
		mins = 0;
		overall = 0;
	}
	
	public void addReceptor(int proId, float unbound, int mpb){
		//System.out.println("SS 52: adding receptor");
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
		int[] newMPB = Arrays.copyOf(molsPerBond, receptorIds.length+1);
		newids[receptorIds.length] = proId;
		newUnbound[receptorIds.length] = unbound;
		newBound[receptorIds.length] = 0;
		newMax[receptorIds.length] = unbound;
		newMPB[receptorIds.length] = mpb;
		receptorIds = newids;
		unboundReceptors = newUnbound;
		boundReceptors = newBound;
		maxReceptors = newMax;
		molsPerBond = newMPB;
		if (receptorIds.length == 1){
			visibleReceptor = 0;
		}
		//if (myId == 0){
		//	System.out.println("SS 76: Initial unbound = " + unbound);
		//}
		//System.out.println("SS 75 Visible receptor: " + visibleReceptor);
		//System.out.println("unbound receptor: " + unboundReceptors[visibleReceptor]);
		//System.out.println("bound receptor: " + boundReceptors[visibleReceptor]);
		//System.out.println("receptor id: " + receptorIds[visibleReceptor]);
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
	
	public SimObject getParent(){
		return parent;
	}
	
	//public void removeTraffickingInfo(int recId){
	//	if (traffickingRates.containsKey(recId)){
	//		traffickingRates.remove(recId);
	//	}
	//}
	
	public float getNumMolecules(int proID, float portion, boolean bound){
		for (int i = 0; i < receptorIds.length; i++){
			if (proID == receptorIds[i]){
				if (bound){
					return boundReceptors[i] * portion;
				}
				else{
					return unboundReceptors[i] * portion;
				}
			}
		}
		//protein not found!
		return -1;
	}
	
	public float getProteinPortion(int proID, boolean bound){
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
	
	public int getMoleculesPerBond(int proID){
		for (int i = 0; i < receptorIds.length; i++){
			if (proID == receptorIds[i]){
				return molsPerBond[i];
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
	
	public void update(long now, float deltaMicroSecs, Vector3f wC){
		//Update all of the proteins on this segment
		//System.out.println("SS 152: Updating proteins");
		TraffickingInfo tf = new TraffickingInfo();
		worldCenter = wC;
		
		for (int i = 0; i < receptorIds.length; i++){
			Integer key = new Integer(receptorIds[i]);
			Protein pro = parent.getProtein(receptorIds[i]);
			//System.out.println("SS 162 protein: " + pro.getName());
			tf = parent.getTraffickInfo(receptorIds[i], myId);
			//System.out.println("SS 163 Getting parent tf");
			//System.out.println("SS 178 receptorId " + i + " tf: " + tf.toString());
			if (tf.isBlank()){
				//Reduce using half life
				//System.out.println("SS 172: tf is Blank");
				float minutes = deltaMicroSecs / 1000000f / 60;
				unboundReceptors[i] = unboundReceptors[i]*(float)Math.pow(.5, minutes/pro.getHalfLife());
				return;
			}
			/*
			if (myId == 0){
				System.out.println("SS 179: Segment Traffic rates");
				System.out.println("secretion: " + tf.getSecretionRate());
				System.out.println("unbound int: " + tf.getUnboundIntRate());
				System.out.println("bound int: " + tf.getBoundIntRate());
			}*/
			//Are there a relevant interactions for this surface protein?
			//Get all interactions with this protein as the target
			
			//Use the trafficking info to update the number of proteins
			//See if there is a gradient of any ligands
			int[] ligands = pro.getLigands();
			//System.out.println("SS 179: ligands length: "+ ligands.length);
			for (int k = 0; k < ligands.length; k++){
				Gradient g = parent.getGradient(ligands[k]);
				float ligandConc = 0;
				if (g!= null){
					//TODO Only works if a single ligand!
					ligandConc = g.getConcentration(now, worldCenter);	
					lastConcentration = ligandConc;
				} 
				float kon = pro.getBindingRate(k);
				float koff = pro.getReverseRate(k);
				float qr = tf.getSecretionRate();
				float kt = tf.getUnboundIntRate();
				float ke = tf.getBoundIntRate();
				/*
				if (myId == 0){
					System.out.println("SS 212: ");
					System.out.println("   onrate: " + kon);
					System.out.println("    offrate: " + koff);
					System.out.println("delta Micro: " + deltaMicroSecs);
					System.out.println("qr: " + qr);
					System.out.println("kt: " + kt);
					System.out.println("ke: " + ke);
				}*/
				
				
				float deltaR = deltaMicroSecs * ((-kon*unboundReceptors[i]*ligandConc)+(koff * boundReceptors[i])-(kt*unboundReceptors[i])+qr);
				float deltaC = deltaMicroSecs * ((kon*unboundReceptors[i]*ligandConc) - (koff * boundReceptors[i]) - (ke * boundReceptors[i]));
				unboundReceptors[i] += deltaR;
				boundReceptors[i] += deltaC;
				//System.out.println("SS 196: unbound: " + unboundReceptors[i] + " boundReceptors: " + boundReceptors[i] + " deltaR: " + deltaR + " deltaC: " + deltaC);
				if (unboundReceptors[i] < 0){
					unboundReceptors[i] = 0;
				}
				if (boundReceptors[i] < 0){
					boundReceptors[i] = 0;
				}
				overall += deltaMicroSecs;
				int newMinutes = (int)(overall/6e7);
				if (newMinutes > mins && parent.getID()==0){
					mins = newMinutes;
					//System.out.println("SS 241 - " + mins + ": R" + unboundReceptors[i] + " C " + boundReceptors[i]);
				}
			}

		}
	}
	
	public void makeBond(int proId, float numMolecules){
		//Remove molecules from unbound and add to bound
		for (int i = 0; i < receptorIds.length; i++){
			if (receptorIds[i] == proId){
				unboundReceptors[i] -= numMolecules;
				boundReceptors[i] += numMolecules;
				return;
			}
		}
	}
	
	public void removeBond(int proId, float numMolecules){
		//Remove molecules from bound and add to unbound
		for (int i = 0; i < receptorIds.length; i++){
			if (receptorIds[i] == proId){
				unboundReceptors[i] += numMolecules;
				boundReceptors[i] -= numMolecules;
				return;
			}
		}
	}
	
	public int getID(){
		return myId;
	}
	public int[] getProteins(){
		return receptorIds;
	}
	
	public String getOutput(Simulation sim){
		String s = "";
		for (int i = 0; i < receptorIds.length; i++){
			//System.out.println("SS 280 Bound receptors: " + boundReceptors[i]);
			if (boundReceptors[i] > 0 || unboundReceptors[i] > 0){
				s += sim.getFormattedTime() + "\t" + getParent().getType() + "-" + getParent().getID() + "\t" + myId + "\t";
				s += sim.getProteinName(receptorIds[i]) + "\t" + unboundReceptors[i] + "\t" + boundReceptors[i];
				s += "\t" + lastConcentration + "\t" + worldCenter.x;
				s += "\n";
			}
		}
		//System.out.println("SS 236: Surface Segment " + myId + ": " + s);
		//System.out.println("  " + "Num receptors: " + receptorIds.length);
		return s;
	}
	
	public String getFinalOutput(){
		return "\tSufaceSegment: " + myId + "\t";
	}

}
