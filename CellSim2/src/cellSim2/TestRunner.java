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
import java.io.PrintWriter;
//import java.util.Date;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.IOException;


public class TestRunner {
	private static File outputDir = null;
	private static int numRuns = 1;
	private static LinkedHashMap<String, String[]> defaultMap;
	private static LinkedHashMap<String, String[]> paramMap;
	private static LinkedHashMap<String, String> proteinMap;
	private static LinkedHashMap<String, String> cellMap;
	private static LinkedHashMap<String, String[][]> gradientMap;
	private static LinkedHashMap<String, String[][]> wallMap;
	private static LinkedHashMap<String, Integer> dirNames;
	private static ArrayList<GeneratorThread> generators;
	static{
		dirNames = new LinkedHashMap<String, Integer>();
		generators = new ArrayList<GeneratorThread>();
		defaultMap = new LinkedHashMap<String, String[]>();
		paramMap = new LinkedHashMap<String, String[]>();
		proteinMap = new LinkedHashMap<String, String>();
		cellMap = new LinkedHashMap<String, String>();
		gradientMap = new LinkedHashMap<String, String[][]>();
		wallMap = new LinkedHashMap<String, String[][]>();
	}

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
	    if (args.length != 2){
	    	System.err.println("Two variables expected. " + args.length + " found.");
	    	System.err.println("Usage: DEFAULT_FILE VAR_FILE");
	    	System.exit(1);
	    }
	    
	    File pf = new File(args[1]);
	    outputDir = new File(pf.getParent());
	    if (!outputDir.exists() || !outputDir.isDirectory()){
	    	System.err.println("Cannot write to Param File Directory!");
	    	System.err.println("Cannot continue without space for output");
	    	System.exit(2);
	    }
	    
	    FileInputStream fis;
	    InputStreamReader isr;
	    BufferedReader br;
	    
	    //args[0], DEFAULT_FILE should be a valid text file with variable default values
	    //read in all non-blank, non-commented lines into a LinkedHashMap
	    String defaultFile = args[0];
	    
