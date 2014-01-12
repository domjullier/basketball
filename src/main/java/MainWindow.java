import com.bulletphysics.demos.opengl.GLDebugDrawer;
import com.bulletphysics.demos.opengl.LWJGL;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import org.lwjgl.LWJGLException;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.googlecode.javacv.cpp.opencv_core.*;

import java.io.IOException;
import java.util.LinkedList; //fifo for speed average

public class MainWindow {

    BasicDemo demo;
	private JFrame frame;
	FrameGrabber grabber;
	JLabel lblwebcam;
	Thread thCam1;
	Thread thView;
	CvRect[] selection = new CvRect[2];
	int p1X, p1Y, p2X, p2Y;
	IplImage img1, img2, histimg;
	CvBox2D track_box = new CvBox2D();
	boolean isTracking=false;
	boolean readyToShoot=true;
	int[] trackObject = new int[2];
	private JLabel lblCam;
	private JLabel lblCam_1;
	private JTextField text_cam1;
	View3D view;
	int x, y, z;
	int px=0, py=0, pz=0;
	LinkedList<Integer> speedXFifo;
	LinkedList<Integer> speedYFifo;
	LinkedList<Integer> speedZFifo;
	
	int speedXTotal = 0;
	int speedYTotal = 0;
	int speedZTotal = 0;
	
	int speedX = 0;
	int speedY = 0;
	int speedZ = 0;
	
	public int[] getPosition()
	{	
		return new int[]{x,640-y,480-z};
	}
	
	public int getTrackObject(int id) {
		return trackObject[id];
	}

	public void setTrackObject(int trackObject, int id) {
		this.trackObject[id] = trackObject;
	}

	public boolean isTracking() {
		return isTracking;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) throws LWJGLException, IOException {


        final BasicDemo ccdDemo = new BasicDemo(LWJGL.getGL());
        ccdDemo.initPhysics();
        ccdDemo.getDynamicsWorld().setDebugDrawer(new GLDebugDrawer(LWJGL.getGL()));

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow(ccdDemo);
					window.frame.setVisible(true);
					
					window.startWebcam();
					
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});


        LWJGL.main(args, 800, 600, "Bullet Physics Demo. http://bullet.sf.net", ccdDemo);
	}

	/**
	 * Create the application.
	 */
	public MainWindow(BasicDemo demo) {
        this.demo = demo;
		initialize();
	}
	
	private void startWebcam() {
		GrabberShow gs = new GrabberShow(this, new int[]{1});
		View3D view = new View3D(this);
		
		thView = new Thread(view);
		thCam1 = new Thread(gs);
		thCam1.start();
		thView.start();
	}
	
	
	public void newFrame(IplImage newFrame, int id, int x, int y, int r)
	{
		
		if(id==0)
		{	
			this.x=x;
			this.y=y;
			this.z = r*2;
			
			demo.setPlayerPos((x/-32) +10, y, (z-300)/15);
			//add to speed fifo
			speedXFifo.add (Math.abs(x-px));
			speedXFifo.removeFirst();
			
			speedYFifo.add (Math.abs(y-py));
			speedYFifo.removeFirst();
			
			speedZFifo.add (Math.abs(z-pz));
			speedZFifo.removeFirst();
			
			px=x;
			py=y;
			pz=z;
			
			speedXTotal = 0;
			speedYTotal = 0;
			speedZTotal = 0;
			
			for(int i = 0; i<15; i++)
			{
				speedXTotal += speedXFifo.get(i);
				speedYTotal += speedYFifo.get(i);
				speedZTotal += speedZFifo.get(i);
			}
			
			speedX = speedXTotal/15;
			speedY = speedYTotal/15;
			speedZ = speedZTotal/15;
			
			
			lblwebcam.setIcon(new ImageIcon(newFrame.getBufferedImage() ));
			text_cam1.setText(Integer.toString(x) + '/' + Integer.toString(y) + '/' + Integer.toString(z));
			
			if (y>400)
			{
				readyToShoot=true;
			}
			
			//check if position above line and throw the ball
			if (z>100 && isTracking && readyToShoot) //constant height trigger
			{
				readyToShoot = false;
				
				//speed range between 0 and 20px per second
                demo.shootBox(new Vector3f(speedX, speedY, speedZ));
		
			}
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

	/**
	 * Initialize the contents of the frame.
	 */

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(0, 0, 800, 700);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		//selection = new CvRect();
		
		lblwebcam = new JLabel("Waiting for webcam...");
	
				
		lblwebcam.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				
				//markerSet=false;
				p2X=0;
				p2Y=0;
				p1X=e.getX();
				p1Y=e.getY();
				
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				
				p2X=e.getX();
				p2Y=e.getY();
								
				selection[0] = cvRect(p1X, p2Y, p2X-p1X, p1Y-p2Y);
				//selection[1] = cvRect(p1X, p2Y, p2X-p1X, p1Y-p2Y);
				isTracking=true;
				trackObject[0]=-1;
				//trackObject[1]=-1;
			}
		});
		
		
		
		lblwebcam.setBorder(new LineBorder(Color.BLACK, 2));
		lblwebcam.setForeground(Color.BLACK);
		lblwebcam.setBackground(Color.WHITE);
		lblwebcam.setBounds(12, 12, 640, 480);
		frame.getContentPane().add(lblwebcam);
		
		
		lblCam = new JLabel("Cam1");
		lblCam.setBounds(26, 539, 70, 15);
		frame.getContentPane().add(lblCam);
		
		lblCam_1 = new JLabel("");
		lblCam_1.setBounds(130, 539, 70, 15);
		frame.getContentPane().add(lblCam_1);
		
		text_cam1 = new JTextField();
		text_cam1.setBounds(26, 566, 100, 19);
		frame.getContentPane().add(text_cam1);
		text_cam1.setColumns(10);
		
		//get average speed
		speedXFifo = new LinkedList<Integer>();
		speedYFifo = new LinkedList<Integer>();
		speedZFifo = new LinkedList<Integer>();
		for(int i = 0; i<15; i++)
		{
			speedXFifo.add(0);
			speedYFifo.add(0);
			speedZFifo.add(0);
		}	

		
	}

	public CvRect getSelection(int id) {
		return selection[id];
	}
}
