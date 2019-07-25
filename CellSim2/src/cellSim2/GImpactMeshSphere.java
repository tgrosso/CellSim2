package cellSim2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.extras.gimpact.GImpactMeshShape;


public class GImpactMeshSphere extends GImpactMeshShape{

private final static int VERTEX_DIM = 3, EDGE_INDICES = 2, TRIANGLE_INDICES = 3;
	
	private static int maxSteps = 4;
	private static float[][] vertices;
	private static int[][] edges;
	private static int[][] trianglesByEdges;
	private static int[][] trianglesByVertices;
	private static TriangleIndexVertexArray[] indexVertexArray;
		
	private int detail_level;

	static{
		vertices = new float[maxSteps][];
		edges = new int[maxSteps][];
		trianglesByEdges = new int[maxSteps][];
		trianglesByVertices = new int[maxSteps][];
		indexVertexArray = new TriangleIndexVertexArray[maxSteps];
		initializeTriangles();
		for (int i = 1; i < maxSteps; i++){
			growSphere(i);
		}
		/*
		for (int i = 0; i < maxSteps; i++){
			System.out.println("Vertices Step " + i+ ": ");
			for (int j = 0; j < vertices[i].length/VERTEX_DIM; j++){
				System.out.print("Vertex " + j + ": ");
				for (int k = 0; k < VERTEX_DIM; k++){
					System.out.print(vertices[i][j * VERTEX_DIM +k] + "\t");
				}
				System.out.println("");
			}
			System.out.println("Edges Step " + i+ ": ");
			for (int j = 0; j < edges[i].length/EDGE_INDICES; j++){
				System.out.print("Edge " + j + ": ");
				for (int k = 0; k < EDGE_INDICES; k++){
					System.out.print(edges[i][j * EDGE_INDICES +k] + "\t");
				}
				System.out.println("");
			}
			System.out.println("Triangles By Edges Step " + i+ ": ");
			for (int j = 0; j < trianglesByEdges[i].length/TRIANGLE_INDICES; j++){
				System.out.print("Triangle " + j + ": ");
				for (int k = 0; k < TRIANGLE_INDICES; k++){
					System.out.print(trianglesByEdges[i][j * TRIANGLE_INDICES +k] + "\t");
				}
				System.out.println("");
			}
			System.out.println("Triangles By Vertices Step " + i + ": ");
			for (int j = 0; j < trianglesByVertices[i].length/TRIANGLE_INDICES; j++){
				System.out.print("Triangle " + j + ": ");
				for (int k = 0; k < TRIANGLE_INDICES; k++){
					System.out.print(trianglesByVertices[i][j * TRIANGLE_INDICES +k] + "\t");
				}
				System.out.println("");
			}
		}*/

		
		for (int i = 0; i < maxSteps; i++){
			makeIndexArray(i);
		}
		
		
	}
	
	private static void initializeTriangles(){
		//float cornerValue = (float)Math.sin(Math.toRadians(45.0));

		float a = (float)(0.816496580927726);
		float b = (float)(0.5773502691896257);
		float c = (float)(0.40824829046386296);
		float d = (float)(0.7071067811865475);
		vertices[0] = new float[]{
			/*
			0.0f, 1.0f, 0.0f,
			-cornerValue, 0.0f, -cornerValue,
			cornerValue, 0.0f, -cornerValue,
			cornerValue, 0.0f, cornerValue,
			-cornerValue, 0.0f, cornerValue,
			0.0f, -1.0f, 0.0f*/
			-a, b, 0.0f,
			-c, -b, -d,
			c, b, -d, 
			c, b, d,
			-c, -b, d,
			a, -b, 0.0f
		};
		
		edges[0] = new int[]{
				0, 1,
				0, 2,
				0, 3,
				0, 4,
				1, 2,
				2, 3,
				3, 4,
				4, 1,
				5, 1,
				5, 2,
				5, 3,
				5, 4
		};
		
		trianglesByEdges[0] = new int[]{
			0, 4, 1,
			1, 5, 2,
			2, 6, 3,
			3, 7, 0,
			8, 4, 9,
			9, 5, 10,
			10, 6, 11,
			11, 7, 8
		};
		trianglesByVertices[0] = getVerticesFromEdges(0);
	}
	
