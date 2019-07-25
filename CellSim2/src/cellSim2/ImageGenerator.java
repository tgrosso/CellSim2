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

package cellSim2;

/**
 * @author Terri Applewhite-Grosso
 *
 */


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;



public class ImageGenerator {

	private int frameNumber;
	private int width, height, bytesPerPixel;
	private String format;
	private ByteBuffer buf;
	private File baseFile;
	
	public ImageGenerator(File base, int w, int h, int bbp) {
		baseFile = base;
		width = w;
		height = h;
		bytesPerPixel = bbp;
		buf = BufferUtils.createByteBuffer(100 * 100 * 3);
		frameNumber = 0;
		format = "PNG";
	}

	public ByteBuffer getBuffer(int w, int h){
		if (w != width || h != height){
			width = w;
			height = h;
			buf = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		}
		return buf;
	}
	
	public boolean makeImage(String time){
		String fileName = "img-" + String.format("%09d", frameNumber) + ".png";
		File imageFile = new File(baseFile, fileName);
		
		try{
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for(int i = 0; i < width; i++){
				for(int j = 0; j < height; j++){
					int index = ((width * j) + i) * bytesPerPixel;
					int r = buf.get(index) & 0xFF;
					int g = buf.get(index + 1) & 0xFF;
					int b = buf.get(index + 2) & 0xFF;
					image.setRGB(i, height - (j + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
				}
			}
			Graphics g = image.getGraphics();
			g.setColor(Color.BLACK);
			
			g.drawString(("Experimental Time: " + time), width - 250, height-10);
			g.dispose();
			ImageIO.write(image, format, imageFile);
		}
		catch(IOException e){
			System.out.println("Error generating image " + frameNumber);
			e.printStackTrace();
			return false;
		}
		frameNumber++;
		return true;
	}
}
