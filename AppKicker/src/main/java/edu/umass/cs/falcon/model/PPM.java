package edu.umass.cs.falcon.model;

public class PPM{

	static {
		System.loadLibrary("native-ppm");
	}
	
	/**
	 * Default constructor
	 */
	public PPM(int topk) {
		init(1,6);
		setTopK(topk);
	}
	
	/**
	 * Initializes PPM for location and time clusters
	 * @param N_LOC_CLUSTERS
	 * @param N_TIME_CLUSTERS
	 */
	public native void init(int N_LOC_CLUSTERS, int N_TIME_CLUSTERS);
	
	/**
	 * Sets k: number of predictions to make
	 * @param topK
	 */
	public native void setTopK(int topK);
	
	/**
	 * Method to get top-k predictions based on current location and time
	 * @param locationCluster
	 * @param timeCluster
	 * @param topK
	 * @return 2-dimensional array with apps and their probabilities
	 */
	public native double[][] getTopPredictions(int locationCluster, int timeCluster, int topK);
	
	/**
	 * Gives probability of app to be used within next-N app 
	 * @param targetApp 
	 * @param depth N or the depth of tree to search
	 * @param locationCluster
	 * @param timeCluster
	 * @return
	 */
	public native double getAppProbability(int targetApp, int depth,int locationCluster, int timeCluster);
	
	/**
	 * Updates model with current app observation, location and time
	 * @param currentApp
	 * @param locationCluster
	 * @param timeCluster
	 */
	public native void updateModel(int currentApp, int locationCluster, int timeCluster);
	
}
