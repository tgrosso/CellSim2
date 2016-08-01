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

import java.util.HashMap;
import java.io.PrintStream;
import java.util.Map.Entry;


public class Defaults {
	
	private static final HashMap<String, String> defaults = new HashMap<String, String>();
	static{
		defaults.put("numCells", "5");
		defaults.put("cellDetailLevel", "1");
		defaults.put("TimeZone", "GMT");
		defaults.put("channelWidth", "600");
		defaults.put("screenWidth", "900");
		defaults.put("screenHeight", "600");
		defaults.put("endTime", "30");
	}
	
	private HashMap<String, String> currentVals;
	
	public Defaults(){
		currentVals = new HashMap<String, String>();
	}
	
	public static boolean variableExists(String key){
		return defaults.containsKey(key);
	}
	
	public void printDefaultValues(PrintStream ps){
		for (Entry<String, String> entry : defaults.entrySet()){
			String key = entry.getKey();
			if (currentVals.containsKey(key)){
				ps.println(key + ": " + currentVals.get(key));
			}
			else{
				ps.println(key + ": " + entry.getValue());
			}
		}
	}
	
	public boolean addCurrent(String k, String v){
		k = k.trim();
		v = v.trim();
		if (!defaults.containsKey(k)){
			System.err.println("Variable " + k + " not found. (" + k + ", " + v + " not added.");
			return false;
		}
		currentVals.put(k, v);
		return true;
	}
	
	public boolean getValue(boolean b, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		return Boolean.parseBoolean(val);
	}
	
	public int getValue(int i, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		int result = 0;
		try{
			result = Integer.parseInt(val);
		}
		catch (NumberFormatException e){
			throw new SimException("Number Format Exception for integer variable " + key + ". Unknwon value: " + val);
		}
		return result;
	}
	
	public float getValue(float f, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		float result = 0.0f;
		try{
			result = Float.parseFloat(val);
		}
		catch (NumberFormatException e){
			throw new SimException("Number Format Exception for float variable " + key + ". Unknwon value: " + val);
		}
		return result;
	}
	
	public String getValue(String s, String key) throws SimException{
		if (!defaults.containsKey(key)){
			throw new SimException("Variable " + key + " not found in default list.");
		}
		String val = defaults.get(key);
		if (currentVals.containsKey(key)){
			val = currentVals.get(key);
		}
		return val;
	}

}
