//Kalman

public class KalmanFilter
{
	//initial values for the kalman filter 
    float x_est_last = 0;
    float P_last = 0; 
    
    //the noise in the system 
    float Q = 0.5f;//0.022f;
    float R = 0.6f;//0.617f;
     
    float K; 
    float P; 
    float P_pos; 
    float x_pos_est; 
    float x_est; 
    float z_measured;
    float z_real = 0.5f;

    
     
    float sum_error_kalman = 0;
    float sum_error_measure = 0;
    
    public KalmanFilter()
    {
    	x_est_last = 0; //start value
    }
    
    int update(int pos)
    {
    	//do a prediction 
        x_pos_est = x_est_last; 
        P_pos = P_last + Q; 
        
        //calculate the Kalman gain 
        K = P_pos * (1.0f/(P_pos + R));
        
        //get measurement
        z_measured = pos;
        
        //correct 
        x_est = x_pos_est + K * (z_measured - x_pos_est);  
        P = (1- K) * P_pos; 
        System.out.println(pos + " " + x_est);
        P_last = P;
        x_est_last = x_est;
    	return (int) x_est;
    }
}