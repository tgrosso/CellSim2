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
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.vecmath.Vector3f;

import com.bulletphysics.demos.opengl.LWJGL;
import com.bulletphysics.demos.opengl.GLDebugDrawer;
import com.bulletphysics.util.ObjectArrayList;

import org.lwjgl.LWJGLException;


public class SimGenerator {
	public static float[][]colorList = new float[][]{
		  { 1.0f, 0.0f, 0.0f },//red
		  { 0.0f, 1.0f, 0.0f },//green
		  { 0.3f, 0.0f, 0.5f},//indigo
		  { 1.0f, 0.5f, 0.0f},//orange
		  { 0.0f, 1.0f, 1.0f}, //cyan
		  { 1.0f, 0.5f, 1.0f}, //violet
		  { 1.0f, 1.0f, 0.0f}, //yellow
		  { 0.0f, 0.0f, 1.0f}, //blue
		};
	
	private File inputFile;
	private File outputDir;
	private Defaults simValues;
	
	public int screenWidth;
	public int screenHeight;
	public float distFromSource;
	public long endTime;
	public boolean generateImages;
	public float secBetweenOutput;
	public int secBetweenImages;
	public boolean displayImages;
	public int speedUp;
	
	public ArrayList<Protein> proteins;
	public ArrayList<Gradient> gradients;
	public ArrayList<Ligand> proteinPairs;
	
	public ImageGenerator imageGen;
	
	public SimGenerator(File in, File out){
		inputFile = in;
		outputDir = out;
		simValues = new Defaults();
		try{
			simValues.readInputFile(in);
		}
		catch(IOException e){
			System.err.println("Could not read all inputs!!");
			System.err.println("Will use some default values");
		}
		//Get defaults for all public variables
		fillVariables();
		
		//Instantiate all proteins
		proteins = new ArrayList<Protein>();
		createProteins();
		//for (int i = 0; i < proteins.size(); i++){
		//	proteins.get(i).print(System.out);
		//}
		
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
			//System.out.println("SG gradient key : " + key);
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
			//System.out.println("Gradient of " +  key);
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
				//System.out.println("SG170 param: " + param);
				if (g == null){
					//gradient doesn't exist yet
					//only valid parameters are "zero" or "file"
					if (param.equalsIgnoreCase("file")){
						//System.out.println("SG175 param: " + param);
						g = new FileGradient(proId, line[1], colorList[proId%colorList.length]);
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
						//Works for both FileGradient (does nothing) or Zero Gradien
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
		float[] wallColor = {.9f, .6f, .6f};
		Wall nextWall;
		Vector3f position = new Vector3f(0f, 0f, 0f);
		
		//Make the vessel
		//bottom
		position.set(0f, -(float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Bottom: " + nextWall.toString());
		
		//top
		position.set(0f, (float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Top: " +nextWall.toString());
		
		//back
		position.set(0f, 0f, (float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Back: " + nextWall.toString());
		
		//front
		position.set(0f, 0f, -(float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Front: "+nextWall.toString());
		
		
		//left
		position.set((float)((channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Left: " + nextWall.toString());
		
		//right
		position.set((float)((-channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position); 
		nextWall.setWallColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		nextWall.setOutputFile(sim.getWallFile());
		walls.add(nextWall);
		//System.out.println("Right: " + nextWall.toString());
		
		sim.setBaseCameraDistance((float)((channelWidth/2)*(1.05)*Math.tan(Math.PI/3)));
		
		//Now take in values from Defaults and update/edit the walls
		HashMap<String, ArrayList<String[]>> wallData = simValues.getWalls();
		for (Entry<String, ArrayList<String[]>> entry : wallData.entrySet()){
			String key = entry.getKey();
			if (key.equals("gradient")){
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
					//If we are here, we have the gradient and the wall id
					Wall oldWall = walls.get(thisWall);
					Vector3f oldSize = oldWall.getSize();
					GradientWall newWall = new GradientWall(sim, oldSize.x, oldSize.y, oldSize.z, oldWall.getOrigin(), g);
					newWall.setVisible(true);
					newWall.setWallColor(0.6f, 0.6f, 1.0f);
					newWall.setDistFromSource(distFromSource);
					//if old wall has protein coatings, they need to be transfered
					int numCoat = oldWall.getNumCoatings();
					if (numCoat > 0){
						for (int p = 0; p < numCoat; p++){
							newWall.coatWithProtein(oldWall.getCoatingProtein(p), oldWall.getCoatingConcentration(p));
						}
					}
					walls.set(thisWall, newWall);
				}
			}
			if (key.equals("coat")){
				ArrayList<String[]> coats = wallData.get(key);
				for (int i = 0; i < coats.size(); i++){
					String[] thisCoat = coats.get(i);
					if (thisCoat.length < 3){
						System.err.println("Bad input for coating wall. Not enough values");
						System.err.println("Usage: wall coat wallID protein surfaceConcentration");
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
					walls.get(thisWall).coatWithProtein(pro, conc);
				}
			}
		}
		for (int i = 0; i < walls.size(); i++){
			sim.addSimulationObject(walls.get(i));
		}
	}
	
	public void createCells(Simulation sim){
		//TODO This could place them more evenly
		//Read in all of the cell files
		//System.out.println("Sim Generator: Creating Cells");
		
		ArrayList<SegmentedCell> cells = new ArrayList<SegmentedCell>();
		int index = 0;
		HashMap<String, String> cellFiles = simValues.getCells();
		for (Entry<String, String> entry : cellFiles.entrySet()){
			String key = entry.getKey();
			cells.addAll(SegmentedCell.readInFile(key, entry.getValue(), sim));
			index++;
			//System.out.println("Cell: " + key);
		}
		
		float maxRadius = 1f;
		for (int i = 0; i < cells.size(); i++){
			SegmentedCell c = cells.get(i);
			if (c.getRadius() > maxRadius){
				maxRadius = c.getRadius();
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
		float cellCenterY = -vesselSize.y/2 + maxRadius + 1;
		System.out.println("Num cells: " + numCells);
		System.out.println("maxRadius: " + maxRadius);
		
		//if there is only one cell. put it right in the middle - it's a testing issue
		if (numCells == 1){
			SegmentedCell c = cells.get(0);
			//sim.removeSimulationObject(c);
			c.setInitialPosition(new Vector3f(0, cellCenterY, 0));
			//sim.addSimulationObject(c);
			return;
		}
		//System.out.println("y: " + cellCenterY);
		//Randomize the grid indexes
		int[] grid = new int[gridNum];
		for (int i = 0; i < gridNum; i++){
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
	
	public static void main(String[] args) {
		//System.out.println("Sim Generator Is Running\n");
		SimGenerator sg = new SimGenerator(new File(args[0]), new File(args[1]));
		boolean showScreen = true;
		int screenwidth = 800;
		int screenheight = 800;
		if (!showScreen){
			screenwidth = 10;
			screenheight = 10;
		}
		Simulation sim = new Simulation(SimLWJGL.getGL(), sg);
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
