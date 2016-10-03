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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.vecmath.Vector3f;


public class Defaults {
	
	private static final HashMap<String, String[]> defaults = new HashMap<String, String[]>();
	static{
		defaults.put("TimeZone", new String[]{"GMT"});
		defaults.put("screenWidth", new String[]{"900"});
		defaults.put("screenHeight", new String[]{"600"});
		defaults.put("endTime", new String[]{"30"});
		defaults.put("numCells", new String[]{"5"});
		defaults.put("cellDetailLevel", new String[]{"1"});
		defaults.put("displayImages", new String[]{"true"});
		defaults.put("vessel", new String[]{"300", "90", "100"});
		defaults.put("distFromSource", new String[]{"0"});
		defaults.put("generateImages", new String[]{"false"});
		defaults.put("secBetweenOutput", new String[]{".5"});
		defaults.put("secBetweenImages", new String[]{"1"});
		defaults.put("speedUp", new String[]{"1"});
	}
	
	private static final HashMap<String, String> cellDefaults = new HashMap<String, String>();
	static{
		cellDefaults.put("Name", "Unnamed Cell Type");
		cellDefaults.put("numCells", "5");
		cellDefaults.put("cellDetailLevel", "1");
		cellDefaults.put("radius", "20");
	}
	
	private static final HashMap<String, String> proteinDefaults = new HashMap<String, String>();
	static{
		proteinDefaults.put("Name", "No Name");
		proteinDefaults.put("bindsToSurface", "false");
		proteinDefaults.put("membraneBound", "Fasle");
		proteinDefaults.put("diffusible", "false");
	}
	
	private HashMap<String, String[]> currentVals;
	private HashMap<String, String> proteins;
	private HashMap<String, String[]> gradients;
	
	public Defaults(){
		currentVals = new HashMap<String, String[]>();
		proteins = new HashMap<String, String>();
		gradients = new HashMap<String, String[]>();
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
				String[] value = Arrays.copyOfRange(valueVar, 1, valueVar.length);
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
					gradients.put(pro, value);
					continue;
				}
				if (!Defaults.variableExists(var)){
					System.err.println("Varible " + var + " does not exist.");
					continue;
				}
				addCurrent(var, value);
			}
		}
		br.close();
		isr.close();
		fis.close();
	}
	
	public static boolean variableExists(String key){
		return defaults.containsKey(key);
	}
	
	public static boolean variableExists(String type, String key){
		if (type.equals("Protein")){
			return proteinDefaults.containsKey(key);
		}
		return false;
	}
	
	public void printDefaultValues(PrintStream ps){
		for (Entry<String, String[]> entry : defaults.entrySet()){
			String key = entry.getKey();
			if (currentVals.containsKey(key)){
				String vals = String.join("\t", Arrays.asList(currentVals.get(key)));
				ps.println(key + ": " + vals);
			}
			else{
				String vals = String.join("\t", Arrays.asList(entry.getValue()));
				ps.println(key + ": " + vals);
			}
		}
	}
	
	public boolean addCurrent(String k, String[] v){
		k = k.trim();
		for (int i = 0; i < v.length; i++){
			v[i] = v[i].trim();
		}
		if (!defaults.containsKey(k)){
			String vals = String.join("\t", Arrays.asList(v));
			System.err.println("Variable " + k + " not found. (" + k + ", " + vals + " not added.");
			return false;
		}
		currentVals.put(k, v);
		return true;
	}
	
	public static String getSpecialDefault(String type, String key){
		if (type == "Protein"){
			return proteinDefaults.get(key);
		}
		else{
			System.err.println("This value does not exist for " + type);
			return("");
		}
	}
	
	public HashMap<String, String> getProteins(){
		return proteins;
	}
	
	public HashMap<String, String[]> getGradients(){
		return gradients;
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

}
