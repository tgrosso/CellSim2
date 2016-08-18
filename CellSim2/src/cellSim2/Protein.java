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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;

public class Protein {
	private String name;
	private boolean bindsToSurface;
	private boolean membraneBound;
	private boolean diffusible;
	private int myId;

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
		try{
			bindsToSurface = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "bindsToSurface"));
			membraneBound = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "membraneBound"));
			diffusible = Boolean.parseBoolean(Defaults.getSpecialDefault("Protein", "diffusible"));
		}
		catch(NumberFormatException e){
			System.err.println("Could not get default values for protein " + name);
		}
		//Read in values from given input file
		try{
			readInputFile(filename);
		}
		catch(IOException e){
			System.err.println("Could not read input file for protein " + name);
		}
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
	        			System.out.println(f.getName() + f.getBoolean(this));
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
	
	public int getID(){
		return myId;
	}
}
