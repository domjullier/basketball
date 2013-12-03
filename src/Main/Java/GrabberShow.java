

import com.googlecode.javacv.FrameGrabber;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_video.*;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvConnectedComp;
import com.googlecode.javacv.cpp.opencv_imgproc.CvHistogram;



public class GrabberShow implements Runnable
{
    //final int INTERVAL=1000;///you may use interval
    IplImage image[];
    //JLabel lblwebcam;
    FrameGrabber[] grabber;
    MainWindow detectMainref;
    boolean isTracking=false;
    
    //bispill
    IplImage[] hsv, hue, mask, backproject, histimg;
    CvHistogram[] hist;
    int[] hdims = {16};
    float hranges_arr[][] = {{0, 180}};
    int vmin = 62, vmax = 256, smin = 110;
    CvRect[] tracking_window;
    CvConnectedComp[] track_comp = new CvConnectedComp[2];
    CvBox2D[] track_box = new CvBox2D[2];
    int backproject_mode = 0;
    int select_object = 0;
    //end
    int[] cams;

    public GrabberShow(MainWindow detectMainref, int[] cams) 
    {
    	this.detectMainref = detectMainref;
    	this.cams = cams;
    }
    
    public void stop() {
        try {
        	for(int i=0; i<cams.length; i++)
        		grabber[i].stop();
		} catch (com.googlecode.javacv.FrameGrabber.Exception e) {
			e.printStackTrace();
		}
        
    }
    
   
    public void run() {
      
    	IplImage[] img1 = new IplImage[cams.length];
    	IplImage[] img2 = new IplImage[cams.length];
    	
    	hsv = new IplImage[cams.length];
    	hue = new IplImage[cams.length];
    	mask = new IplImage[cams.length];
    	backproject = new IplImage[cams.length];
    	histimg = new IplImage[cams.length];
        hist = new CvHistogram[cams.length];
    	
    	tracking_window = new CvRect[cams.length];
    	
    	grabber = new FrameGrabber[cams.length];
    	
        try {
        	for(int i=0; i<cams.length; i++) {
        		if(grabber[i]==null)
        			grabber[i] = FrameGrabber.createDefault(cams[i]);
        		
        		
            	grabber[i].setImageWidth(640);
            	grabber[i].setImageHeight(480);
                grabber[i].start();
                
                //get first picture
                
                img2[i] = grabber[i].grab();
                
        	}
        	
        	
            
            while(!detectMainref.isTracking())
            {
            	for(int i=0; i<cams.length; i++) {
	            	if (img2[i] != null)
	                	cvFlip(img2[i], img2[i], 1);
	            	
	            	
	            	
	            	//draw line
        			CvPoint lp1 = cvPointFrom32f(new CvPoint2D32f(0, 200));
        			CvPoint lp2 = cvPointFrom32f(new CvPoint2D32f(640, 200));
        			cvLine(img2[i], lp1, lp2, CvScalar.RED, 2, 8, 0 );
        			
        			detectMainref.newFrame(img2[i], i, 0, 0);
	            	img2[i] = grabber[i].grab();
            	}
            }
            
            
            while (!Thread.currentThread().isInterrupted()) {
            	
            	for(int i=0; i<cams.length; i++) {
            		img1[i] = grabber[i].grab();
            		if (img1[i] != null) {
            			cvFlip(img1[i], img1[i], 1);
                
		            //paint initial selection
                	CvPoint p1 = cvPointFrom32f(new CvPoint2D32f(detectMainref.getSelection(i).x(), detectMainref.getSelection(i).y()));
        			CvPoint p2 = cvPointFrom32f(new CvPoint2D32f(detectMainref.getSelection(i).x() + detectMainref.getSelection(i).width(), detectMainref.getSelection(i).y() + detectMainref.getSelection(i).height()));
        			//cvCircle(newFrame, p1, 50, CvScalar.GREEN, 7, CV_AA, 0);
        			cvRectangle(img1[i], p1, p2, CvScalar.BLUE, 1, 8, 0 );
		            
        			//draw line
        			CvPoint lp1 = cvPointFrom32f(new CvPoint2D32f(0, 200));
        			CvPoint lp2 = cvPointFrom32f(new CvPoint2D32f(640, 200));
        			cvLine(img1[i], lp1, lp2, CvScalar.RED, 2, 8, 0 );
        			
		        	camshift_tracking(img1[i], img2[i], detectMainref.getSelection(i), i);
		        	
		        	//System.out.println("CAM" + i + ": " + tracking_window[i].x() + " " + tracking_window[i].y());
		        	
		        	
		        	int x = tracking_window[i].x()+(tracking_window[i].width()/2);
		        	int y = tracking_window[i].y()+(tracking_window[i].height()/2);
		        	
		        	detectMainref.newFrame(img1[i], i, x, y);
		        	
		        	
		        	
		        	img2[i]=img1[i];
		        	
		        	cvCopy(img1[i], img2[i]);
            		}
                	
                }
                 //Thread.sleep(100);
            }
            
            for(int i=0; i<cams.length; i++) {
            	grabber[i].stop();
            }
             
        } catch (Exception e) {
        }
    	
    }
    
