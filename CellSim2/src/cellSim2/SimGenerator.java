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
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;



public class SimGenerator implements Runnable{
	private File inputFile;
	private File outputDir;
	
	public SimGenerator(File in, File out){
		inputFile = in;
		outputDir = out;
	}
	
	public void run() {
		System.out.println("Sim Generator Is Running");
		System.out.println("Input File: " + inputFile.toString());
		System.out.println("Output Directory: " + outputDir.toString());
		try{
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			PrintWriter pw = new PrintWriter(new File(outputDir, "testing.txt"));
			String line = "";
			while (line != null){
				line = br.readLine();
				pw.println(line);
			}
			br.close();
			pw.close();
		}
		catch(IOException e){
			System.out.println(e.toString());
		}
	}

}
