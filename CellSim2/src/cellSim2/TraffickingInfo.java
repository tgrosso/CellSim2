package cellSim2;

public class TraffickingInfo {
	
	public static final int Secretion = 0, IntUnbound = 1, IntBound = 2;
	private float secretionRate; //in molecules per microseconds
	private float unboundInternalizationRate;  //expressed as per microsecond
	private float boundInternalizationRate; //expressed as per microsecond

	public TraffickingInfo() {
		this(0, 0, 0);
	}
	
	public TraffickingInfo(float s, float u, float b){
		//time units should be per microseconds
		secretionRate = s;
		unboundInternalizationRate = u;
		boundInternalizationRate = b;
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
	
	public void setSecretionRate(float r){
		secretionRate = r;
	}
	
	public void setUnboundIntRate(float r){
		unboundInternalizationRate = r;
	}
	
	public void getBoundIntRate(float r){
		boundInternalizationRate = r;
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
		return -1.0f;
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
	
	public String toString(){
		String s = "Traf. Info: secRate: " + secretionRate + " unboundInt: " + unboundInternalizationRate;
		s += " boundInt: " + boundInternalizationRate;
		return s;
	}
}
