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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import javax.vecmath.Vector3f;


public class Defaults {
	
	private static final HashMap<String, String[]> defaults = new HashMap<String, String[]>();
	static{
		defaults.put("TimeZone", new String[]{"GMT"});
		defaults.put("screenWidth", new String[]{"700"});
		defaults.put("screenHeight", new String[]{"500"});
		defaults.put("endTime", new String[]{"30"});
		defaults.put("displayImages", new String[]{"true"});
		defaults.put("vessel", new String[]{"300", "90", "100"});
		defaults.put("vesselColor", new String[]{"0", "0", ".8"});
		defaults.put("distFromSource", new String[]{"0"});
		defaults.put("generateImages", new String[]{"false"});
		defaults.put("secBetweenOutput", new String[]{".5"});
		defaults.put("secBetweenImages", new String[]{"1"});
		defaults.put("speedUp", new String[]{"1"});
		defaults.put("startX", new String[]{"0"});
		//defaults.put("molsPerBond", new String[]{"100"});
	}
	
	private static final HashMap<String, String> defaultTitles = new HashMap<String, String>();
	static{
		defaultTitles.put("TimeZone", "Time Zone");
		defaultTitles.put("screenWidth", "Screen Width (pixels)");
		defaultTitles.put("screenHeight", "Screen Height (pixels)");
		defaultTitles.put("endTime", "End Time (seconds)");
		defaultTitles.put("displayImages", "Display Images (True/False)");
		defaultTitles.put("vessel", "Vessel Size (x, y, z pixels)");
		defaultTitles.put("vesselColor", "Vessel Color (r, g, b)");
		defaultTitles.put("distFromSource", "Distance from Source (microns)");
		defaultTitles.put("generateImages", "Generate Images (True/False)");
		defaultTitles.put("secBetweenOutput", "Seconds Betwen Data Output");
		defaultTitles.put("secBetweenImages", "Seconds Between Image Output");
		defaultTitles.put("speedUp", "Acceleration Value");
		defaultTitles.put("startX", "startX - What is this?");
		//defaultTitles.put("molsPerBond", "Molecules Per Bond");
		defaultTitles.put("Name", "Name or Type");
		defaultTitles.put("numCells", "Number of Cells");
		defaultTitles.put("cellDetailLevel", "Cell Detail Level (1-3)");
		defaultTitles.put("radius", "Cell Radius (microns)");
		defaultTitles.put("density", "Cell Density (g/ml)");
		defaultTitles.put("maxDeltaVel", "Maximum Cell Velocity Change");
		defaultTitles.put("bindsToSurface", "Binds to Surfaces");
		defaultTitles.put("membraneBound", "Binds to Cell Membranes");
		defaultTitles.put("diffusible", "Diffuses in Solution");
		defaultTitles.put("halflife", "Half-life");
	}
	
	private static final String[] paramOrder = new String[] {"TimeZone", "", "endTime", "secBetweenOutput", "secBetweenImages", "", "screenWidth", "screenHeight", "", 
				"molsPerBond"};
	
	private static final String[] cellOrder = new String[] {"Name", "numCells", "cellDetailLevel", "radius", "density", "maxDeltaVel"};
	private static final String[] proteinOrder = new String[] {"TimeZone"};
	
	
	private static final HashMap<String, String> cellDefaults = new HashMap<String, String>();
	static{
		cellDefaults.put("Name", "No Name");
		cellDefaults.put("numCells", "5");
		cellDefaults.put("cellDetailLevel", "1");
		cellDefaults.put("radius", "10");
		cellDefaults.put("density", "1.01");
		cellDefaults.put("maxDeltaVel", "1");
	}
	
	private static final HashMap<String, String> proteinDefaults = new HashMap<String, String>();
	static{
		proteinDefaults.put("Name", "No Name");
		proteinDefaults.put("bindsToSurface", "false");
		proteinDefaults.put("membraneBound", "Fasle");
		proteinDefaults.put("diffusible", "false");
		proteinDefaults.put("halflife", "720");
	}
	
	private HashMap<String, String[]> currentVals;
	private HashMap<String, String> proteins;
	private HashMap<String, String> cells;
	private HashMap<String, ArrayList<String[]>> gradients;
	private HashMap<String, ArrayList<String[]>> walls;
	private ArrayList<String[]> proteinPairs;
	
	public Defaults(){
		currentVals = new HashMap<String, String[]>();
		proteins = new HashMap<String, String>();
		gradients = new HashMap<String, ArrayList<String[]>>();
		walls = new HashMap<String, ArrayList<String[]>>();
		cells = new HashMap<String, String>();
		proteinPairs = new ArrayList<String[]>();
	}
	
