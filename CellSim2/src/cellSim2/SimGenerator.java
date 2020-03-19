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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.Iterator;


import javax.vecmath.Vector3f;

import com.bulletphysics.demos.opengl.LWJGL;
import com.bulletphysics.demos.opengl.GLDebugDrawer;
import com.bulletphysics.util.ObjectArrayList;

import org.lwjgl.LWJGLException;


public class SimGenerator {
	public static float[][]colorList = new float[][]{
		  { 1.0f, 0.0f, 0.0f },//red
		  { 0.0f, 0.9f, 0.9f}, //cyan
		  { 0.5f, 0.5f, 0.1f},//blue
		  { 0.0f, 1.0f, 0.0f },//green
		  { 1.0f, 0.5f, 0.0f},//orange
		  { 1.0f, 0.5f, 1.0f}, //violet
		  { 1.0f, 1.0f, 0.0f}, //yellow
		  { 0.0f, 0.0f, 1.0f}, //blue
		};
	
	private File inputFile;
	private File outputDir;
	private Defaults simValues;
	private final static String basicFile = "BasicTestFile.txt";
	
	public int screenWidth;
	public int screenHeight;
	public float distFromSource;
	public float startX;
	public long endTime;
	public boolean generateImages;
	public float secBetweenOutput;
	public int secBetweenImages;
	public boolean displayImages;
	public int speedUp;
	public float[] simMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
	public float[] simMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
	
	public ArrayList<Protein> proteins;
	public ArrayList<Gradient> gradients;
	
	public ImageGenerator imageGen;
	
	public SimGenerator(File in, File out){
		inputFile = in;
		outputDir = out;
		//System.out.println("outputDir " + outputDir.getAbsolutePath());
		//System.out.println("inputDir " + inputFile.getAbsolutePath());
		simValues = new Defaults();
		try{
			simValues.readInputFile(in);
		}
		catch(IOException e){
			System.err.println("Could not read all inputs!!");
			System.err.println("Will use some default values");
		}
		//System.out.println("SG 90");
		//Get defaults for all public variables
		fillVariables();
		
		//Instantiate all proteins
		proteins = new ArrayList<Protein>();
		createProteins();
		//for (int i = 0; i < proteins.size(); i++){
		//	proteins.get(i).print(System.out);
		//}
		//System.out.println("Proteins created");
		gradients = new ArrayList<Gradient>();
		createGradients();
	}
	
	private void fillVariables(){
		Field[] allFields = SimGenerator.class.getDeclaredFields();
	    for (Field f : allFields) {
	        if (Modifier.isPublic(f.getModifiers())) {
	            String type = f.getType().getName();
	            //System.out.println(f.getName() + ": " + type);
	        	try{
	        		switch(type){
	        		case "int":
	        			f.setInt(this, simValues.getValue(0, f.getName()));
	        			//System.out.println(f.getName() + f.getInt(this));
	        			break;
	        		case "float":
	        			f.setFloat(this, simValues.getValue(0f, f.getName()));
	        			//System.out.println(f.getName() + f.getFloat(this));
	        			break;
	        		case "boolean":
	        			f.setBoolean(this, simValues.getValue(true, f.getName()));
	        			//System.out.println(f.getName() + f.getBoolean(this));
	        			break;
	        		case "long":
	        			f.setLong(this, simValues.getValue(0L, f.getName()));
	        			//System.out.println(f.getName() + f.getLong(this));
	        			break;
	        		}
	        	}
	        	catch(SimException e){
	        		System.err.println("Programming Error! Variable " + f.getName() + " does not have a default value.");
	        		System.err.println("Program cannot proceed.");
	        		System.exit(1);
	        	}
	        	catch(IllegalAccessException g){
	        		System.err.println("Illegal Access!");
	        		System.err.println(g.toString());
	        	}
	        }
	    }
	}
	
