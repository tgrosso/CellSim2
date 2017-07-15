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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
import java.util.Arrays;
//import java.util.HashMap;

import com.bulletphysics.util.ObjectArrayList;

public class Protein {
	private String name;
	private boolean bindsToSurface;
	private boolean membraneBound;
	private boolean diffusible;
	private int myId;
	private float halflife, decayRate;
	private String[][] ligandInfo;
	private int[] ligands;
	private float[] bindingRates;
	private float[] reverseRates;
	private float[] bondLengths;
	private float[] bondLifetimes;

	/**
	 * A class to represent a protein in simulation
	 */
	public Protein(String n, String filename, int id) {
		//Set variables to default values
		name = n;
		myId = id;
		bindsToSurface = false;
		membraneBound = false;
		diffusible = false;
		halflife = 0f;
		try{
			bindsToSurface = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "bindsToSurface"));
			membraneBound = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "membraneBound"));
			diffusible = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "diffusible"));
			halflife = Float.parseFloat(Defaults.getSpecialDefault("Protein", "halflife"));
		}
		catch(NumberFormatException e){
			System.err.println("Could not get default values for protein " + name);
		}
		ligands = new int[0];
		bindingRates = new float[0];
		reverseRates = new float[0];
		bondLengths = new float[0];
		bondLifetimes = new float[0];
		ligandInfo = new String[0][];
		//Read in values from given input file
		try{
			readInputFile(filename);
		}
		catch(IOException e){
			System.err.println("Could not read input file for protein " + name);
		}
		decayRate = halflife / 0.693147180559945f;//TODO WTF?
	}
	
	public void readInputFile(String filename) throws IOException{
		File inFile = new File(filename);
		FileInputStream fis = new FileInputStream(inFile);
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader br = new BufferedReader(isr);
		String line;
		
		while ((line = br.readLine()) != null) {
			if (line.length() > 0 && !line.contains("\\\\")){
				String[] valueVar = line.split("\t");
				if (valueVar.length < 2){
					System.err.println("Badly formatted input. Variable<tab>value.");
					System.err.println("Input was: " + line);
					continue;
				}
				String var = valueVar[0];
				String value = valueVar[1];
				if (value == null || var == null){
					System.err.println("Badly formatted input. Variable <tab>value.");
					System.err.println("Input was: " + line);
					continue;
				}
				if (var.compareTo("ligand")==0){
					if (valueVar.length < 6){
						System.err.println("Ligand info should have 6 values. Found " + valueVar.length);
						System.err.println("Usage: ligand<tab>ProteinName<tab>ForwardBindingRate<tab>ReverseBindingRate<tab>BondLength<tab>BondLifetime");
						continue;
					}
					//Add the information string to ligandInfo
					String[][] ligInf = new String[ligandInfo.length+1][];
					for (int i = 0; i < ligandInfo.length; i++){
						ligInf[i] = ligandInfo[i];
					}
					ligInf[ligandInfo.length] = valueVar;
					ligandInfo = ligInf;
					continue;
				}
				if (!Defaults.variableExists("Protein", var)){
					System.err.println("Varible " + var + " does not exist for proteins.");
					continue;
				}
				try{
					Field f = Protein.class.getDeclaredField(var);
					String type = f.getType().getName();
					switch(type){
					case "boolean":
	        			f.setBoolean(this, Boolean.parseBoolean(value));
	        			//System.out.println(f.getName() + " " + f.getBoolean(this));
	        			break;
					case "float":
						f.setFloat(this, Float.parseFloat(value));
						break;
					}
						
				}catch(NoSuchFieldException e){
					System.err.println("Protein " + name + " variable " + var + " does not exist");
				}
				catch(IllegalAccessException f){
					System.err.println("Protein " + name + " variable " + var + " access not granted");;
				}
			}
		}
		br.close();
		isr.close();
		fis.close();
	}

	public String getName(){
		return name;
	}
	
	public boolean canBindToSurface(){
		return bindsToSurface;
	}
	
	public boolean canBindToMembrane(){
		return membraneBound;
	}
	
	public boolean canDiffuse(){
		return diffusible;
	}
	
	public float getDecayRate(){
		return decayRate;
	}
	
	public float getHalfLife(){
		return halflife;
	}
	
	public int getId(){
		return myId;
	}
	
	public void setLigands(SimGenerator sim){
		//Takes the ligand info that was read in, checks that the other proteins exist
		//and imports the data
		for (int i = 0; i < ligandInfo.length; i++){
			//get the name of the protein
			String [] info = ligandInfo[i];
			String proName = info[1];
			int proId = sim.getProteinId(proName);
			//see if protein exists in sim generator
			if (proId < 0){
				System.err.println("Ligand Protein for " + name + ": " + proName + " does not exist in simulation");
			}
			else{
				//insert the information
				try{
					float br = Float.parseFloat(info[2]);
					float rr = Float.parseFloat(info[3]);
					float bLen = Float.parseFloat(info[4]);
					float bLife = Float.parseFloat(info[5]);
					int[] ligs = Arrays.copyOf(ligands, ligands.length+1);
					float[] bRate = Arrays.copyOf(bindingRates, ligands.length+1);
					float[] rRate = Arrays.copyOf(reverseRates, ligands.length+1);
					float[] bLength = Arrays.copyOf(bondLengths, ligands.length+1);
					float[] bLifetime = Arrays.copyOf(bondLifetimes, ligands.length+1);
					ligs[ligands.length] = proId;
					bRate[ligands.length] = br;
					rRate[ligands.length] = rr;
					bLength[ligands.length] = bLen;
					bLifetime[ligands.length] = bLife;
					ligands = ligs;
					bindingRates = bRate;
					reverseRates = rRate;
					bondLengths = bLength;
					bondLifetimes = bLifetime;
				}catch(NumberFormatException e){
					System.err.println("Error adding ligand: " + proName + ". Number format exception");
					System.err.println(e.toString());
				}
			}
		}
		System.out.println("Ligands for " + name);
		for (int i = 0; i < ligands.length; i++){
			System.out.println("ID: " + ligands[i] + " Name: " + sim.getProteinName(ligands[i]) + " forward rate: "+ bindingRates[i] + " reverse rate: " + reverseRates[i] + " bondLength: " + bondLengths[i]+ " bondLifetimes: " + bondLifetimes[i]);
		}
	}
	
	public void print(PrintStream p){
		p.println("Protein: " + getName());
		p.println("\tID: " + getId());
		p.println("\tBinds to surfaces: " + canBindToSurface());
		p.println("\tBinds to membranes: " + canBindToMembrane());
		p.println("\tDiffusible: " + canDiffuse());
		p.println("\tHalflife: " + this.halflife);
		p.println("\tDecayRate: " + getDecayRate());
	}
}
