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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import javax.vecmath.Vector3f;


public class FileGradient implements Gradient{

	/**
	 * This file reads concentrations of a ligand from a text file
	 */
	protected int proteinId;
	protected int axis;
	protected float maxConcentration;
	protected float minConcentration;
	
	private File source;
	private BufferedReader br;
	private boolean gradientSuccessful;
	private boolean finalConcentrations;
	
	float[] baseColor;
	float[] distances;
	float[] prevConc;
	float[] nextConc;
	long previousTime; //These times should be in milliseconds, although they are read in as seconds
	long nextTime;
	
	public FileGradient(int protein, String filename, float[] col) {
		proteinId = protein;
		gradientSuccessful = false;
		finalConcentrations = false;
		axis = 0;
		maxConcentration = Float.MIN_VALUE;
		minConcentration = Float.MAX_VALUE;
		baseColor = new float[]{0.5f, 0.5f, 0.5f, 1f};
		if (col.length >= 3){
			for (int i = 0; i < 3; i++){
				baseColor[i] = col[i];
			}
		}
		if (col.length >= 4){
			baseColor[3] = col[3];
		}
		else{
			baseColor[3] = 1.0f;
		}
		source = new File(filename);
		//Try to initialize distances from file and determine minimum and maximum concentrations
		try {
			String line = "";
            br = new BufferedReader(new FileReader(source));
            
            line = br.readLine();
            String[] lineText = line.split("\t");
            if (lineText.length < 2){
            	throw new SimException("Gradient File must have at least one distance included");
            }
            distances = new float[lineText.length-1];
            //First line is distances. First cell should be empty
            for (int i = 1; i < lineText.length; i++){
            	distances[i-1] = Float.parseFloat(lineText[i]);
            }
            prevConc = new float[distances.length];
            nextConc = new float[distances.length];
            
            //Now read in all of the concentrations to get maximum and minimum values
            //Also set up the first pair of concentration values
            int count = 0;
            while ((line = br.readLine()) != null) {
                lineText = line.split("\t");
                for (int i = 0; i < lineText.length; i++){
                		float value = Float.parseFloat(lineText[i]);
                		if (value > maxConcentration){
                			maxConcentration = value;
                		}
                		if (value < minConcentration){
                			minConcentration = value;
                		}
                		if (count == 0){
                			if (i == 0){
                				previousTime = (long) (value * 1000000);
                			}
                			else{
                				prevConc[i-1] = value;
                			}
                		}
                		else if (count == 1){
                			if (i == 0){
                				nextTime = (long)(value * 1000000);
                			}
                			else{
                				nextConc[i-1] = value;
                			}
                		}
                }
            }
            gradientSuccessful = true;

        } 
		catch (FileNotFoundException e) {
            System.err.println("File for Gradient could not be found: " + filename);
            System.err.println("All concentrations for protein " + proteinId + " set to zero"); //UGH WHERE DO I STORE PROTEIN LIST?
            maxConcentration = 0f;
            minConcentration = 0f;
        }
		catch (IOException e) {
        	System.err.println("Problem reading file for gradient: " + filename);
        	e.printStackTrace();
            System.err.println("All concentrations for protein " + proteinId + " set to zero"); //UGH WHERE DO I STORE PROTEIN LIST?
            maxConcentration = 0f;
            minConcentration = 0f;
        }
		catch (NumberFormatException e){
        	System.err.println("Error reading number from file. Incorrectly formatted.");
        	maxConcentration = 0f;
        	minConcentration = 0f;
        }
		catch (SimException e){
        	System.err.println(e.getMessage());
        	maxConcentration = 0f;
        	minConcentration = 0f;
        }
		finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
	
	public FileGradient(int protein, String filename){
		this(protein, filename, new float[]{0.5f, 0.5f, 0.5f, 1.0f});
	}
	
	@Override
	public float getConcentration(long time, Vector3f position){
		float[] pos = new float[3];
		position.get(pos); //pos has the values copied from position
		if (gradientSuccessful){
			if (time  > nextTime ){
				//pull in the next line of the file - The first number should be the time > nextTime
				try {
					String line = "";
		            br = new BufferedReader(new FileReader(source));
		            
		            while ((line = br.readLine()) != null) {
		            	String[] lineText = line.split("\t");
		            	float value = Float.parseFloat(lineText[0]);
		            	long t = (long) (value * 1000000);
		            	if (t > nextTime){
		            		previousTime = nextTime;
		            		nextTime = t;
		            		for (int i = 1; i < prevConc.length; i++){
		            			prevConc[i-1] = nextConc[i-1];
		            			value = Float.parseFloat(lineText[i]);
		            			nextConc[i-1] = value;
		            		}
		            		break;
		            	}
		            }
				}
				catch (FileNotFoundException e) {
		            System.err.println("File for Gradient could not be found: " + source.getName());
		            System.err.println("All concentrations for protein " + proteinId + " set to zero"); //UGH WHERE DO I STORE PROTEIN LIST?
		            maxConcentration = 0f;
		            minConcentration = 0f;
		        }
				catch (IOException e) {
		        	System.err.println("Problem reading file for gradient: " + source.getName());
		        	e.printStackTrace();
		            System.err.println("All concentrations for protein " + proteinId + " set to zero"); //UGH WHERE DO I STORE PROTEIN LIST?
		            maxConcentration = 0f;
		            minConcentration = 0f;
		        }
				catch (NumberFormatException e){
		        	System.err.println("Error reading number from file. Incorrectly formatted.");
		        	maxConcentration = 0f;
		        	minConcentration = 0f;
		        }
				finally {
		            if (br != null) {
		                try {
		                    br.close();
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }
				
				//if this is the last line of the file, i.e. no line > nextTime then set finalConcentrations to true
				if (time > nextTime){
					finalConcentrations = true;
				}
			}
			
			//find the two distances that position is in between given the axis
			int lastPos = 0;
			int nextPos = 1;
			while (pos[axis] >= distances[nextPos] && nextPos < distances.length){
					lastPos = nextPos;
					nextPos++;
			}
			//ratio from previousTime to nextTime
			long timeDiff = time - previousTime;
			float timeRatio = timeDiff / (float)(nextTime - previousTime);
			//ratio from previousDistance to position
			float distDiff = pos[axis] - distances[lastPos];
			float distRatio = distDiff / (float)(distances[nextPos] - distances[lastPos]);
			if (pos[axis]>distances[nextPos]){
				//The distance is beyond the end of the given concentrations
				//return the concentrations at the maximum distances (nextPos) at this time
				if (finalConcentrations){
					//We are past the maximum time
					return nextConc[nextPos];
				}
				else{
					float concentration = prevConc[nextPos]+ timeRatio * (nextConc[nextPos] - prevConc[nextPos]);
					return concentration;
				}
			}
			if (finalConcentrations){
				//Not past the final distance, but past the final time
				float concentration = nextConc[lastPos] + distRatio * (nextConc[nextPos] - nextConc[lastPos]);
				return concentration;
			}
			//find concentration at this distance at previous time
			float lastConcentration = prevConc[lastPos]+ distRatio * (prevConc[nextPos]-prevConc[lastPos]);
			//find concentration at this distance at next time
			float nextConcentration = nextConc[lastPos]+ distRatio * (nextConc[nextPos]-nextConc[lastPos]);
			//find concentration between the two times
			float concentration = lastConcentration + timeRatio * (nextConcentration - lastConcentration);
			return concentration;
		}
		return 0f;
	}
	
	public int getProtein(){
		return proteinId;
	}
	
	@Override
	public float[] getColor(float con){
		//Find the color that represents this concentration
		//white is minConcentration, baseColor is maximum
		float[] newColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
		if (!gradientSuccessful){
			return newColor;
		}
		if (con >= maxConcentration){
			return baseColor;
		}
		if (con <= minConcentration){
			return newColor;
		}
		float ratio = (con-minConcentration)/(maxConcentration - minConcentration);
		for (int i = 0; i < 3; i++){
			float diff = 1.0f - baseColor[i];
			float newCol = 1.0f - (ratio * diff);
			newColor[i] = newCol;
		}
		return newColor;
	}
	
	@Override
	public int getAxis(){
		return axis;
	}
	
	@Override
	public float getMaxConcentration(){
		return maxConcentration;
	}
	
	@Override
	public float getMinConcentration(){
		return minConcentration;
	}
	
	@Override
	public void setBaseColor(float r, float g, float b){
		baseColor[0] = r;
		baseColor[1] = g;
		baseColor[2] = b;
	}
	
	@Override
	public void setAxis(int a){
		axis = a;
	}
	
	@Override
	public void setMaxConcentration(float c){
		//Do nothing. Concentrations taken from file
	}
	
	@Override
	public void setMinConcentration(float c){
		//Do nothing. Concentrations taken from file
	}
	
	@Override
	public void print(PrintStream p){
		p.println("FileGradient");
		p.println("\tProtein ID: "+ getProtein());
		p.println("\tSource File: " + source.toString());
	}

}