	private void createProteins(){
		//System.out.println("Sim Generator: Creating Proteins");
		int index = 0;
		HashMap<String, String> pro = simValues.getProteins();
		for (Entry<String, String> entry : pro.entrySet()){
			String key = entry.getKey();
			proteins.add(new Protein(key, entry.getValue(), index));
			index++;
			//System.out.println("Protein: " + key);
		}
		for (int i = 0; i < proteins.size(); i++){
			Protein p = proteins.get(i);
			p.setLigands(this);
		}
	}
	
	private void createGradients(){
		//System.out.println("Sim Generator: Creating Gradients");
		HashMap<String, ArrayList<String[]>> grad = simValues.getGradients();
		for (Entry<String, ArrayList<String[]>> entry : grad.entrySet()){
			String key = entry.getKey();
			//System.out.println("SG 170: gradient key : " + key);
			//Does this protein exist?
			int proId = -1;
			for (int i = 0 ;i <  proteins.size(); i++){
				if (proteins.get(i).getName().equals(key)){
					//protein does exist. Is it diffusible?
					proId = proteins.get(i).getId();
					if (!proteins.get(i).canDiffuse()){
						System.err.println("Protein " + key + " is not diffusible. Cannot be used for a gradient");
						break;
					}
				}
			}
			if (proId < 0){
				System.err.println("Protein " + key + " not in protein list. Gradient not made.");
				continue;
			}
			//System.out.println("SG 191: Gradient of " +  key);
			//Does this gradient exist already?
			Gradient g = null;
			for (int i = 0; i < gradients.size(); i++){
				if (gradients.get(i).getProtein() == proId){
					g = gradients.get(i);
					//System.out.println("found gradient for " + g.getProtein());
					break;
				}
			}
			//Go through each of the value strings
			ArrayList<String[]> paramStrings = entry.getValue();
			for (int i = 0; i < paramStrings.size(); i++){
				String[] line = paramStrings.get(i);
				String param = line[0];
				//System.out.println("SG202 param: " + param);
				if (g == null){
					//gradient doesn't exist yet
					//only valid parameters are "zero" or "file"
					if (param.equalsIgnoreCase("file")){
						//System.out.println("SG175 param: " + param);
						g = new FileGradient(proteins.get(proId), line[1]);
						FileGradient test = (FileGradient)g;
						if (!test.successfullyMade()){
							System.err.println("FileGradient " + key + " not successful.");
							System.err.println("Making zero gradient");
							g = new ZeroGradient(proId);
						}
					}
					else if (param.compareToIgnoreCase("zero")==0){
						g = new ZeroGradient(proId, Float.parseFloat(line[1]));
						g.setBaseColor(colorList[proId % colorList.length]);
					}
					else{
						System.err.println("Must create gradient before setting parameters");
						System.err.println("Parameter " + param + " not set on gradient " + key);
						continue;
					}
					gradients.add(g);
					continue;
				}
				//g is not null
				switch(param){
					case "Concentration":
					case "concentration":
						float c = -1f;
						try{
							c = Float.parseFloat(line[1]);
						} catch (NumberFormatException e){
							System.err.println("Concentration value " + param + " not a valid float. Not using.");
							break;
						}
						if (c < 0){
							System.err.println("Concentration value " + c + " not valid. Must be positive. Not using");
							break;
						}
						g.setMaxConcentration(c);
						//Works for both FileGradient (does nothing) or Zero Gradient
						break;
					case "Axis":
					case "axis":
						int a = -1;
						try{
							a = Integer.parseInt(line[1]);
						}catch(NumberFormatException e){
							System.err.println("Axis value " + param + " not a valid integer. Not using");
							break;
						}
						if (a < 0 || a > 2){
							System.err.println("Axis value " + " not a valid integer. Must be between 0 and 2. Not using.");
							break;
						}
						g.setAxis(a);
					default:
						System.err.println("Gradient parameter " + param + " is not valid");
				}
			}
		}
	}
	