	    try{
			fis = new FileInputStream(defaultFile);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
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
					//System.out.println("TestRunner var: " + var);
					String[] value = Arrays.copyOfRange(valueVar, 1, valueVar.length);
					if (value == null || var == null){
						System.err.println("Badly formatted input. Variable <tab>value.");
						System.err.println("Input was: " + line);
						continue;
					}
					if (var.equals("protein")){
						if (value.length != 2){
							System.err.println("Badly formatted protein input");
							System.err.println("Input was " + line);
							continue;
						}
						proteinMap.put(value[0], value[1]);
						continue;
					}
					if (var.equals("cell")){
						if (value.length != 2){
							System.err.println("Badly formatted cell input");
							System.err.println("Input was " + line);
							continue;
						}
						cellMap.put(value[0], value[1]);
						continue;
					}
					if (var.equals("gradient")){
						if (value.length < 3){
							System.err.println("Badly formatted gradient input");
							System.err.println("Input was " + line);
							continue;
						}
						if (!gradientMap.containsKey(value[0])){
							//make a new array of string arrays
							String[][] newArray = new String[1][];
							newArray[0] = Arrays.copyOfRange(value, 1, value.length);
							gradientMap.put(value[0], newArray);
						}
						else{
							//add a new String array to the end of the old one.
							String[][] oldArray = gradientMap.get(value[0]);
							String[][] newArray = new String[oldArray.length+1][];
							for(int i = 0; i < oldArray.length; i++){
								newArray[i] = oldArray[i];
							}
							newArray[oldArray.length] = Arrays.copyOfRange(value, 1, value.length);
							gradientMap.put(value[0], newArray);
						}
						continue;
					}
					if (var.equals("wall")){
						if (value.length < 3){
							System.err.println("Badly formatted wall input");
							System.err.println("Input was " + line);
							continue;
						}
						if (!wallMap.containsKey(value[0])){
							//make a new array of string arrays
							String[][] newArray = new String[1][];
							newArray[0] = Arrays.copyOfRange(value, 1, value.length);
							wallMap.put(value[0], newArray);
						}
						else{
							//add a new String array to the end of the old one.
							String[][] oldArray = wallMap.get(value[0]);
							String[][] newArray = new String[oldArray.length+1][];
							for(int i = 0; i < oldArray.length; i++){
								newArray[i] = oldArray[i];
							}
							newArray[oldArray.length] = Arrays.copyOfRange(value, 1, value.length);
							wallMap.put(value[0], newArray);
						}
						continue;
					}
					if (!Defaults.variableExists(var)){
						System.err.println("TestRunner reading Defaults: Varible " + var + " does not exist.");
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
	    
	    //DEBUGGING - Print list of default values
	    // System.out.println("Values in default map:");
	    //for(Entry<String, String> e : defaultMap.entrySet()) {
	    //   String var = e.getKey();
	    //    String val = e.getValue();
	    //    System.out.println("\t"+var + ": " + val);
	    //}
	    
	   //args[1], PARAM_FILE should be a valid text file with parameters and test values
	    //The first line should be the number of runs that should be performed and must contain a positive integer
	    //The format of the file shout be parameter<tab>value_1<tab>value_2<tab>...
	    String paramFile = args[1];
	    try{
			fis = new FileInputStream(paramFile);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			String line;
			
			if ((line = br.readLine()) != null){
				try{
					numRuns = Integer.parseInt(line);
				}
				catch (NumberFormatException e){
					System.err.println("First line of paramfile needs positive integer for number of runs");
					System.err.println("Found: " + line);
					System.err.println("Ignoring this line. Default number of runs is " + numRuns);
				}
			}
			
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && !line.contains("\\\\")){
					String[] paramVar = line.split("\t");
					if (paramVar.length < 2){
						System.err.println("Badly formatted input. parameter<tab>value_1<tab>value_2<tab>...");
						System.err.println("Input was: " + line);
						continue;
					}
					if (paramVar[0] == "protein"){
						continue;
						//TODO Right now proteins can't be testing values!
					}
					if (paramVar[0] == "cell"){
						continue;
						//TODO Right now cell changes can't be testing values!
					}
					if (paramVar[0] == "gradient"){
						continue;
						//TODO Right now gradients can't be testing values!
					}
					if (paramVar[0] == "wall"){
						continue;
						//TODO Right now wall changes can't be testing values!
					}
					if (!Defaults.variableExists(paramVar[0])){
						System.err.println("TestRunner Reading Params: Varible " + paramVar[0] + " does not exist.");
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
	    
	    //DEBUGGING - Print list of parameter values
	    //System.out.println("Values in param map:");
	    //for(Entry<String, String[]> e : paramMap.entrySet()) {
	    //    String var = e.getKey();
	    //    String[] val = e.getValue();
	    //    System.out.print("\t"+var + ": \t");
	    //    for (int i = 0; i < val.length; i++){
	    //    	System.out.print(val[i] + " ");
	    //    }
	    //    System.out.println("");
	    //}
	    
	    //Create testing text files
	    //Get an array of the parameter keys
	    Set<String> paramSet = paramMap.keySet();
	    String[] params = paramSet.toArray(new String[0]);
	    for (int i = 0; i < numRuns; i++){
	    	runTests(paramMap, params, null, 0, i);
	    	//Now task list is full
	    	ExecutorService executor = Executors.newCachedThreadPool();
	    	for (int j = 0; j < generators.size(); j++){
	    		executor.execute(generators.get(j));
	    	}
	    	executor.shutdown();
	    	try{
				boolean finished = executor.awaitTermination(60, TimeUnit.MINUTES);
				if (finished){
					System.out.println("Threads completed");
				}
				else{
					System.out.println("Timeout happened first!");
				}
			}
			catch(InterruptedException e){
				System.out.println(e.toString());
			}	    	
	    	generators.clear();
	    	dirNames.clear();
	    }

	}//end main
	
	private static void runTests(LinkedHashMap<String, String[]> paramMap, String[] params, int[] valueIds, int myId, int runNum){
		if (myId >= params.length){
			//all value for parameters are set
			
			//make a directory for this set of parameters
			String dirName = "defaults_";
			if (params.length > 0){
				dirName = "";
			}
			for (int i = 0; i < params.length; i++){
				String[] vals = (String[])paramMap.get(params[i]);
				String val = vals[valueIds[i]];
				//TODO Unit test - value_ids should not be null here
				//System.out.print(params[i] + ": " + val + " ");
				String var = params[i];
				if (var.length()>=4){
					var = var.substring(0, 4);
				}
				dirName = dirName + var + "_" + val + "_";
			}
			dirName = dirName.substring(0, dirName.length()-1);//drop the trailing _
			if (!dirNames.containsKey(dirName)){
				dirNames.put(dirName, new Integer(1));
			}
			else{
				int v = dirNames.get(dirName);
				dirName = dirName + "(" + v + ")";
				dirNames.replace(dirName, new Integer(v+1));
			}
			//System.out.println("DirectoryName: " + dirName + " run " + runNum);
			File paramDir = new File(outputDir, dirName);
			if (!paramDir.exists()){
				paramDir.mkdir();
			}
			//If this is the first run,
			//put a file into this directory with the contents of the default parameters
			//substituting these parameter values
			String fileName = "inputFile.txt";
			File infile = new File(paramDir, fileName);
			if (runNum == 0){
				try{
					PrintWriter pw = new PrintWriter(infile);
					
					//First the current parameters
					//TODO: Get non-singular parameters! For now, params don't have them!
					for (int i = 0; i < params.length; i++){
						String[] vals = (String[])paramMap.get(params[i]);
						String val = vals[valueIds[i]];
						pw.println(params[i] + "\t" + val);
					}
					
					//Then the rest of the defaults
					for(Entry<String, String[]> e : defaultMap.entrySet()) {
				        String var = e.getKey();
				        String[] val = e.getValue();
				        if (!paramMap.containsKey(var)){
				        	String vals = String.join("\t", Arrays.asList(val));
				        	pw.println(var + "\t" + vals);
				        }
				    }
					//Now the proteins
					for(Entry<String, String> e : proteinMap.entrySet()) {
				        String var = e.getKey();
				        String val = e.getValue();
				        pw.println("protein\t" + var + "\t" + val);
				   
				    }
					//Now the gradients
					for(Entry<String, String[][]> e : gradientMap.entrySet()) {
						String var = e.getKey();
						String[][] vals = e.getValue();
						for (int i = 0; i < vals.length; i++){
							String valString = String.join("\t", Arrays.asList(vals[i]));
							pw.println("gradient\t" + var + "\t" + valString);
						}
				    }
					//Now the walls
					for(Entry<String, String[][]> e : wallMap.entrySet()) {
						String var = e.getKey();
						String[][] vals = e.getValue();
						for (int i = 0; i < vals.length; i++){
							String valString = String.join("\t", Arrays.asList(vals[i]));
							pw.println("wall\t" + var + "\t" + valString);
						}
				    }
					
					pw.close();
				}
				catch(IOException e){
					System.err.println("Error: " + e.toString());
					System.err.println("Could not create parameter input files in directory: " + paramDir.toString());;
					System.err.println("Cannot proceed");
					System.exit(3);
				}
			}
			
			//Now create a directory for this run
			String runString = "run_" + runNum;
			File runDir = new File(paramDir, runString);
			if (!runDir.exists()){
				runDir.mkdir();
			}
			
			//Now make a new SimGenerator for this process
			generators.add(new GeneratorThread(infile.toString(), runDir.toString()));
			
			return;
		}
		
		//Params not filled yet. Recursive call to keep filling
		int myValsLen = ((String[])paramMap.get(params[myId])).length;
		int oldLen = 0;
		if (valueIds != null){
			oldLen = valueIds.length;
		}
		int[] newIds = new int[]{0};
		if (valueIds != null){
			newIds = Arrays.copyOf(valueIds, oldLen+1);
		}
		for (int i = 0; i < myValsLen; i++){
			newIds[oldLen] = i;
			runTests(paramMap, params, newIds, myId+1, runNum);
		}
	}
}
	
class GeneratorThread implements Runnable{
	private String infile, outfile;
	private ArrayList<String> command;
		
	public GeneratorThread(String i, String o){
		infile = i;
		outfile = o;
		command = new ArrayList<String>();
		command.add("java");
		command.add("-cp");
		command.add(".:/Users/terri/Documents/javaLibraries/lwjgl-2.9.3/jar/lwjgl.jar:/Users/terri/Documents/javaLibraries/lwjgl-2.9.3/jar/lwjgl_util.jar:/Users/terri/Documents/javaLibraries/jbullet-20101010/dist/jbullet.jar:/Users/terri/Documents/javaLibraries/jbullet-20101010/dist/jbullet-demos.jar:/Users/terri/git/CellSim2/bin/:");
		command.add("-Djava.library.path=/Users/terri/Documents/javaLibraries/lwjgl-2.9.3/native/macosx/");			
		command.add("cellSim2.SimGenerator");
		command.add(infile);
		command.add(outfile);
	}
		
	public void run(){
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.inheritIO();
		try{
			Process p = pb.start();
			int outcome = p.waitFor();
			System.out.println("Process complete. Outcome: " + outcome);
		}
		catch(IOException e){
			System.err.println(e.toString());
		}
		catch(InterruptedException f){
			System.err.println(f.toString());
		}
	}
}