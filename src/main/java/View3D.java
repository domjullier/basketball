import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Random;

import javax.swing.*;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;

import static java.awt.event.KeyEvent.*;
import static javax.media.opengl.GL.*;  // GL constants
import static javax.media.opengl.GL2.*; // GL2 constants



@SuppressWarnings("serial")
public class View3D implements GLEventListener, KeyListener, Runnable {
   private static String TITLE = "ddddddddd";
   private static final int CANVAS_WIDTH = 320;  // width of the drawable
   private static final int CANVAS_HEIGHT = 240; // height of the drawable
   private static final int FPS = 60; // animator's target frames per second
   private float posX = 2.5f;
   private float posY = -1.5f; 
   private float posZ = 2.0f;
   
   private float ballX = 2.5f;
   private float ballY = 0.0f; 
   private float ballZ = 0.0f;
   
   private float randX = 2.5f;
   private float randY = 2.5f; 
   private float randZ = 2.5f;
   
   private Random rand = new Random();
   
   
   
   private GLUquadric qobj0;   
   MainWindow detectMainref;  
   private GLU glu;  
   // Texture
   private Texture texture;
   private Texture ballTexture;
   private Texture randomBallTexture;
   
   private float textureTop, textureBottom, textureLeft, textureRight;
   
   
   public View3D(MainWindow detectMainref)
   {
	   this.detectMainref=detectMainref;
   }
   
   public void initialize()
   {
	   GLCanvas canvas = new GLCanvas();
       canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
       View3D renderer = new View3D(detectMainref);
       canvas.addGLEventListener(renderer);             
       canvas.addKeyListener(renderer);         
       canvas.setFocusable(true);
       canvas.requestFocus();

       // Create a animator that drives canvas' display() at the specified FPS. 
       final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
       
       // Create the top-level container
       final JFrame frame = new JFrame(); // Swing's JFrame or AWT's Frame
       frame.getContentPane().add(canvas);
       
       frame.addWindowListener(new WindowAdapter() {
          @Override 
          public void windowClosing(WindowEvent e) {
             // Use a dedicate thread to run the stop() to ensure that the
             // animator stops before program exits.
             new Thread() {
                @Override 
                public void run() {
                   if (animator.isStarted()) animator.stop();
                   System.exit(0);
                }
             }.start();
          }
       });
       frame.setTitle(TITLE);
       frame.pack();
       frame.setVisible(true);
       animator.start(); // start the animation loop  
	 
   }
   
   public void setNewPosition(int x, int y, int z)
   {
	   System.out.println(x + " " + y + "" + z);
	   //x: 0 do 640
	   //y: 0 do 640
	   //z: 0 do 480
   
   }
   