	private static void growSphere(int step){
		//Algorithm:
		//Add a new vertex to each edges, doubling the number of edges
		//Move the vertex out to the sphere
		//Make new triangles with the new edges
		 
		vertices[step] = addNewVertices(step);
		edges[step] = addNewEdges(step);
		trianglesByEdges[step] = addNewTriangles(step);
		trianglesByVertices[step] = getVerticesFromEdges(step);
	}
	
	private static float[] addNewVertices(int step){
		//Returns a new float array with old vertices at the beginning
		//New vertices are in order of the edges they are associated with
		int numVertices = vertices[step-1].length/VERTEX_DIM;
		int numEdges = edges[step-1].length/EDGE_INDICES;
		int numNewVertices = numVertices + numEdges;
		float[] newVertices = new float[numNewVertices * VERTEX_DIM];
		//System.out.println("total new vertices: " + numNewVertices);
		
		for (int i = 0; i < (numVertices * VERTEX_DIM); i++){
			newVertices[i] = vertices[step-1][i];
		}
		
		float[] oldVert0 = new float[3];
		float[] oldVert1 = new float[3];
		float[] newVert = new float[3];
		Vector3f scaledVert = new Vector3f();
		for (int i = 0; i < numEdges; i++){
			int firstVertexIndex = edges[step-1][i * EDGE_INDICES];
			int secondVertexIndex = edges[step-1][i * EDGE_INDICES + 1];
			//System.out.println("Edge " + i + ": " + firstVertexIndex + ", " + secondVertexIndex);
			for (int j = 0; j < 3; j++){
				oldVert0[j] = vertices[step-1][firstVertexIndex * VERTEX_DIM + j];
				oldVert1[j] = vertices[step-1][secondVertexIndex * VERTEX_DIM + j];
				newVert[j] = (float)((oldVert0[j] + oldVert1[j])/2.0);
				//System.out.println("Old-0 " + oldVert0[j] + " Old1 " + oldVert1[j]);
			}
			
			scaledVert = new Vector3f(newVert);
			scaledVert.normalize();
			
			newVertices[(numVertices + i) * VERTEX_DIM] = scaledVert.x;
			newVertices[(numVertices + i) * VERTEX_DIM + 1 ] = scaledVert.y;
			newVertices[(numVertices + i) * VERTEX_DIM + 2] = scaledVert.z;
		}
		
		return newVertices;
	}
	
