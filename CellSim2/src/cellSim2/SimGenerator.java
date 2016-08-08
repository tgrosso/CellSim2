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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.bulletphysics.demos.opengl.LWJGL;
import com.bulletphysics.demos.opengl.GLDebugDrawer;

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
	
	
	
	public SimGenerator(File in, File out){
		inputFile = in;
		outputDir = out;
		simValues = new Defaults();
		//Get defaults for all public variables
		Field[] allFields = SimGenerator.class.getDeclaredFields();
	    for (Field f : allFields) {
	        if (Modifier.isPublic(f.getModifiers())) {
	            String type = f.getType().getName();
	            System.out.println(f.getName() + ": " + type);
	        	try{
	        		switch(type){
	        		case "int":
	        			f.setInt(this, simValues.getValue(0, f.getName()));
	        			System.out.println(f.getName() + f.getInt(this));
	        			break;
	        		case "float":
	        			f.setFloat(this, simValues.getValue(0f, f.getName()));
	        			System.out.println(f.getName() + f.getFloat(this));
	        			break;
	        		case "boolean":
	        			f.setBoolean(this, simValues.getValue(true, f.getName()));
	        			System.out.println(f.getName() + f.getBoolean(this));
	        			break;
	        		case "long":
	        			f.setLong(this, simValues.getValue(0L, f.getName()));
	        			System.out.println(f.getName() + f.getLong(this));
	        			break;
	        		default:
	        			System.err.println("Programming Error! Variable " + f.getName() + " does not have a default value.");
		        		System.err.println("Program cannot proceed.");
		        		System.exit(1);
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
	
	public Defaults getValues(){
		return simValues;
	}
	
	public static void main(String[] args) {
		System.out.println("Sim Generator Is Running");
		SimGenerator sg = new SimGenerator(new File(args[0]), new File(args[1]));
		Defaults def = sg.getValues();
		boolean showScreen = true;
		int screenwidth = 800;
		int screenheight = 800;
		if (!showScreen){
			screenwidth = 10;
			screenheight = 10;
		}
		try{
			showScreen = def.getValue(showScreen, "displayImages");
			screenwidth = def.getValue(screenwidth, "screenWidth");
			screenheight = def.getValue(screenheight, "screenHeight");
		}
		catch(SimException e){
			System.err.println(e.toString());
		}
		System.out.println("Input File: " + sg.inputFile.toString());
		System.out.println("Output Directory: " + sg.outputDir.toString());
		Simulation sim = new Simulation(LWJGL.getGL());
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
