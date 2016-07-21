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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
//import java.io.PrintWriter;
//import java.util.Date;

import java.util.LinkedHashMap;
import java.util.Arrays;

import java.io.IOException;


public class TestRunner {
	public static File dataDir = null;

	/**
	 * This class runs one or more simulations and is the class used to run the program. 
	 * It takes as input two text files. One, DEFAULT_FILE, provides a list of variables and their initial
	 * values. The second, PARAM_FILE, is a list of parameters with values that will
	 * be tested. This class will generate simulations for all permutations
	 * of the variable values and create summary files when the simulations
	 * are complete.
	 * All output files will be generated in the directory of the PARAM_FILE.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Number of arguments: " + args.length);
	    if (args.length != 2){
	    	System.err.println("Two variables expected. " + args.length + " found.");
	    	System.err.println("Usage: DEFAULT_FILE VAR_FILE");
	    	System.exit(1);
	    }
	    
	    FileInputStream fis;
	    InputStreamReader isr;
	    BufferedReader br;
	    
	    //args[0], DEFAULT_FILE should be a valid text file with variable default values
	    //read in all non-blank, non-commented lines into a LinkedHashMap
	    LinkedHashMap<String, String> defaultMap = new LinkedHashMap<String, String>();
	    String defaultFile = args[0];
	    
	    try{
			fis = new FileInputStream(defaultFile);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line;
			
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.contains("\\\\")){
					String[] valueVar = line.split("\t");
					if (valueVar.length != 2){
						System.err.println("Badly formatted input. Variable<tab>value.");
						System.err.println("Input was: " + line);
						continue;
					}
					String value = valueVar[0];
					String var = valueVar[1];
					if (value == null || var == null){
						System.err.println("Badly formatted input. Variable <tab>value.");
						System.err.println("Input was: " + line);
						continue;
					}
					defaultMap.put(var, value);
				}
			}
			br.close();
			isr.close();
			fis.close();
	    }
	    catch(IOException e){
			System.err.println("Error reading DEFAULT_VALUE file. (" + defaultFile + ").");
			System.err.println(e.toString());
		}
	    
	   //args[1], PARAM_FILE should be a valid text file with parameters and test values
	    //The parameters must come from the DEFAULT_FILE list of parameters
	    //The format of the file shout be parameter<tab>value_1<tab>value_2<tab>...
	    LinkedHashMap<String, String[]> paramMap = new LinkedHashMap<String, String[]>();
	    String paramFile = args[1];
	    try{
			fis = new FileInputStream(paramFile);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line;
			
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.contains("\\\\")){
					String[] paramVar = line.split("\t");
					if (paramVar.length < 2){
						System.err.println("Badly formatted input. parameter<tab>value_1<tab>value_2<tab>...");
						System.err.println("Input was: " + line);
						continue;
					}
					if (!defaultMap.containsKey(paramVar[0])){
						System.err.println("This parameter is not in the default value file.");
						System.err.println("Parameter was: " + paramVar[0]);
						continue;
					}
					String param = paramVar[0];
					String[] values = Arrays.copyOfRange(paramVar, 1, paramVar.length);
					paramMap.put(param, values);
				}
			}
			br.close();
			isr.close();
			fis.close();
	    }
	    catch(IOException e){
			System.err.println("Error reading PARAM_FILE. (" + paramFile + ").");
			System.err.println(e.toString());
		}
	    

	}

}