	public void readInputFile(File input) throws IOException{
		FileInputStream fis = new FileInputStream(input);
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
				//System.out.println("Defaults: var: " + var);
				String[] value = Arrays.copyOfRange(valueVar, 1, valueVar.length);
				//System.out.println(var + ": " + value.toString());
				if (value == null || var == null){
					System.err.println("Badly formatted input. Variable <tab>value.");
					System.err.println("Input was: " + line);
					continue;
				}
				if (var.equals("protein")){
					if (value.length != 2){
						System.err.println("Badly formatted input. Protein does not have enough arguments");
						System.err.println("Correct format: protein<tab>Name<tab>SourceFile");
						continue;
					}
					proteins.put(value[0], value[1]);
					continue;
				}
				if (var.equals("cell")){
					if (value.length != 2){
						System.err.println("Badly formatted input. Cell does not have enough arguments");
						System.err.println("Correct format: cell<tab>Name<tab>SourceFile");
						continue;
					}
					cells.put(value[0], value[1]);
					continue;
				}
				if (var.equals("gradient")){
					if (value.length < 2){
						System.err.println("Badly formatted input. Gradient does not have enough arguments");
						System.err.println("Correct format: gradient<tab>ProteinName<tab>params");
						continue;
					}
					String pro = value[0];
					//System.out.println(pro);
					if (!proteins.containsKey(pro)){
						System.err.println("Protein " + pro + " for gradient not in protein list.");
						continue;
					}
					value = Arrays.copyOfRange(value, 1, value.length);
					//System.out.println("Defaults 144: Gradient value:");
					//for (int x = 0; x < value.length; x++){
					//	System.out.println("Defaults 146:   "   +value[x]);
					//}
					//see if the gradient for this protein already exists
					//if not make it with the values
					if (!gradients.containsKey(pro)){
						ArrayList<String[]> vals = new ArrayList<String[]>();
						vals.add(value);
						gradients.put(pro, vals);
					}
					else{
						gradients.get(pro).add(value);
					}
					
					continue;
				}
				//System.out.println("Defaults 161");
				if (var.equals("wall")){
					//System.out.println("Defaults 163: var is wall");
					if (value.length < 2){
						System.err.println("Badly formatted input. Wall does not have enough arguments");
						System.err.println("Correct format: wall<tab>ID number<tab>params");
						continue;
					}
					String k = value[0];
					String[] info = Arrays.copyOfRange(value, 1, value.length);
					if (!walls.containsKey(k)){
						ArrayList<String[]> vals = new ArrayList<String[]>();
						vals.add(info);
						walls.put(k, vals);
					}
					else{
						walls.get(k).add(info);
					}
					continue;
				}
				if (var.equals("proteinPair")){
					if (value.length < 4){
						System.err.println("Badly formatted input. proteinPair does not have enough arguments");
						System.err.println("Correct format: proteinPair<tab>ligand<tab>receptor<tab>forward rate<tab>reverse rate");
						System.err.println("optionally: <tab>max bond length<tab>bond lifetime");
						System.err.println("Or - define a target");
						continue;
					}
					String lig = value[0];
					String rec = value[1];
					if (!proteins.containsKey(lig)){
						System.err.println("Bad input. proteinPair ligand is not a defined protein");
						System.err.println("Ligand entered: " + lig);
						continue;
					}
					if (!proteins.containsKey(rec)){
						System.err.println("Bad input. proteinPair receptor is not a defined protein");
						System.err.println("Ligand entered: " + rec);
						continue;
					}
					proteinPairs.add(value);
					continue;
				}
				if (!Defaults.variableExists(var)){
					System.err.println("Varible " + var + " does not exist.");
					continue;
				}
				addCurrent(var, value);
			}
		}
		
		//print out proteins and gradients hashmaps
		/*
		System.out.println("This is what defaults has entered:");
		System.out.println("CELLS");
		for (String name: cells.keySet()){
            String key =name.toString();
            String value = cells.get(name).toString();  
            System.out.println(key + " " + value);  
         }
		System.out.println("PROTEINS");
		for (String name: proteins.keySet()){
            String key =name.toString();
            String value = proteins.get(name).toString();  
            System.out.println(key + " " + value);  
         }
		 System.out.println("GRADIENTS");
	     for (String key: gradients.keySet()){
            ArrayList<String[]> value = gradients.get(key);
            System.out.println(key + ":");
            for (int i = 0; i < value.size(); i++){
            	for (int j = 0; j < value.get(i).length; j++){
            		System.out.print(value.get(i)[j] + " ");
            	}
            	System.out.println("");
            }
         }
	     System.out.println("WALLS");
	     for (String key: walls.keySet()){
	            ArrayList<String[]> value = walls.get(key);
	            System.out.println(key + ":");
	            for (int i = 0; i < value.size(); i++){
	            	for (int j = 0; j < value.get(i).length; j++){
	            		System.out.print(value.get(i)[j] + " ");
	            	}
	            	System.out.println("");
	            }
	      }
	     */
		br.close();
		isr.close();
		fis.close();
		//System.out.println("Defaults has read in the file.\n");
	}
	
	public static boolean variableExists(String key){
		return defaults.containsKey(key);
	}
	
	public static boolean variableExists(String type, String key){
		if (type.equals("Protein")){
			return proteinDefaults.containsKey(key);
		}
		if (type.equals("Cell")){
			return cellDefaults.containsKey(key);
		}
		return false;
	}
	
	public void printDefaultValues(PrintStream ps){
		for (Entry<String, String[]> entry : defaults.entrySet()){
			String key = entry.getKey();
			if (currentVals.containsKey(key)){
				String vals = String.join("\t", Arrays.asList(currentVals.get(key)));
				ps.println(key + "\t" + vals);
			}
			else{
				String vals = String.join("\t", Arrays.asList(entry.getValue()));
				ps.println(key + "\t" + vals);
			}
		}
	}
	
	public boolean addCurrent(String k, String[] v){
		k = k.trim();
		//System.out.print("k is " + k);
		//We need the formal key
		if (defaultTitles.containsValue(k)){
			//Iterate through set of keys and find the key with k as a value
			for (String key : defaultTitles.keySet()){
				if (defaultTitles.get(key)==k){
					k = key;
					break;
				}
			}
		}
		//System.out.println(" and now k is " + k);
		if (defaults.containsKey(k)){
			//If the value for this is different than defaults, add to Current Vals
			if (v[0]  != defaults.get(k)[0]){
				currentVals.put(k, v);
			}
			return true;
		}
		
		System.err.println("Could not add parameter: " + k + ": " + v[0]);
		return false;
	}
	
	public static String getSpecialDefault(String type, String key){
		if (type == "Protein"){
			return proteinDefaults.get(key);
		}
		if (type == "Cell"){
			return cellDefaults.get(key);
		}
		else{
			System.err.println("This value does not exist for " + type);
			return("");
		}
	}
	
	
	public HashMap<String, String> getProteins(){
		return proteins;
	}
	
	public HashMap<String, String> getCells(){
		return cells;
	}
	
	public HashMap<String, ArrayList<String[]>> getGradients(){
		return gradients;
	}
	
	public HashMap<String, ArrayList<String[]>> getWalls(){
		return walls;
	}
	
	public String getStringTitle(String val){
		String s = "Title not found";
		if (defaultTitles.containsKey(val)){
			s = defaultTitles.get(val);
		}
		return s;
	}
	
	public boolean getValue(boolean b, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String[] val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		if (val.length < 1){
			throw new SimException("Variable " + key + " does not have valid value");
		}
		return Boolean.parseBoolean(val[0]);
	}
	
	public int getValue(int i, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String[] val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		if (val.length < 1){
			throw new SimException("Variable " + key + " does not have valid value");
		}
		int result = 0;
		try{
			result = Integer.parseInt(val[0]);
		}
		catch (NumberFormatException e){
			throw new SimException("Number Format Exception for integer variable " + key + ". Unknwon value: " + val[0]);
		}
		return result;
	}
	
	public float getValue(float f, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String[] val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		float result = 0.0f;
		if (val.length < 1){
			throw new SimException("Variable " + key + " does not have valid value");
		}
		try{
			result = Float.parseFloat(val[0]);
		}
		catch (NumberFormatException e){
			throw new SimException("Number Format Exception for float variable " + key + ". Unknwon value: " + val[0]);
		}
		return result;
	}
	
	public long getValue(long lg, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String[] val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		long result = 0L;
		if (val.length < 1){
			throw new SimException("Variable " + key + " does not have valid value");
		}
		try{
			result = Long.parseLong(val[0]);
		}
		catch (NumberFormatException e){
			throw new SimException("Number Format Exception for long variable " + key + ". Unknwon value: " + val[0]);
		}
		return result;
	}
	
	public String getValue(String s, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String val[] = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		if (val.length < 1){
			throw new SimException("Variable " + key + " does not have valid value");
		}
		return val[0];
	}
	
	public Vector3f getValue(Vector3f v, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String[] val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		if (val.length < 3){
			throw new SimException("Variable " + key + " is not a valid Vector3f");
		}
		float[] f = new float[3];
		try{
			for (int i = 0; i < 3; i++){
				f[i] = Float.parseFloat(val[i]);
			}
		}
		catch (NumberFormatException e){
			String value = "";
			for (int j = 0; j < val.length; j++){
				value = value + val[j] + " ";
			}
			throw new SimException("Number Format Exception for float variable " + key + ". Unknwon value: " + value);
			
		}
		return new Vector3f(f);
	}

	
	public String[] getParamList(String type){
		if (type == "Param"){
			return paramOrder;
		}
		else if (type == "Cell"){
			return cellOrder;
		}
		else if (type == "Protein"){
			return proteinOrder;
		}
		int n = defaults.keySet().size();
		String [] ret = new String[n]; 
		return defaults.keySet().toArray(ret);
	}

}
