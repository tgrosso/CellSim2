package cellSim2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.File;

public class SimRandom {
	//Implementation of Marsaglia Xorshift random number generator
	//https://www.javamex.com/tutorials/random_numbers/xorshift.shtml#.XFOoR89KjBI
	private long seed;

	public SimRandom() {
		// Generate a relatively random seed
		//Use /dev/urandom if possible
		//If not, use System.nanoTime()
		try{
			FileInputStream fis = new FileInputStream("/dev/urandom");
			byte[] bArray = new byte[8];
			fis.read(bArray);
			long s = 0;
			for (int i = 0; i < 8; i++){
				s <<= 8;
				s |= (bArray[i] & 0xFF);
			}
			seed = Math.abs(s);
			//System.out.println("from /dev/urandom");
		}
		catch (SecurityException|IOException ex){
			seed = System.nanoTime();
		}
	}
	
	public SimRandom(long s){
		seed = s;
	}
	
	long getNextPositiveRandom(){
		seed ^= (seed << 21);
        seed ^= (seed >>> 35);
        seed ^= (seed << 4);
        return Math.abs(seed);
	}
	
	long getNextSignedRandom(){
		seed ^= (seed << 21);
        seed ^= (seed >>> 35);
        seed ^= (seed << 4);
        return seed;
	}
	
	float getRandomSignedFloat(){
		//returns a value between -1.0 and 1.0
        return ((float)getNextSignedRandom()/Long.MAX_VALUE);
	}
	
	float getRandomPositiveFloat(){
		//returns a value between 0.0 and 1.0
        return ((float)getNextPositiveRandom()/Long.MAX_VALUE);
	}
	
	public static void main(String args[]){
		SimRandom sr = new SimRandom();
		ByteBuffer bytes = ByteBuffer.allocate(8);
		for (int i = 0; i < 50; i++){
			String val = String.format("%02d", i);
			String filename = "Test"+val+".txt";
			File f = new File(filename);
			
			try{
				//System.out.println(f.createNewFile());
				FileOutputStream fos = new FileOutputStream(f);
				//System.out.println(f.exists());
				for (int j = 0; j < 10000; j++){
					Long x = sr.getNextPositiveRandom();
					//System.out.println(x);
					bytes.putLong(0, x);
					fos.write(bytes.array());
				}
				fos.flush();
				fos.close();
			}
			catch(IOException e){
				System.out.println("Could not write to file");
				System.out.println(e.toString());
			}
		}
		
	}

}
