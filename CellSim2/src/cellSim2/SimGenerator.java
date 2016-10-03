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
	
	public ArrayList<Protein> proteins;
	public ArrayList<Gradient> gradients;
	public ArrayList<Wall> walls;
	
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
		for (int i = 0; i < proteins.size(); i++){
			proteins.get(i).print(System.out);
		}
		
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
		System.out.println("Creating Proteins");
		int index = 0;
		HashMap<String, String> pro = simValues.getProteins();
		for (Entry<String, String> entry : pro.entrySet()){
			String key = entry.getKey();
			proteins.add(new Protein(key, entry.getValue(), index));
			index++;
			//System.out.println("Protein: " + key);
		}
	}
	
	private void createGradients(){
		System.out.println("Creating Gradients");
		HashMap<String, String[]> grad = simValues.getGradients();
		for (Entry<String, String[]> entry : grad.entrySet()){
			String key = entry.getKey();
			//Does this protein exist?
			int proId = -1;
			for (int i = 0 ;i <  proteins.size(); i++){
				if (proteins.get(i).getName().equals(key)){
					//protein does exist. Is it diffusible?
					if (!proteins.get(i).canDiffuse()){
						System.err.println("Protein " + key + " is not diffusible. Cannot be used for a gradient");
						break;
					}
					proId = proteins.get(i).getId();
				}
			}
			if (proId < 0){
				System.err.println("Protein " + key + " not in protein list. Gradient not made.");
				continue;
			}
			
			//Does this gradient exist already?
			Gradient g = null;
			for (int i = 0; i < gradients.size(); i++){
				if (gradients.get(i).getProtein() == proId){
					g = gradients.get(i);
				}
			}
			//No the gradient doesn't exist - Make it
			if (g == null){
				//If there is only one value and it is a file, make a FileGradient and continue
				File f = new File(entry.getValue()[0]);
				if (f.exists()){
					g = new FileGradient(proId, entry.getValue()[0]);
					continue;
				}
				//If it's not a file name, make a Zero Gradient
				else{
					g = new ZeroGradient(proId);
				}
			}
			//Yes the gradient did exist or we just made one
			String param = entry.getValue()[0];
			switch (param){
				case "Color":
				case "color":
					if (entry.getValue().length < 4){
						System.err.println("Color must have three float entries. Value only has " + (entry.getValue().length -1));
						break;
					}
					String[] colText = Arrays.copyOfRange(entry.getValue(), 1, 3);
					float[] col = new float[3];
					for (int i = 0; i < 3; i++){
						try{
							col[i] = Float.parseFloat(colText[i]);
							System.out.println(col[i]);
						}catch(NumberFormatException e){
							System.err.println("Color values must be floats. " + colText[i] + " is not valid.");
							break;
						}
					}
					g.setBaseColor(col[0], col[1], col[2]);
					break;
				case "Concentration":
				case "concentration":
					float c = -1f;
					try{
						c = Float.parseFloat(entry.getValue()[1]);
					} catch (NumberFormatException e){
						System.err.println("Concentration value " + param + " not a valid float. Not using.");
						break;
					}
					if (c < 0){
						System.err.println("Concentration value " + c + " not valid. Must be positive. Not using");
						break;
					}
					g.setMaxConcentration(c);
					break;
				case "Axis":
				case "axis":
					int a = -1;
					try{
						a = Integer.parseInt(entry.getValue()[1]);
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
	
	public void createWalls(Simulation sim){
		float channelWidth = 300f, channelHeight = 90f, channelDepth = 100f;
		float wallThick = 2f;
		float[] wallColor = {.9f, .6f, .6f};
		Wall nextWall;
		Vector3f position = new Vector3f(0f, 0f, 0f);
		
		//Make the channel
		//bottom
		position.set(0f, -(float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		nextWall.setColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(true);
		sim.addSimulationObject(nextWall);
		
		//top
		position.set(0f, (float)((channelHeight+wallThick)/2.0), 0f);
		nextWall = new Wall(sim, channelWidth, wallThick, channelDepth, position); 
		nextWall.setColor(wallColor[0], wallColor[1], wallColor[2]);
		sim.addSimulationObject(nextWall);
		
		//back
		position.set(0f, 0f, (float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		nextWall.setColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		sim.addSimulationObject(nextWall);
		
		//left
		position.set((float)((channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position);
		nextWall.setColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		sim.addSimulationObject(nextWall);
		
		//right
		position.set((float)(-(channelWidth+wallThick)/2.0), 0f, 0f);
		nextWall = new Wall(sim, wallThick, channelHeight, channelDepth, position);
		nextWall.setColor(wallColor[0], wallColor[1], wallColor[2]);
		nextWall.setVisible(false);
		sim.addSimulationObject(nextWall);
		
		//front
		position.set(0f, 0f, -(float)((channelDepth+wallThick)/2.0));
		nextWall = new Wall(sim, channelWidth, channelHeight, wallThick, position); 
		nextWall.setVisible(false);
		sim.addSimulationObject(nextWall);
		
		sim.setBaseCameraDistance(110f);
		
	}
	
	public Defaults getValues(){
		return simValues;
	}
	
	public String getProteinName(int id){
		return proteins.get(id).getName();
	}
	
	public static void main(String[] args) {
		System.out.println("Sim Generator Is Running");
		SimGenerator sg = new SimGenerator(new File(args[0]), new File(args[1]));
		boolean showScreen = true;
		int screenwidth = 800;
		int screenheight = 800;
		if (!showScreen){
			screenwidth = 10;
			screenheight = 10;
		}
		Simulation sim = new Simulation(LWJGL.getGL(), sg);
		sim.initPhysics();
		sim.getDynamicsWorld().setDebugDrawer(new GLDebugDrawer(LWJGL.getGL()));

		try{
			LWJGL.main(args, screenwidth, screenheight, ("Cell Simulation:" + sg.inputFile.toString()), sim);
		}
		catch(LWJGLException e){
			System.err.println("Could not run simulation.  Error: " + e.toString());
		}
	}

}
