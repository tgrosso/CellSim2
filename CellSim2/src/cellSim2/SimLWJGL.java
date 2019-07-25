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

/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */
//This is an extension of the JBullet LWJGL class

import java.awt.event.KeyEvent;
import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.GL11;
import com.bulletphysics.demos.opengl.IGL;
import com.bulletphysics.demos.opengl.LwjglGL;
import java.util.Date;

/**
 *
 * @author jezek2
 */

public class SimLWJGL {

	private static boolean redisplay = false;
	private static LwjglGL gl = new LwjglGL();
	
	public static void postRedisplay() {
		redisplay = true;
	}

	public static IGL getGL() {
		return gl;
	}
	
	public static int main(String[] args, int width, int height, String title, Simulation demoApp) throws LWJGLException {
		Display.setDisplayMode(new DisplayMode(width, height));
		Display.setTitle(title);
		Display.create(new PixelFormat(0, 24, 0));
		
		Keyboard.create();
		Keyboard.enableRepeatEvents(true);
		Mouse.create();
		
		gl.init();
		
		demoApp.myinit();
		demoApp.reshape(width, height);
		
		boolean quit = false;
		
		long lastTime = System.currentTimeMillis();
		int frames = 0;
		
		while (!Display.isCloseRequested() && !quit &&!demoApp.readyToQuit()) {
			demoApp.moveAndDisplay();
			Display.update();
			
			if (demoApp.timeToOutputImage()){
				GL11.glReadBuffer(GL11.GL_FRONT);
				int w = Display.getDisplayMode().getWidth();
				int h= Display.getDisplayMode().getHeight();
				ByteBuffer buf = demoApp.getImageBuffer(w, h);
				//System.out.println("w: " + w + " h: " + h + " Buf size: " + buf.capacity());
				GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
				demoApp.outputImage();
			}

			int modifiers = 0;
			if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) modifiers |= KeyEvent.SHIFT_DOWN_MASK;
			if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) modifiers |= KeyEvent.CTRL_DOWN_MASK;
			if (Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA)) modifiers |= KeyEvent.ALT_DOWN_MASK;
		
			while (Keyboard.next()) {
				if (Keyboard.getEventCharacter() != '\0') {
					demoApp.keyboardCallback(Keyboard.getEventCharacter(), Mouse.getX(), Mouse.getY(), modifiers);
				}
				
				if (Keyboard.getEventKeyState()) {
					demoApp.specialKeyboard(Keyboard.getEventKey(), Mouse.getX(), Mouse.getY(), modifiers);
				}
				else {
					demoApp.specialKeyboardUp(Keyboard.getEventKey(), Mouse.getX(), Mouse.getY(), modifiers);
				}
									
				if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) quit = true;
				if (Keyboard.getEventKey() == Keyboard.KEY_Q) quit = true;
			}
			
			while (Mouse.next()) {
				if (Mouse.getEventButton() != -1) {
					int btn = Mouse.getEventButton();
					if (btn == 1) {
						btn = 2;
					}
					else if (btn == 2) {
						btn = 1;
					}
					demoApp.mouseFunc(btn, Mouse.getEventButtonState()? 0 : 1, Mouse.getEventX(), Display.getDisplayMode().getHeight()-1 - Mouse.getEventY());
				}
				demoApp.mouseMotionFunc(Mouse.getEventX(), Display.getDisplayMode().getHeight()-1 - Mouse.getEventY());
			}
			
			long time = System.currentTimeMillis();
			if (time - lastTime < 1000) {
				frames++;
			}
			else {
				Display.setTitle(title+" | FPS: "+frames);
				lastTime = time;
				frames = 0;
			}
		}
		
			//This line is the whole point of making this class
		demoApp.wrapUp();
		Display.destroy();
		demoApp.destroy();
		return 0;
	}
	

}
