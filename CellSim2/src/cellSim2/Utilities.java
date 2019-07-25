package cellSim2;

import javax.vecmath.Vector3f;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class Utilities {

	private Utilities() {
	}
	
	public static void makeBonds(Simulation s, int rec, int lig, SurfaceSegment recSurface, SurfaceSegment ligSurface){
		Protein receptor = s.getProtein(rec);
		int ligandIndex = receptor.proteinIndex(lig);
		float bondLength = receptor.getBondLength(ligandIndex);
		float bindingRate = receptor.getBindingRate(ligandIndex);
		float timeToStable = receptor.getTimeToStable(ligandIndex);

		int recSurfaceId = recSurface.getID();
		int ligSurfaceId = ligSurface.getID();
		
		
		//If the bonds (which project along the normal to recSurface)
		//are parallel to ligSurface, we can't make bonds here.
		Vector3f recNormal = recSurface.getParent().getSegmentWorldNormal(recSurfaceId);
		Vector3f ligNormal = ligSurface.getParent().getSegmentWorldNormal(ligSurfaceId);
		//System.out.println("ligSurfaceId " + ligSurfaceId + " ligNormal: " + ligNormal);
		float normalsDot = recNormal.dot(ligNormal);
		//System.out.println("Length of normals: " + recNormal.length() + ", " + ligNormal.length() + " dot Product " + normalsDot);
		
		if (normalsDot == 0f){
			return;
		}
		
		//The area in which we look for bonds is the receptor surface area. If the other surface is smaller, we just get 
		//fewer bonds created.
		float bindingArea = Math.abs(recSurface.getParent().getSegmentArea(recSurfaceId));
		if (bindingArea == 0){
			System.err.println("Utilities-makeBonds: A surface with no area?");
			return;
		}
		
		//Find the portion of the ligand surface that we are looking at. The whole surface or less
		float ligandArea = Math.abs(ligSurface.getParent().getSegmentArea(ligSurfaceId));
		float ligandPortion = Math.min(bindingArea/ligandArea, 1.0f);
		//The volume of the space with the binding is taking place is the area * the length of the bond
		float bindingVolume = bindingArea * bondLength;
		
		//Get number of unbound receptors
		float numUnboundReceptors = recSurface.getNumMolecules(rec, 1, false);
		float totalLigands = ligSurface.getNumMolecules(lig, 1, false);
		float numLigands = ligSurface.getNumMolecules(lig, ligandPortion, false);
		float ligandConc = numLigands/bindingVolume;
		//convert ligandConc to nM : 10^15 (cubic micron/L) * 10^9 nMoles/mole / 6.02 * 10^23 molecules/mole
		//= 1.6611.2957
		ligandConc *= 1.66112975;
		
		//Try to make bonds using the differential equations
		float time = s.getDeltaTimeMicroseconds();
		/*System.out.println("binding area: " + bindingArea + " totalLigands: " + totalLigands);
		System.out.println("dt: " + time + " ligandArea: " + ligandArea + " ligandPortion: " + ligandPortion);
		System.out.println("binding Volume: " + bindingVolume + " unbound Receptors: " + numUnboundReceptors + " bindingRate: " + bindingRate);
		System.out.println("numLigands: " + numLigands + " ligandConc: " + ligandConc + " MOLS_PER_BOND: " + Protein.MOLS_PER_BOND);
		*/
		float maxBonds = (time * bindingRate * numUnboundReceptors * ligandConc/Protein.MOLS_PER_BOND);
		//System.out.println("Maximum Bonds: " + maxBonds);
		
		//TODO If the number is under some kind of minimum, use the Gillespie algorithm
		
		//Now that we know how many bonds to attempt, let's attempt them
		Vector3f[] recWorldVertices = recSurface.getParent().getWorldCoordinates(recSurfaceId);
		Vector3f[] ligWorldVertices = ligSurface.getParent().getWorldCoordinates(ligSurfaceId);
		Vector3f recOrigin = new Vector3f(), ligOrigin = new Vector3f();
		recSurface.getParent().getRigidBody().getCenterOfMassPosition(recOrigin);
		ligSurface.getParent().getRigidBody().getCenterOfMassPosition(ligOrigin);
		
		
		
		for (int i = 0; i < (int)(maxBonds); i++){
			//Find a random point on the receptor surface
			//adamswaab.wordpress.com/2009/12/11/random-point-in-a-triangle-barycentric-coordinates/
			//Get two vectors on the triangle
			Vector3f T1 = new Vector3f(recWorldVertices[1]);//AB
			T1.sub(recWorldVertices[0]);
			Vector3f T2 = new Vector3f(recWorldVertices[2]);//AC
			T2.sub(recWorldVertices[0]);
			//System.out.println("initial 0: " + recWorldVertices[0] + " initial 1: " + recWorldVertices[1] + " initial 2: " + recWorldVertices[2]);
			//System.out.println("T1 :" + T1 + " T2: " + T2);
			
			//Get two random values
			float r = s.getNextRandomF();
			float t = s.getNextRandomF();
			while(r==t){ //make sure they are different
				r = s.getNextRandomF();
			}
			if (r + t >= 1){
				r = 1 - r;
				t = 1 - t;
			}
			T1.scale(r);
			T2.scale(t);
			//System.out.println("r: " + r + " s: " + s + "T1 :" + T1 + " T2: " + T2);
			Vector3f randVec = new Vector3f(recWorldVertices[0]);
			randVec.add(T1);
			randVec.add(T2);
			//System.out.println("Random Triangle Point: " + randVec);
			
			//using these algorithm: http://geomalgorithms.com/a05-_intersect-1.html
			Vector3f w = new Vector3f(randVec);
			w.sub(ligWorldVertices[0]);
			float s1 = -ligNormal.dot(w)/normalsDot;
			Vector3f intersection = new Vector3f(recNormal);
			intersection.scale(s1);
			intersection.add(randVec);
			//System.out.println("ligNorm: " + ligNormal + " ligWorldVertices[0] " + ligWorldVertices[0]);
			//System.out.println("randVec: " + randVec + " recOrigin: " + recOrigin);
			
			//System.out.println("s1 : " + s1 + ", " + intersection);
			//Find the intersection of this point with the plane of the other surface
			//See if the intersection point is within the bound of the other surface.
			if (s1 <= bondLength){
				//System.out.println("Make a bond");
				Vector3f localRec = new Vector3f(randVec);
				localRec.sub(recOrigin);
				//System.out.println(" local point on triangle: " + localRec);
				//System.out.println("intersection: " + intersection + " ligOrigin: " + ligOrigin);
				Vector3f localLig = new Vector3f(intersection);
				localLig.sub(ligOrigin);
				Transform localA = new Transform(), localB = new Transform();
				localA.setIdentity();
				localB.setIdentity();
				localA.origin.set(localRec);
				localB.origin.set(localLig);
				//System.out.println(" local point on wall: " + localLig);
				//BondConstraint(Simulation s, float stab, SurfaceSegment ssa, SurfaceSegment ssb, int proa, int prob, RigidBody rbA, RigidBody rbB, Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA) {
				BondConstraint bc = new BondConstraint(s, timeToStable, bondLength, recSurface, ligSurface, rec, lig, recSurface.getParent().getRigidBody(), ligSurface.getParent().getRigidBody(), localA, localB, true);
			}
		}
	}

}