   public void init(GLAutoDrawable drawable) {
      GL2 gl = drawable.getGL().getGL2();      // get the OpenGL graphics context
      glu = new GLU();                         // get GL Utilities
      gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
      gl.glClearDepth(1.0f);      // set clear depth value to farthest
      gl.glEnable(GL_DEPTH_TEST); // enables depth testing
      
      qobj0 = glu.gluNewQuadric();
      glu.gluQuadricDrawStyle( qobj0, GLU.GLU_FILL );
      glu.gluQuadricNormals( qobj0, GLU.GLU_SMOOTH );
      glu.gluQuadricTexture(qobj0,true);


      // Load texture from image
      try {
         // Create a OpenGL Texture object from (URL, mipmap, file suffix)
         // Use URL so that can read from JAR and disk file.
         texture = TextureIO.newTexture(
               getClass().getClassLoader().getResource("images/glass.png"),
               false, ".png");
         
         ballTexture = TextureIO.newTexture(
                 getClass().getClassLoader().getResource("images/cube.bmp"), // relative to project root 
                 false,".bmp");
         
         randomBallTexture = TextureIO.newTexture(
                 getClass().getClassLoader().getResource("images/ball.bmp"), // relative to project root 
                 false,".bmp");

         // Use linear filter for texture if image is larger than the original texture
         gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
         // Use linear filter for texture if image is smaller than the original texture
         gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

         // Texture image flips vertically. Shall use TextureCoords class to retrieve
         // the top, bottom, left and right coordinates, instead of using 0.0f and 1.0f.
         TextureCoords textureCoords = texture.getImageTexCoords();
         textureTop = textureCoords.top();
         textureBottom = textureCoords.bottom();
         textureLeft = textureCoords.left();
         textureRight = textureCoords.right();
      } catch (GLException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Call-back handler for window re-size event. Also called when the drawable is 
    * first set to visible.
    */
   
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context

      if (height == 0) height = 1;   // prevent divide by zero
      float aspect = (float)width / height;

      // Set the view port (display area) to cover the entire window
      gl.glViewport(0, 0, width, height);

      // Setup perspective projection, with aspect ratio matches viewport
      gl.glMatrixMode(GL_PROJECTION);  // choose projection matrix
      gl.glLoadIdentity();             // reset projection matrix
      glu.gluPerspective(45.0, aspect, 0.1, 100.0); // fovy, aspect, zNear, zFar

      // Enable the model-view transform
      gl.glMatrixMode(GL_MODELVIEW);
      gl.glLoadIdentity(); // reset
      

   }

   /**
    * Called back by the animator to perform rendering.
    */
   
   public void display(GLAutoDrawable drawable) {
	   
	   
	  int[] pos = detectMainref.getPosition();
	  
	  this.ballX =(float) (pos[0]/640.0)*5;
	  this.ballY =(float) (pos[1]/640.0)*5;
	  this.ballZ =(float) (pos[2]/480.0)*5;
	 
	  
	  System.out.println("x: " + this.ballX + " " + "y: " + this.ballY + " " + "z: " + this.ballZ);
	  
	  float fact1= (this.ballX - this.randX)*(this.ballX - this.randX);
	  float fact2= (this.ballY - this.randY)*(this.ballY - this.randY);
	  float fact3= (this.ballZ - this.randZ)*(this.ballY - this.randY);
	  
	  
	  
	  if(Math.sqrt(fact1 + fact2 + fact3)<0.5){
		  this.randX  =  rand.nextFloat() * 5.f; 
		  this.randY  =  rand.nextFloat() * 5.f;
		  this.randZ  =  rand.nextFloat() * 5.f;
	  }
		  
	  
	  //System.out.println("y: " + this.ballY); 
	  //System.out.println("z: " + this.ballZ);
	  
	   
      GL2 gl = drawable.getGL().getGL2();  // get the OpenGL 2 graphics context
      gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear color and depth buffers

      // ------ Render a Cube with texture ------
      gl.glLoadIdentity();  // reset the model-view matrix
      glu.gluLookAt(posX, posY,1,posX,posY + 2,1,0,0,1);
      
       
      texture.enable(gl);  // same as gl.glEnable(texture.getTarget());

      texture.bind(gl);  
     
     gl.glBegin(GL_QUADS);
   //   gl.glPolygonMode(gl.GL_FRONT_AND_BACK,gl.GL_FILL);
      gl.glTexCoord2f(textureLeft, textureBottom);
      gl.glVertex3f(0.0f, 0.0f, 0.0f);
      gl.glTexCoord2f(textureRight,textureBottom);
      gl.glVertex3f(5.0f, 0.0f, 0.0f);
      gl.glTexCoord2f(textureRight, textureTop);
      gl.glVertex3f(5.0f, 5.0f, 0.0f);
      gl.glTexCoord2f(textureLeft,textureTop);
      gl.glVertex3f(0.0f, 5.0f, 0.0f);
      
     // gl.glPolygonMode(gl.GL_FRONT_AND_BACK,gl.GL_LINES);     
      gl.glEnd();
      
      gl.glPushMatrix();
      	ballTexture.bind(gl);
      	gl.glTranslatef(this.ballX,this.ballY,this.ballZ);
      	glu.gluSphere( qobj0, 0.4f, 10, 10);
      gl.glPopMatrix();
      
      gl.glPushMatrix();
        randomBallTexture.bind(gl);
      	gl.glTranslatef(this.randX,this.randY,this.randZ);
      	glu.gluSphere( qobj0, 0.4f, 10, 10);
      gl.glPopMatrix();
      
      texture.disable(gl);
      
      
      gl.glBegin(GL_LINES);
      gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GLU.GLU_LINE);  
      gl.glLineWidth(5.0f);
      gl.glVertex3f(0.0f, 0.0f, 0.0f);
      gl.glVertex3f(0.0f, 0.0f, 5.0f);
      
      gl.glVertex3f(0.0f, 0.0f, 5.0f);
      gl.glVertex3f(5.0f, 0.0f, 5.0f);
      
      gl.glVertex3f(5.0f, 0.0f, 5.0f);
      gl.glVertex3f(5.0f, 0.0f, 0.0f);
      //----------------------
      gl.glVertex3f(0.0f, 5.0f, 0.0f);
      gl.glVertex3f(0.0f, 5.0f, 5.0f);
      
      gl.glVertex3f(0.0f, 5.0f, 5.0f);
      gl.glVertex3f(5.0f, 5.0f, 5.0f);
      
      gl.glVertex3f(5.0f, 5.0f, 5.0f);
      gl.glVertex3f(5.0f, 5.0f, 0.0f);
      //----------------------
      gl.glVertex3f(0.0f, 0.0f, 5.0f);
      gl.glVertex3f(0.0f, 5.0f, 5.0f);
      
      gl.glVertex3f(5.0f, 0.0f, 5.0f);
      gl.glVertex3f(5.0f, 5.0f, 5.0f);
         
      gl.glEnd();
      
   }

   /** 
    * Called back before the OpenGL context is destroyed. Release resource such as buffers. 
    */
   
   public void dispose(GLAutoDrawable drawable) { }

public void keyPressed(KeyEvent e) {
	 switch (e.getKeyCode()) {
	 	case VK_LEFT:  
	 		System.out.println(posX+":"+posY);
	 	//	if(this.posX>=0.0f)
	 			this.posX-=0.2f;
	 		break;
	 	case VK_RIGHT: 
	 		System.out.println(posX+":"+posY);
	 		//if(this.posX<=5.0f)
	 			this.posX+=0.2f;
	 		break;
	 	case VK_UP:
	 		System.out.println(posX+":"+posY);
	 	//	if(this.posY<=5.0f)
	 			this.posY+=0.2f;
	 		break;
	 	case VK_DOWN:
	 		System.out.println(posX+":"+posY);
	 	//	if(this.posY>=0.0f)
	 			this.posY-=0.2f;
	 		break;
	 	
	 	case 'I':
	 		System.out.println(posX+":"+posY);
	 			this.posZ+=0.2f;
	 		break;
	 		
	 	case 'K':
	 		System.out.println(posX+":"+posY);
	 			this.posZ-=0.2f;
	 		break;
	 		
	 }
	 
	 int[] result = detectMainref.getPosition();
	 
	 System.out.println( result[0] + " " + result[1] + " "  + result[2]); 
	
}

public void keyReleased(KeyEvent e) {
	// TODO Auto-generated method stub
	
}

public void keyTyped(KeyEvent e) {
	// TODO Auto-generated method stub
	
}


public void run() {
	initialize();	
}
   
   
}