	private static int[] addNewEdges(int step){
		int numTriangles = trianglesByEdges[step-1].length/TRIANGLE_INDICES;
		int numOldEdges = edges[step-1].length/EDGE_INDICES;
		int numOldVertices = vertices[step-1].length/VERTEX_DIM;
		int numNewEdges = 2 * numOldEdges + numTriangles * 3; 
		int[] newEdges = new int[numNewEdges * EDGE_INDICES];
		//System.out.println("Num New Edges: " + numNewEdges);
		
		//Make two new edges from each old edge
		for (int i = 0; i < numOldEdges; i++){
			int newVertexIndex = numOldVertices + i;
			newEdges[i * 4] = edges[step-1][i * EDGE_INDICES];
			newEdges[i * 4 + 1] = newVertexIndex;
			newEdges[i * 4 + 2] = newVertexIndex;
			newEdges[i * 4 + 3] = edges[step-1][i* EDGE_INDICES +1];
		}
		
		int nextEdgeIndex = EDGE_INDICES * numOldEdges;
		
		//Make three new edges from each triangle
		for (int i = 0; i < numTriangles; i++){
			int edge0 = trianglesByEdges[step-1][i * TRIANGLE_INDICES];
			int newVertex0 = numOldVertices + edge0;
			
			int edge1 = trianglesByEdges[step-1][i * TRIANGLE_INDICES +1];
			int newVertex1 = numOldVertices + edge1;
			
			int edge2 = trianglesByEdges[step-1][i * TRIANGLE_INDICES +2];
			int newVertex2 = numOldVertices + edge2;
			
			/*
			System.out.print("Triangle: " + i);
			System.out.print(" Edge 0: " + edge0);
			System.out.print(" Edge 1: " + edge1);
			System.out.println(" Edge 2: " + edge2);
			*/
			
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6] = newVertex0;
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6 + 1] = newVertex1;
			
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6 + 2] = newVertex1;
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6 + 3] = newVertex2;
			
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6 + 4] = newVertex2;
			newEdges[nextEdgeIndex * EDGE_INDICES + i * 6 + 5] = newVertex0;
			
			/*
			System.out.print("New Vertex 0: " + newVertex0);
			System.out.print(" New Vertex 1: " + newVertex1);
			System.out.println(" New Vertex 2: " + newVertex2);
			*/
		}
		
		
		return newEdges;
	}

	private static int[] addNewTriangles(int step){
		//System.out.println("Add New Triangles");
		int numOldTriangles = trianglesByEdges[step-1].length/TRIANGLE_INDICES;
		int newTrianglesForOld = 4; //Each old triangle becomes four new ones
		int numNewTriangles = numOldTriangles * newTrianglesForOld;
		int[] newTrianglesByEdge = new int[numNewTriangles * TRIANGLE_INDICES];
		
		for (int i = 0; i < numOldTriangles; i++){
			//System.out.println("Triangle: " + i + ": ");
			int newTriangleIndex = i *  newTrianglesForOld;
			
			int edge0 = trianglesByEdges[step-1][i * TRIANGLE_INDICES];
			int edge1 = trianglesByEdges[step-1][i * TRIANGLE_INDICES + 1];
			int edge2 = trianglesByEdges[step-1][i * TRIANGLE_INDICES + 2];
			//System.out.println("Edge0 : " + edge0 + " Edge1: "+ edge1 + " Edge2: " + edge2);
			
			int newEdgeA = edge0 * 2 + 1;
			int newEdgeB = edge1 * 2;
			int newEdgeC = (numOldTriangles + i) * TRIANGLE_INDICES;
			if (getSharedVertex(step, newEdgeA, newEdgeB) < 0){
				//Find the correct edges for this triangle
				int[] newEdges = getSharedEdges(step, edge0, edge1);
				newEdgeA = newEdges[0];
				newEdgeB = newEdges[1];
			}
			//System.out.print("New Triangle " + newTriangleIndex + ":");
			//System.out.println("newEdgeA: " + newEdgeA + " newEdgeB: " + newEdgeB + " newEdgeC: " + newEdgeC);
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES] = newEdgeA;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 1] = newEdgeB;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 2] = newEdgeC;
			
			newEdgeA = edge1 * 2 + 1;
			newEdgeB = edge2 * 2 + 1;
			newEdgeC = (numOldTriangles + i) * TRIANGLE_INDICES + 1;
			if (getSharedVertex(step, newEdgeA, newEdgeB) < 0){
				//Find the correct edges for this triangle
				int[] newEdges = getSharedEdges(step, edge1, edge2);
				newEdgeA = newEdges[0];
				newEdgeB = newEdges[1];
			}
			//System.out.print("New Triangle " + (newTriangleIndex+1) + ":");
			//System.out.println("newEdgeA: " + newEdgeA + " newEdgeB: " + newEdgeB + " newEdgeC: " + newEdgeC);
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 3] = newEdgeA;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 4] = newEdgeB;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 5] = newEdgeC;
			
			newEdgeA = edge2 * 2;
			newEdgeB = edge0 * 2;
			newEdgeC = (numOldTriangles + i) * TRIANGLE_INDICES + 2;
			if (getSharedVertex(step, newEdgeA, newEdgeB) < 0){
				int[] newEdges = getSharedEdges(step, edge2, edge0);
				newEdgeA = newEdges[0];
				newEdgeB = newEdges[1];
			}
			//System.out.print("New Triangle " + (newTriangleIndex+2) + ":");
			//System.out.println("newEdgeA: " + newEdgeA + " newEdgeB: " + newEdgeB + " newEdgeC: " + newEdgeC);
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 6] = newEdgeA;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 7] = newEdgeB;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 8] = newEdgeC;
			
			
			newEdgeA = (numOldTriangles + i) * TRIANGLE_INDICES;
			newEdgeB = (numOldTriangles + i) * TRIANGLE_INDICES + 1;
			newEdgeC = (numOldTriangles + i) * TRIANGLE_INDICES + 2;
			//System.out.print("New Triangle " + (newTriangleIndex+3) + ":");
			//System.out.println("newEdgeA: " + newEdgeA + " newEdgeB: " + newEdgeB + " newEdgeC: " + newEdgeC);
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 9] = newEdgeA;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES + 10] = newEdgeB;
			newTrianglesByEdge[newTriangleIndex * TRIANGLE_INDICES +  11] = newEdgeC;
			
			//DIRECTION MATTERS!  Determine which are going the wrong way and switch them!
		}
		
		return newTrianglesByEdge;
	}
	
	private static int getSharedVertex(int step, int edge0, int edge1){
		//returns either the vertex shared by the two edges or -1
		int vertex00 = edges[step][edge0 * EDGE_INDICES];
		int vertex01 = edges[step][edge0 * EDGE_INDICES +1];
		int vertex10 = edges[step][edge1 * EDGE_INDICES];
		int vertex11 = edges[step][edge1 * EDGE_INDICES + 1];
		if (vertex00 == vertex10 || vertex00 == vertex11){
			return vertex00;
		}
		else if (vertex01 == vertex10 || vertex01==vertex11){
			return vertex01;
		}
		else{
			return -1;
		}
	}
	
	private static int[] getSharedEdges(int step, int oldEdge0, int oldEdge1){
		int[] sharedEdges = new int[2];
		int edge00 = oldEdge0 * 2;
		int edge01 = oldEdge0 * 2 + 1;
		int edge10 = oldEdge1 * 2;
		int edge11 = oldEdge1 * 2 + 1;
		
		if (getSharedVertex(step, edge00, edge10) >= 0){
			sharedEdges[0] = edge00;
			sharedEdges[1] = edge10;
		}
		else if (getSharedVertex(step, edge00, edge11) >= 0){
			sharedEdges[0] = edge00;
			sharedEdges[1] = edge11;
		}
		else if (getSharedVertex(step, edge01, edge10) >= 0){
			sharedEdges[0] = edge01;
			sharedEdges[1] = edge10;
		}
		else if (getSharedVertex(step, edge01, edge11) >= 0){
			sharedEdges[0] = edge01;
			sharedEdges[1] = edge11;
		}
				
		return sharedEdges;
	}
	
	private static int[] getVerticesFromEdges(int step){
		//System.out.println("Getting Vertex Indices:");
		int[] newTrianglesByVertices = new int[trianglesByEdges[step].length];
		int numTriangles = trianglesByEdges[step].length / TRIANGLE_INDICES;

		
		for (int i = 0; i < numTriangles; i++){
			int edge1 = trianglesByEdges[step][i * TRIANGLE_INDICES];
			//System.out.println("Triangle" + i  + " edge1 " + edge1);

			int edge2 = trianglesByEdges[step][i * TRIANGLE_INDICES + 1];
			//System.out.println("Triangle" + i  + " edge2 " + edge2);
			
			int vertexIndex0 = edges[step][edge1 * EDGE_INDICES];
			//System.out.println("Triangle" + i  + " vertexIndex 0 " + vertexIndex0);
			
			int vertexIndex1 = edges[step][edge1 * EDGE_INDICES + 1];
			//System.out.println("Triangle" + i  + " vertexIndex1 " + vertexIndex1);
			
			int vertexIndex2 = edges[step][edge2 * EDGE_INDICES + 1 ];
			if (vertexIndex2 == vertexIndex0 || vertexIndex2 == vertexIndex1){
				vertexIndex2 = edges[step][edge2 * EDGE_INDICES];
			}
			//System.out.println("Triangle" + i  + " vertexIndex2 " + vertexIndex2);
			
			Vector3f p0 = new Vector3f();
			Vector3f p1 = new Vector3f();
			Vector3f p2 = new Vector3f();
			 
			int base0 = vertexIndex0 * VERTEX_DIM;
			int base1 = vertexIndex1 * VERTEX_DIM;
			int base2 = vertexIndex2 * VERTEX_DIM;
			
			//Ensure that normal of triangle points away from the center
			p0.set(vertices[step][base0], vertices[step][base0 + 1], vertices[step][base0 + 2]);
			p1.set(vertices[step][base1], vertices[step][base1 + 1], vertices[step][base1 + 2]);
			p2.set(vertices[step][base2], vertices[step][base2 + 1], vertices[step][base2 + 2]);

			/*
			if (step <= 1){
			System.out.print("Step 1: Triangle: " + i +
						" p0 " + p0 + " p1: " + p1 + " p2: " + p2);
			}
			*/
			
			Vector3f centroid = new Vector3f((float)((p0.x + p1.x + p2.x)/3.0), 
					(float)((p0.y + p1.y + p2.y)/3.0),
					(float)((p0.z + p1.z + p2.z)/3.0));
			centroid.normalize();
			
			p1.sub(p0);
			p2.sub(p0);
			/*
			if (step <= 1){
				System.out.print(" p1-p0 " + p1 + " p2-p0: " + p2);
			}*/
			
			Vector3f crossProd = new Vector3f(0f,0f,0f);
			crossProd.cross(p1, p2);
			crossProd.normalize();
			/*
			if (step <= 1){
				System.out.print(" cross p1, p2 " + crossProd);
			}
			*/
			float dot = centroid.dot(crossProd);
			
			
			if (dot < 0){
				int temp = vertexIndex1;
				vertexIndex1 = vertexIndex2;
				vertexIndex2 = temp;
			}
			/*
			if (step <= 1){
				System.out.print(" centroid: " + centroid + "  dotProduct1: " + dot);
			}
			*/
			
			base0 = vertexIndex0 * VERTEX_DIM;
			base1 = vertexIndex1 * VERTEX_DIM;
			base2 = vertexIndex2 * VERTEX_DIM;
			
			//Ensure that normal of triangle points away from the center
			p0.set(vertices[step][base0], vertices[step][base0 + 1], vertices[step][base0 + 2]);
			p1.set(vertices[step][base1], vertices[step][base1 + 1], vertices[step][base1 + 2]);
			p2.set(vertices[step][base2], vertices[step][base2 + 1], vertices[step][base2 + 2]);

			centroid = new Vector3f((float)((p0.x + p1.x + p2.x)/3.0), 
					(float)((p0.y + p1.y + p2.y)/3.0),
					(float)((p0.z + p1.z + p2.z)/3.0));
			centroid.normalize();
			
			p1.sub(p0);
			p2.sub(p0);
			
			crossProd = new Vector3f(0f,0f,0f);
			crossProd.cross(p1, p2);
			crossProd.normalize();
			
			dot = centroid.dot(crossProd);
			if (Math.abs(dot) <= .5){
				//System.out.println("************Step: " + step + " Triangle " + i + " dot2: " + dot);
			} 
			
			newTrianglesByVertices[i * TRIANGLE_INDICES] = vertexIndex0;
			newTrianglesByVertices[i * TRIANGLE_INDICES + 1] = vertexIndex1;
			newTrianglesByVertices[i * TRIANGLE_INDICES + 2] = vertexIndex2;
		}
		return newTrianglesByVertices;
	}
	
	private static void makeIndexArray(int step){
		ByteBuffer verticesBuffer = ByteBuffer.allocateDirect(vertices[step].length*4).order(ByteOrder.nativeOrder());
		for (int i=0; i<vertices[step].length; i++) {
			verticesBuffer.putFloat(vertices[step][i]);
		}
		verticesBuffer.flip();
		
		ByteBuffer indicesBuffer = ByteBuffer.allocateDirect(trianglesByVertices[step].length*4).order(ByteOrder.nativeOrder());
		for (int i=0; i<trianglesByVertices[step].length; i++) {
			indicesBuffer.putInt(trianglesByVertices[step][i]);
		}
		indicesBuffer.flip();
		
		int numTriangles = trianglesByVertices[step].length/3;
		int numVertices = vertices[step].length/3;
		int triangleIndexStride = 4 * 3;
		int vertexStride = 4 * 3;
		
		indexVertexArray[step] = new TriangleIndexVertexArray(numTriangles, indicesBuffer, triangleIndexStride, numVertices, verticesBuffer, vertexStride);
	}
	
	public GImpactMeshSphere(int dl){
		super(indexVertexArray[dl]);
		detail_level = dl;
		outputSomeData();
	}
	
	public int getNumTriangles(){
		int numTriangles = trianglesByVertices[detail_level].length/TRIANGLE_INDICES;
		return numTriangles;
	}
	
	public void outputSomeData(){
		/*
		System.out.println("detail level: " + detail_level);
		for (int i = 0; i < vertices[detail_level].length/TRIANGLE_INDICES; i++){
			float xVal = vertices[detail_level][i*TRIANGLE_INDICES];
			float yVal = vertices[detail_level][i*TRIANGLE_INDICES];
			float zVal = vertices[detail_level][i*TRIANGLE_INDICES];
			System.out.println("Vertex " + i + ": " + xVal + ", " + yVal + ", " + zVal);
		}*/
		/*
		System.out.println("num triangles: " + getNumTriangles());
		System.out.println("len triangles by vertices: " + trianglesByVertices[detail_level].length);
		System.out.println("len vertices: " + vertices[detail_level].length);
		//int numTriangles = getNumTriangles();
		int lowestVertex = 0;
		float bottomVertex = 3;
		for (int i = 0; i < vertices[detail_level].length/TRIANGLE_INDICES; i++){
			int yIndex = i*TRIANGLE_INDICES +1;
			//System.out.print("Vertex " + i + ": " + vertices[detail_level][xIndex] + ", " + vertices[detail_level][yIndex] + ", " + vertices[detail_level][zIndex]);
			if (vertices[detail_level][yIndex]< bottomVertex){
				lowestVertex = yIndex;
				bottomVertex = vertices[detail_level][yIndex];
			}
		}
		System.out.println("Lowest Vertex = " + lowestVertex);
		System.out.println("Bottom Vertex = " + bottomVertex);
		for (int i = 0; i < trianglesByVertices[detail_level].length/TRIANGLE_INDICES; i++){
			int vertex0 = i*TRIANGLE_INDICES;
			int vertex1 = i*TRIANGLE_INDICES + 1;
			int vertex2 = i*TRIANGLE_INDICES + 2;
			if (vertex0 == lowestVertex || vertex1 == lowestVertex || vertex2 == lowestVertex){
				System.out.println("Triangle " + i + ":");
				System.out.println("V0: " + vertex0 + " V1: " + vertex1 + " V2: " + vertex2);
				System.out.println(vertices[detail_level][vertex0] + ", " + vertices[detail_level][vertex1] + ", " + vertices[detail_level][vertex2]);
			}
		}*/
	}
	
	public float getLowestY(){
		float bottomVertex = 3;
		for (int i = 0; i < vertices[detail_level].length/3; i++){
			int yIndex = i*3 +1;
			if (vertices[detail_level][yIndex]< bottomVertex){
				bottomVertex = vertices[detail_level][yIndex];
			}
		}
		return bottomVertex;
	}


}