    public void camshift_tracking(IplImage img1, IplImage img2, CvRect bb, int id)
	{
    	//System.out.println(img1.cvSize());
    	if (hsv[id] == null) {
            hsv[id] = cvCreateImage(img1.cvSize(), 8, 3);
            hue[id] = cvCreateImage(img1.cvSize(), 8, 1);
            mask[id] = cvCreateImage(img1.cvSize(), 8, 1);
            backproject[id] = cvCreateImage(img1.cvSize(), 8, 1);
            hist[id] = cvCreateHist(1, hdims, CV_HIST_ARRAY, hranges_arr, 1);
            histimg[id] = cvCreateImage(img1.cvSize(), 8, 3);
            cvZero(histimg[id]);
        }
    	
    	cvCvtColor(img1, hsv[id], CV_BGR2HSV);
        
         
         
         if (detectMainref.getTrackObject(id) != 0) {

             int _vmin = vmin, _vmax = vmax;
             int bin_w;

             cvInRangeS(hsv[id], cvScalar(0, smin, Math.min(_vmin, _vmax), 0), cvScalar(180, 256, Math.max(_vmin, _vmax), 0), mask[id]);
             cvSplit(hsv[id], hue[id], null, null, null);
             IplImage[] hueArray = {hue[id]};

             if (detectMainref.getTrackObject(id) < 0) {
                 float max_val[] = {0.f};
                 cvSetImageROI(hue[id], detectMainref.getSelection(id));
                 cvSetImageROI(mask[id], detectMainref.getSelection(id));
                 cvCalcHist(hueArray, hist[id], 0, mask[id]);
                 
                 cvGetMinMaxHistValue(hist[id], new float[]{0f}, max_val, new int[]{0}, new int[]{0});
                
                 
                 cvConvertScale(hist[id].bins(), hist[id].bins(), max_val[0] != 0 ? 255.0 / max_val[0] : 0, 0);
                 cvResetImageROI(hue[id]);
                 cvResetImageROI(mask[id]);
                 tracking_window[id] = detectMainref.getSelection(id);
                 detectMainref.setTrackObject(1, id);

                 
                 cvZero(histimg[id]);
                 bin_w = histimg[id].width() / hdims[0];
                 for (int i = 0; i < hdims[0]; i++) {
                     int val = Math.round((int) (cvGetReal1D(hist[id].bins(), i) * histimg[id].height() / 255.));
                     CvScalar color = hsv2rgb(i * 180.f / hdims[0]);
                     cvRectangle(histimg[id], cvPoint(i * bin_w, histimg[id].height()), cvPoint((i + 1) * bin_w, histimg[id].height() - val), color, -1, 8, 0);
                 
                 }
             }
          
             cvCalcBackProject(hueArray, backproject[id], hist[id]);
             cvAnd(backproject[id], mask[id], backproject[id], null);

             if(track_box[id]==null)
            	 track_box[id] = new CvBox2D();
             
             if(track_comp[id]==null)
            	 track_comp[id] = new CvConnectedComp();
             
             cvCamShift(backproject[id], tracking_window[id], cvTermCriteria(CV_TERMCRIT_EPS | CV_TERMCRIT_ITER, 10, 0.1), track_comp[id], track_box[id]);

             tracking_window[id] = track_comp[id].rect();
             track_box[id].angle(-track_box[id].angle());
             

             if (backproject_mode != 0) {
                 cvCvtColor(backproject[id], image[id], CV_GRAY2BGR);
             }
             if (img1.origin() == 0) {
                 track_box[id] = track_box[id].angle(-track_box[id].angle());
                 cvEllipseBox(img1, track_box[id], CV_RGB(255, 0, 0), 3, CV_AA, 0);
             }
         }
         if (select_object > 0 && detectMainref.getSelection(id).width() > 0 && detectMainref.getSelection(id).height() > 0) {
             cvSetImageROI(image[id], detectMainref.getSelection(id));
             cvXorS(image[id], cvScalarAll(255), image[id], null);
             cvResetImageROI(image[id]);
         }
         
	}
	
	CvScalar hsv2rgb(float hue) {
        int[] rgb = new int[3];
        int p, sector;
        int[][] sector_data = {{0, 2, 1}, {1, 2, 0}, {1, 0, 2}, {2, 0, 1}, {2, 1, 0}, {0, 1, 2}};
        hue *= 0.033333333333333333333333333333333f;
        sector = (int) Math.floor(hue);
        p = Math.round(255 * (hue - sector));
        p = p ^ 1;
        int temp = 0;
        if ((sector & 1) == 1) {
            temp = 255;
        } else {
            temp = 0;
        }
        p ^= temp;

        rgb[sector_data[sector][0]] = 255;
        rgb[sector_data[sector][1]] = 0;
        rgb[sector_data[sector][2]] = p;

        return cvScalar(rgb[2], rgb[1], rgb[0], 0);
    }

 
 /*
    public class TestGrabber {
        public void main(String[] args) {
            GrabberShow gs = new GrabberShow();
            Thread th = new Thread(gs);
            th.start();
        }
    }
    */
}