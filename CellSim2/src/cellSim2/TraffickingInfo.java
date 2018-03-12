package cellSim2;

public class TraffickingInfo {
	
	public static final int Secretion = 0, IntUnbound = 1, IntBound = 2;
	private float secretionRate; //in molecules per microseconds
	private float unboundInternalizationRate;  //expressed as per microsecond
	private float boundInternalizationRate; //expressed as per microsecond

	public TraffickingInfo() {
		this(0, 0, 0);
	}
	
	public TraffickingInfo(long s, float u, float b){
		//data are read as per minute, and converted to per microseconds
		secretionRate = (float)(s * (6.0e-7));
		unboundInternalizationRate = (float)(u * (6.0e-7));
		boundInternalizationRate = (float)(b * (6.0e-7));
	}
	
	public float getSecretionRate(){
		return secretionRate;
	}
	
	public float getUnboundIntRate(){
		return unboundInternalizationRate;
	}
	
	public float getBoundIntRate(){
		return boundInternalizationRate;
	}
	
	public float getRate(int i){
		switch(i){
			case 0:
				return secretionRate;
			case 1:
				return unboundInternalizationRate;
			case 2:
				return boundInternalizationRate;
		}
		return secretionRate;
	}
	
	public boolean withinRange(TraffickingInfo ti){
		//returns true if all values are within .1% of ti values
		boolean secretion = Math.abs(ti.getSecretionRate() - secretionRate) <= (ti.getSecretionRate()*.001);
		boolean unbound = Math.abs(ti.getUnboundIntRate() - unboundInternalizationRate) <= (ti.getUnboundIntRate()*.001);
		boolean bound = Math.abs(ti.getBoundIntRate() - boundInternalizationRate) <= (ti.getBoundIntRate()*.001);
		return secretion && unbound && bound;
	}
	
	public boolean isBlank(){
		return (secretionRate == 0 && unboundInternalizationRate == 0 && boundInternalizationRate == 0);
	}
}