	public void createWalls(Simulation sim){
		//System.out.println("SimGen 272 - Create walls");
		ArrayList<Wall> walls = new ArrayList<Wall>();
		Vector3f vesselSize = new Vector3f();
		try{
			vesselSize = simValues.getValue(vesselSize, "vessel");
		}catch(SimException e){
			System.err.println("Programming Error! Variable vessel does not have a default value.");
    		System.err.println("Program cannot proceed.");
    		System.exit(1);
		}
		//System.out.println("Vessel size: " + vesselSize.toString());
		float channelWidth = vesselSize.x, channelHeight = vesselSize.y, channelDepth = vesselSize.z;
		float wallThick = 2f;
		//startX = (-channelWidth+wallThick)/2.0f;
		//System.out.println("startX = " + startX);
		//float[] wallColor = {.9f, .6f, .6f};
		Wall nextWall;
		Vector3f position = new Vector3f(0f, 0f, 0f);
		
		//Make the vessel
		//bottom
		position.set(0f, -(float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Bottom: " + nextWall.toString());
		
		//top
		position.set(0f, (float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		
		//System.out.println("Top: " +nextWall.toString());
	
		//back
		position.set(0f, 0f, (float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Back: " + nextWall.toString());
		
		
		//front
		position.set(0f, 0f, -(float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Front: "+nextWall.toString());
		
		
		//left
		position.set((float)((channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		//walls.add(nextWall);
		//System.out.println("Left: " + nextWall.toString());
		
		
		//right
		position.set((float)((-channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position); 
		//nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		//walls.add(nextWall);
		//System.out.println("Right: " + nextWall.toString());
		
		sim.setBaseCameraDistance((float)((channelWidth/2)*(1.05)*Math.tan(Math.PI/3)));
		
		//Now take in values from Defaults and update/edit the walls
		HashMap<String, ArrayList<String[]>> wallData = simValues.getWalls();
		for (Entry<String, ArrayList<String[]>> entry : wallData.entrySet()){
			String key = entry.getKey();
			if (key.equals("gradient")){
				//System.out.println("SG 353");
				ArrayList<String[]> grads = wallData.get(key);
				for (int i = 0; i < grads.size(); i++){
					String[] thisGrad = grads.get(i);
					if (thisGrad.length < 2){
						System.err.println("Bad input for gradient wall. Not enough values");
						System.err.println("Usage: wall gradient wallID protein");
						System.err.println("Not using");
						continue;
					}
					int thisWall = -1;
					try{
						thisWall = Integer.parseInt(thisGrad[0]);
					}
					catch(NumberFormatException e){
						System.err.println("Format problem. Wall ID must be integer. Found: " + thisGrad[0]);
						continue;
					}
					if (thisWall < 0 || thisWall >= walls.size()){
						System.err.println("Wall id is not valid. " + thisGrad[0]);
						continue;
					}
					//System.out.println("SG349 - Adding gradient to wall " + thisWall);
					//Change the wall in the ArrayList to a gradient wall with this gradient
					int pro = -1;
					for (int j = 0; j < proteins.size(); j++){
						Protein n = proteins.get(j);
						if (n.getName().equals(thisGrad[1])){
							pro = n.getId();
						}
					}
					if (pro < 0){
						System.err.println("Protein " + thisGrad[1] + " is not on the protein list.");
						System.err.println("Not using");
						continue;
					}
					//System.out.println("Protein for gradient: " + pro);
					//System.out.println("Num gradients: " + gradients.size());
					Gradient g = null;
					for (int j = 0; j < gradients.size(); j++){
						Gradient n = gradients.get(j);
						if (n.getProtein() == pro){
							g = n;
						}
					}
					if (g == null){
						System.err.println("No gradient found for : " + thisGrad[1]);
						System.err.println("Not using");
						continue;
					}
					//System.out.println("SG 403: Making gradient wall");
					//If we are here, we have the gradient and the wall id
					Wall oldWall = walls.get(thisWall);
					Vector3f oldSize = oldWall.getSize();
					GradientWall newWall = new GradientWall(sim, oldSize.x, oldSize.y, oldSize.z, oldWall.getOrigin(), g);
					newWall.setVisible(true);
					newWall.setWallColor(0.6f, 0.6f, 1.0f);
					newWall.setDistFromSource(distFromSource);
					//if old wall has SurfaceSegments, they need to be transfered
					int numSegs = oldWall.getNumSegments();
					for (int p = 0; p < numSegs; p++){
						oldWall.getSurfaceSegment(p).setNewParent(newWall);
					}
					walls.set(thisWall, newWall);
					//System.out.println("SG 417: Gradient wall added " + newWall.toString());
				}
			}
			if (key.equals("coat")){
				ArrayList<String[]> coats = wallData.get(key);
				for (int i = 0; i < coats.size(); i++){
					String[] thisCoat = coats.get(i);
					if (thisCoat.length < 4){
						System.err.println("Bad input for coating wall. Not enough values");
						System.err.println("Usage: wall coat wallID protein surfaceConcentration surface");
						System.err.println("Not using");
						continue;
					}
					int thisWall = -1;
					try{
						thisWall = Integer.parseInt(thisCoat[0]);
					}
					catch(NumberFormatException e){
						System.err.println("Format problem. Wall ID must be integer. Found: " + thisCoat[0]);
						continue;
					}
					if (thisWall < 0 || thisWall >= walls.size()){
						System.err.println("Wall id is not valid. " + thisCoat[0]);
						continue;
					}
					//Coat this wall with the appropriate protein
					int pro = -1;
					//System.out.println("SG411 - Adding protein coat to wall " + thisWall);
					for (int j = 0; j < proteins.size(); j++){
						Protein n = proteins.get(j);
						if (n.getName().equals(thisCoat[1])){
							if (!n.canBindToSurface()){
								System.err.println("Protein " + thisCoat[1] + " does not bind to surfaces. Not coating wall.");
								continue;
							}
							pro = n.getId();
						}
					}
					if (pro < 0){
						System.err.println("Protein " + thisCoat[1] + " is not on the protein list.");
						System.err.println("Not using");
						continue;
					}
					float conc = 0f;
					try{
						conc = Float.parseFloat(thisCoat[2]);
					}
					catch(NumberFormatException f){
						System.err.println("Could not parse surface concentration: " + thisCoat[2]);
						continue;
					}
					//Now find which surface
					int surf = -1;
					switch(thisCoat[3]){
						case "FRONT":
							surf = Wall.FRONT;
							break;
						case "BACK":
							surf = Wall.BACK;
							break;
						case "TOP":
							surf = Wall.TOP;
							break;
						case "BOTTOM":
							surf = Wall.BOTTOM;
							break;
						case "LEFT":
							surf = Wall.LEFT;
							break;
						case "RIGHT":
							surf = Wall.RIGHT;
							break;
						default:
							System.err.println("Wall surface must be BACK, BOTTOM, FRONT, LEFT, RIGHT or TOP. Found: " + thisCoat[3]);
							continue;
					}
					walls.get(thisWall).coatWithProtein(pro, conc, surf);
				}
			}
		}
		for (int i = 0; i < walls.size(); i++){
			sim.addSimulationObject(walls.get(i));
		}
		Vector3f tempMin =new Vector3f();
		Vector3f tempMax =new Vector3f();
		float[] tMin = new float[3];
		float[] tMax = new float[3];
		for (int i = 0; i < walls.size(); i++){
			//System.out.println("Debugging: SimGenerator");
			Wall w = walls.get(i);
			//System.out.println("Wall number: " + i);
			HashSet<Integer> lig = w.getReceptorsBindTo();
			HashSet<Integer> rec = w.getSurfaceProteins();
			Iterator<Integer> iter = lig.iterator();
			//System.out.println("Receptors on this wall bind to:");
			/*while (iter.hasNext()) {
			    //System.out.println("    " + sim.getProteinName(iter.next().intValue()));
			}*/
			iter = rec.iterator();
			//System.out.println("Proteins on this wall:");
			/*while (iter.hasNext()) {
			    System.out.println("    " + sim.getProteinName(iter.next().intValue()));
			}*/
			
			//Set up the minimum and maximum bounds of the simulation boundaries
			w.getRigidBody().getAabb(tempMin, tempMax);
			tempMin.get(tMin);
			tempMax.get(tMax);
			for (int j = 0; j < 3; j++){
				if (tMin[j] < simMin[j]){
					simMin[j] = tMin[j];
				}
				if (tMax[j] > simMax[j]){
					simMax[j] = tMax[j];
				}
			}
		}
	}
	
	public void createCells(Simulation sim){
		//TODO This could place them more evenly
		//Read in all of the cell files
		//System.out.println("Sim Generator: Creating Cells");
		
		ArrayList<SegmentedCell> cells = new ArrayList<SegmentedCell>();
		
		HashMap<String, String> cellFiles = simValues.getCells();
		for (Entry<String, String> entry : cellFiles.entrySet()){
			String key = entry.getKey();
			//System.out.println("getting these cells: key: " + key + " val: " + entry.getValue());
			cells.addAll(SegmentedCell.readInFile(key, entry.getValue(), sim));
		}
		
		float maxRadius = 1f;
		float minY = 100;
		for (int i = 0; i < cells.size(); i++){
			SegmentedCell c = cells.get(i);
			if (c.getRadius() > maxRadius){
				maxRadius = c.getRadius();
			}
			if (c.getMinY() < minY){
				minY = c.getMinY();
			}
		}
		int numCells = cells.size();
		//System.out.println("MaxRadius: " + maxRadius);
		//Get the dimensions of the vessel
		Vector3f vesselSize = new Vector3f();
		try{
			vesselSize = simValues.getValue(vesselSize, "vessel");
		}catch(SimException e){
			System.err.println("Programming Error! Variable vessel does not have a default value.");
    		System.err.println("Program cannot proceed.");
    		System.exit(1);
		}
		//System.out.println("Vessel Size: " + vesselSize.toString());
		//Divide the x,z axis into a grid to fit the cells
		float gridSize = maxRadius * 2.1f; //Give a little extra space between cells
		int rows = (int)(vesselSize.z / gridSize);
		int cols = (int)(vesselSize.x / gridSize);
		int gridNum = rows * cols;
		//System.out.println("gridSize:  " + gridSize + " rows:" + rows + " cols: " + cols + " gridNum: " + gridNum);
		
		if (numCells > gridNum){
			numCells = gridNum;
			System.err.println("Maximum number of single layer cells is " + gridNum);
		}
		//Determine the y value for the origin of each cell
		float cellCenterY = -vesselSize.y/2 - minY;
		//System.out.println("Num cells: " + numCells);
		//System.out.println("maxRadius: " + maxRadius);
		//System.out.println("Center y: " + cellCenterY);
		
		//if there is only one cell. put it right in the middle - it's a testing issue
		if (numCells == 1){
			SegmentedCell c = cells.get(0);
			c.setInitialPosition(new Vector3f(0, cellCenterY, 0));
		}
		else{
			//System.out.println("y: " + cellCenterY);
			//Randomize the grid indexes
			int[] grid = new int[gridNum];
			for	(int i = 0; i < gridNum; i++){
				grid[i] = i;
			}
			int temp = 0;
			for (int i = 0; i < gridNum; i++){
				int nextIndex = (int)(sim.getNextRandomF() * gridNum);
				//System.out.println(i + "nextIndex: " + nextIndex);
				temp = grid[nextIndex];
				grid[nextIndex] = grid[i];
				grid[i] = temp;
			}
			//Add cells into the grid in random order
		
			for (int i = 0; i < numCells; i++){
				int g_row = grid[i] / cols;
				int g_col = grid[i] % cols;
				float x = (vesselSize.x/2)-(gridSize / 2) - (g_col * gridSize);
				float z = (vesselSize.z/2)-(gridSize / 2) - (g_row * gridSize);
				SegmentedCell c = cells.get(i);
				//sim.removeSimulationObject(c);
				c.setInitialPosition(new Vector3f(x, cellCenterY, z));
				//sim.addSimulationObject(c);
			}
		}
		//For debugging
		/*
		for (int i = 0; i < numCells; i++){
			//System.out.println("Debugging: SimGenerator");
			SegmentedCell c = cells.get(i);
			
			//System.out.println("End of making cells: " + c.getRigidBody().getCenterOfMassPosition(out));
			//System.out.println("Cell number: " + i);
			//HashSet<Integer> lig = c.getReceptorsBindTo();
			//HashSet<Integer> rec = c.getSurfaceProteins();
			//Iterator<Integer> iter = lig.iterator();
			//System.out.println("Receptors on this cell bind to:");
			//while (iter.hasNext()) {
			//    System.out.println("    " + sim.getProteinName(iter.next().intValue()));
			//}
			//iter = rec.iterator();
			//System.out.println("Proteins on this cell:");
			//while (iter.hasNext()) {
			//    System.out.println("    " + sim.getProteinName(iter.next().intValue()));
			//}
		}*/
	}
	
	public Defaults getValues(){
		return simValues;
	}
	
	public String getProteinName(int id){
		return proteins.get(id).getName();
	}
	
	public int getProteinId(String s){
		for (int i = 0; i < proteins.size(); i++){
			Protein p = proteins.get(i);
			if (p.getName().compareTo(s)==0){
				return p.getId();
			}
		}
		//If we are here, the protein doesn't exist in the simulation
		return (-1);
	}
	
	public File getOutputDir(){
		return outputDir;
	}
	
	public Gradient getGradient(int proId){
		//This really should be a hash map!!
		Gradient grad = null;
		for (int i = 0; i < gradients.size(); i++){
			grad = gradients.get(i);
			if (grad.getProtein() == proId){
				return grad;
			}
		}
		return grad;
	}
	
	public void outputFiles(File dir){
		try{
			//Write the default values from Defaults to the base file
			File baseFile = new File(dir, basicFile);
			PrintStream out = new PrintStream(baseFile);
			getValues().printDefaultValues(out);
			//Write each cell file 
			//Write each protein file
			//Write each gradient file
			//Write each wall file
			out.flush();
			out.close();
		}
		catch (FileNotFoundException e){
			System.err.println("SimGenerator 696: File to write not found");
		}
		catch (IOException f){
			System.err.println("SimGenerator 699: Output error writing output files");
		}
	}
	
	public static void main (String[] args){
		//System.out.println("Sim Generator Is Running\n");
		SimGenerator sg = new SimGenerator(new File(args[0]), new File(args[1]));
		long seed = -1;
		if (args.length > 2){
			seed = Integer.getInteger(args[2]);
		}
		//seed = 3066472563200689152L;
		//System.out.println(sg);

		boolean showScreen = true;
		int screenwidth = 800;
		int screenheight = 800;
		if (!showScreen){
			screenwidth = 10;
			screenheight = 10;
		}
		Simulation sim = null;
		if (seed < 0){
			sim = new Simulation(SimLWJGL.getGL(), sg);
		}
		else{
			sim = new Simulation(SimLWJGL.getGL(), sg, seed);
		}
		sim.initPhysics();
		sim.getDynamicsWorld().setDebugDrawer(new GLDebugDrawer(SimLWJGL.getGL()));

		try{
			SimLWJGL.main(args, screenwidth, screenheight, ("Cell Simulation:" + sg.inputFile.toString()), sim);
		}
		catch(LWJGLException e){
			System.err.println("Could not run simulation.  Error: " + e.toString());
		}
	}

}
