/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2008 Erwin Coumans  http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

import com.bulletphysics.BulletStats;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.demos.opengl.*;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DebugDrawModes;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import org.lwjgl.LWJGLException;
import com.jogamp.opengl.util.texture.Texture;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLException;
import javax.vecmath.Color3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import java.io.*;

import static com.bulletphysics.demos.opengl.IGL.*;


public class BasicDemo extends DemoApplication {
	
	// keep the collision shapes, for deletion/cleanup
	private ObjectArrayList<CollisionShape> collisionShapes = new ObjectArrayList<CollisionShape>();
	private BroadphaseInterface broadphase;
	private CollisionDispatcher dispatcher;
	private ConstraintSolver solver;
	private DefaultCollisionConfiguration collisionConfiguration;
	private float mousePickClamping;
    private Texture basketballTexture;

    private Vector3f playerPos = new Vector3f(0,-3,-10);

	protected float ShootBoxInitialSpeed = 20f;
	
	public BasicDemo(IGL gl) {
		super(gl);
	}
	
	@Override
	public void clientMoveAndDisplay() {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		// simple dynamics world doesn't handle fixed-time-stepping
		float ms = getDeltaTimeMicroseconds();

		// step the simulation
		if (dynamicsWorld != null) {
			dynamicsWorld.stepSimulation(ms / 1000000f);
			// optional but useful: debug drawing
			dynamicsWorld.debugDrawWorld();
		}

		renderme();

		//glFlush();
		//glutSwapBuffers();
	}

	@Override
	public void displayCallback() {
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		renderme();

		// optional but useful: debug drawing to detect problems
		if (dynamicsWorld != null) {
			dynamicsWorld.debugDrawWorld();
		}

		//glFlush();
		//glutSwapBuffers();
	}

	public void initPhysics() throws IOException {

        //InputStream stream = getClass().getResourceAsStream("images/crate.png");
        //basketballTexture =   TextureIO.newTexture(stream, false, "png");

		setCameraDistance(50f);

		// collision configuration contains default setup for memory, collision setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);

		broadphase = new DbvtBroadphase();

		// the default constraint solver. For parallel processing you can use a different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
		solver = sol;
		
		// TODO: needed for SimpleDynamicsWorld
		//sol.setSolverMode(sol.getSolverMode() & ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());
		
		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);

		dynamicsWorld.setGravity(new Vector3f(0f, -10f, 0f));

		// create a few basic rigid bodies
		CollisionShape groundShape = new BoxShape(new Vector3f(50f, 50f, 50f));
		//CollisionShape groundShape = new StaticPlaneShape(new Vector3f(0, 1, 0), 50);

		collisionShapes.add(groundShape);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(0, -56, 0);

		// We can also use DemoApplication::localCreateRigidBody, but for clarity it is provided here:
		{
			float mass = 0f;

			// rigidbody is dynamic if and only if mass is non zero, otherwise static
			boolean isDynamic = (mass != 0f);

			Vector3f localInertia = new Vector3f(0, 0, 0);
			if (isDynamic) {
				groundShape.calculateLocalInertia(mass, localInertia);
			}

			// using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
			DefaultMotionState myMotionState = new DefaultMotionState(groundTransform);
			RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, groundShape, localInertia);
			RigidBody body = new RigidBody(rbInfo);

			// add the body to the dynamics world
			dynamicsWorld.addRigidBody(body);
		}

		{
			// create a few dynamic rigidbodies
			// Re-using the same collision is better for memory usage and performance

			//SphereShape colShape = new SphereShape(2);
			//CollisionShape colShape = new SphereShape(1f);
			//collisionShapes.add(colShape);

			// Create Dynamic Objects
			Transform startTransform = new Transform();
			startTransform.setIdentity();

			float mass = 0.5f;

			// rigidbody is dynamic if and only if mass is non zero, otherwise static
			boolean isDynamic = (mass != 0f);

			Vector3f localInertia = new Vector3f(0, 0, 0);
			if (isDynamic) {
				//colShape.calculateLocalInertia(mass, localInertia);
			}

			
			startTransform.origin.set(
					30,
					10,
					10);

			// using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects
			DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
			//RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, myMotionState, colShape, localInertia);
			//RigidBody body = new RigidBody(rbInfo);
			//body.setActivationState(RigidBody.ISLAND_SLEEPING);

			//dynamicsWorld.addRigidBody(body);
			//body.setActivationState(RigidBody.ISLAND_SLEEPING);
			
		}

        /*CollisionShape ballShape = new SphereShape(1);
        //collisionShapes.add(ballShape);
        Transform ballTransform = new Transform();
        ballTransform.setIdentity();
        ballTransform.origin.set(playerPos.x,playerPos.y, playerPos.z);
        dynamicsWorld.addRigidBody(new RigidBody(new RigidBodyConstructionInfo(0.0f,new DefaultMotionState(ballTransform),ballShape,new Vector3f(0, 0, 0))));
*/

        CollisionShape basketShape = new BoxShape(new Vector3f(1.0f, 1.0f,5.0f));
        collisionShapes.add(basketShape);
        Transform basketTransform = new Transform();
        basketTransform.setIdentity();
        basketTransform.origin.set(4, 5, 8);
        DefaultMotionState basketMotionState = new DefaultMotionState(basketTransform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(0.0f, basketMotionState,basketShape,new Vector3f(0, 0, 0));
        RigidBody basketBody = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(basketBody);


        CollisionShape basketShape2 = new BoxShape(new Vector3f(5.0f, 1.0f,1.0f));
        collisionShapes.add(basketShape2);
        Transform basketTransform2 = new Transform();
        basketTransform2.setIdentity();
        basketTransform2.origin.set(0, 5, 4);
        DefaultMotionState basketMotionState2 = new DefaultMotionState(basketTransform2);
        RigidBody basketBody2 = new RigidBody(new RigidBodyConstructionInfo(0.0f, basketMotionState2,basketShape2,new Vector3f(0, 0, 0)));
        dynamicsWorld.addRigidBody(basketBody2);

        CollisionShape basketShape3 = new BoxShape(new Vector3f(5.0f, 5.0f,1.0f));
        collisionShapes.add(basketShape3);
        Transform basketTransform3 = new Transform();
        basketTransform3.setIdentity();
        basketTransform3.origin.set(0, 5, 12);
        DefaultMotionState basketMotionState3 = new DefaultMotionState(basketTransform3);
        RigidBody basketBody3 = new RigidBody(new RigidBodyConstructionInfo(0.0f, basketMotionState3,basketShape3,new Vector3f(0, 0, 0)));
        dynamicsWorld.addRigidBody(basketBody3);


        CollisionShape basketShape4 = new BoxShape(new Vector3f(1.0f, 1.0f,5.0f));
        collisionShapes.add(basketShape4);
        Transform basketTransform4 = new Transform();
        basketTransform4.setIdentity();
        basketTransform4.origin.set(-4, 5, 8);
        DefaultMotionState basketMotionState4 = new DefaultMotionState(basketTransform4);
        RigidBodyConstructionInfo rbInfo4 = new RigidBodyConstructionInfo(0.0f, basketMotionState4,basketShape4,new Vector3f(0, 0, 0));
        RigidBody basketBody4 = new RigidBody(rbInfo4);
        dynamicsWorld.addRigidBody(basketBody4);



		clientResetScene();
	}
	
	@Override
	public void mouseFunc(int button, int state, int x, int y) {
		//printf("button %i, state %i, x=%i,y=%i\n",button,state,x,y);
		//button 0, state 0 means left mouse down
		
		if(button == 2 && state == 0)
		{
		//Vector3f rayTo = new Vector3f(getRayTo(x, y));
		
		Vector3f rayTo = new Vector3f(0, 18, 5);
        //Vector3f rayTo = new Vector3f(0, 11, 0);
		
		shootBox(rayTo);
		}
		
	}
	
	
	
	@Override
	public Vector3f getRayTo(int x, int y) {
		float top = 1f;
		float bottom = -1f;
		float nearPlane = 1f;
		float tanFov = (top - bottom) * 0.5f / nearPlane;
		float fov = 2f * (float) Math.atan(tanFov);

		Vector3f rayFrom = new Vector3f(0,1,1);
		Vector3f rayForward = new Vector3f(0,1,1);
		rayForward.sub(getCameraTargetPosition(), getCameraPosition());
		rayForward.normalize();
		float farPlane = 10000f;
		rayForward.scale(farPlane);

		Vector3f rightOffset = new Vector3f();
		Vector3f vertical = new Vector3f(cameraUp);

		Vector3f hor = new Vector3f();
		// TODO: check: hor = rayForward.cross(vertical);
		hor.cross(rayForward, vertical);
		hor.normalize();
		// TODO: check: vertical = hor.cross(rayForward);
		vertical.cross(hor, rayForward);
		vertical.normalize();

		float tanfov = (float) Math.tan(0.5f * fov);
		
		float aspect = glutScreenHeight / (float)glutScreenWidth;
		
		hor.scale(2f * farPlane * tanfov);
		vertical.scale(2f * farPlane * tanfov);
		
		if (aspect < 1f) {
			hor.scale(1f / aspect);
		}
		else {
			vertical.scale(aspect);
		}
		
		Vector3f rayToCenter = new Vector3f();
		rayToCenter.add(rayFrom, rayForward);
		Vector3f dHor = new Vector3f(hor);
		dHor.scale(1f / (float) glutScreenWidth);
		Vector3f dVert = new Vector3f(vertical);
		dVert.scale(1.f / (float) glutScreenHeight);

		Vector3f tmp1 = new Vector3f();
		Vector3f tmp2 = new Vector3f();
		tmp1.scale(0.5f, hor);
		tmp2.scale(0.5f, vertical);

		Vector3f rayTo = new Vector3f();
		rayTo.sub(rayToCenter, tmp1);
		rayTo.add(tmp2);

		tmp1.scale(2, dHor);
		tmp2.scale(2, dVert);

		rayTo.add(tmp1);
		rayTo.sub(tmp2);
		return rayTo;
	}

	public void shootBox(Vector3f destination) {
		if (dynamicsWorld != null) {
			float mass = 10f;
			Transform startTransform = new Transform();
			startTransform.setIdentity();
			//Vector3f camPos = new Vector3f(getCameraPosition());
			
			//Vector3f playerPos = new Vector3f(-3,-3,-3);
			startTransform.origin.set(playerPos);

			if (shootBoxShape == null) {
				//#define TEST_UNIFORM_SCALING_SHAPE 1
				//#ifdef TEST_UNIFORM_SCALING_SHAPE
				//btConvexShape* childShape = new btBoxShape(btVector3(1.f,1.f,1.f));
				//m_shootBoxShape = new btUniformScalingShape(childShape,0.5f);
				//#else
				shootBoxShape = new SphereShape(2);
				//#endif//
			}

			RigidBody body = this.localCreateRigidBody(mass, startTransform, shootBoxShape);

			Vector3f linVel = new Vector3f(destination.x - playerPos.x, destination.y - playerPos.y, destination.z - playerPos.z);
			
			
			linVel.normalize();
			linVel.scale(ShootBoxInitialSpeed);

			Transform worldTrans = body.getWorldTransform(new Transform());
			worldTrans.origin.set(playerPos);
			worldTrans.setRotation(new Quat4f(0f, 0f, 0f, 1f));
			body.setWorldTransform(worldTrans);
			
			body.setLinearVelocity(linVel);
			body.setAngularVelocity(new Vector3f(0f, 0f, 0f));

			body.setCcdMotionThreshold(1f);
			body.setCcdSweptSphereRadius(0.2f);

		}
	}
	
	/*
	@Override
	public void shootBox(Vector3f destination) {
		if (dynamicsWorld != null) {
			float mass = 10f;
			Transform startTransform = new Transform();
			startTransform.setIdentity();
			Vector3f camPos = new Vector3f(getCameraPosition());
			startTransform.origin.set(camPos);

			if (shootBoxShape == null) {
				//#define TEST_UNIFORM_SCALING_SHAPE 1
				//#ifdef TEST_UNIFORM_SCALING_SHAPE
				//btConvexShape* childShape = new btBoxShape(btVector3(1.f,1.f,1.f));
				//m_shootBoxShape = new btUniformScalingShape(childShape,0.5f);
				//#else
				shootBoxShape = new SphereShape(2);
				//#endif//
			}

			RigidBody body = this.localCreateRigidBody(mass, startTransform, shootBoxShape);

			//Vector3f linVel = new Vector3f(destination.x - camPos.x, destination.y - camPos.y, destination.z - camPos.z);
			Vector3f linVel = new Vector3f(0,1,1);
			linVel.normalize();
			linVel.scale(ShootBoxInitialSpeed);

			Transform worldTrans = body.getWorldTransform(new Transform());
			worldTrans.origin.set(camPos);
			worldTrans.setRotation(new Quat4f(0f, 0f, 0f, 1f));
			body.setWorldTransform(worldTrans);
			
			body.setLinearVelocity(linVel);
			body.setAngularVelocity(new Vector3f(0f, 0f, 0f));

			body.setCcdMotionThreshold(1f);
			body.setCcdSweptSphereRadius(0.2f);
		}
	}
	*/
	
	private final Transform m = new Transform();
	private Vector3f wireColor = new Vector3f();
	protected Color3f TEXT_COLOR = new Color3f(0f, 0f, 0f);
	private StringBuilder buf = new StringBuilder();
	
	@Override
	public void renderme() {

        float[] rgba = {1f, 1f, 1f};

        gl.glPushMatrix();
            gl.glColor3f(1,0,0);
            gl.glTranslatef(playerPos.x,playerPos.y, playerPos.z);
            gl.drawSphere(2,32,32);
        gl.glPopMatrix();

		updateCamera();

		if (dynamicsWorld != null) {
			int numObjects = dynamicsWorld.getNumCollisionObjects();
			wireColor.set(1f, 0f, 0f);
			for (int i = 0; i < numObjects; i++) {
				CollisionObject colObj = dynamicsWorld.getCollisionObjectArray().getQuick(i);
				RigidBody body = RigidBody.upcast(colObj);

				if (body != null && body.getMotionState() != null) {
					DefaultMotionState myMotionState = (DefaultMotionState) body.getMotionState();
					m.set(myMotionState.graphicsWorldTrans);
				}
				else {
					colObj.getWorldTransform(m);
				}

				wireColor.set(1f, 1f, 0.5f); // wants deactivation
				if ((i & 1) != 0) {
					wireColor.set(0f, 0f, 1f);
				}

				// color differently for active, sleeping, wantsdeactivation states
				if (colObj.getActivationState() == 1) // active
				{
					if ((i & 1) != 0) {
						//wireColor.add(new Vector3f(1f, 0f, 0f));
						wireColor.x += 1f;
					}
					else {
						//wireColor.add(new Vector3f(0.5f, 0f, 0f));
						wireColor.x += 0.5f;
					}
				}
				if (colObj.getActivationState() == 2) // ISLAND_SLEEPING
				{
					if ((i & 1) != 0) {
						//wireColor.add(new Vector3f(0f, 1f, 0f));
						wireColor.y += 1f;
					}
					else {
						//wireColor.add(new Vector3f(0f, 0.5f, 0f));
						wireColor.y += 0.5f;
					}
				}

				GLShapeDrawer.drawOpenGL(gl, m, colObj.getCollisionShape(), wireColor, getDebugMode());
			}

			float xOffset = 10f;
			float yStart = 20f;
			float yIncr = 20f;

			gl.glDisable(GL_LIGHTING);
			gl.glColor3f(0f, 0f, 0f);

			if ((debugMode & DebugDrawModes.NO_HELP_TEXT) == 0) {
				setOrthographicProjection();

				yStart = showProfileInfo(xOffset, yStart, yIncr);

//					#ifdef USE_QUICKPROF
//					if ( getDebugMode() & btIDebugDraw::DBG_ProfileTimings)
//					{
//						static int counter = 0;
//						counter++;
//						std::map<std::string, hidden::ProfileBlock*>::iterator iter;
//						for (iter = btProfiler::mProfileBlocks.begin(); iter != btProfiler::mProfileBlocks.end(); ++iter)
//						{
//							char blockTime[128];
//							sprintf(blockTime, "%s: %lf",&((*iter).first[0]),btProfiler::getBlockTime((*iter).first, btProfiler::BLOCK_CYCLE_SECONDS));//BLOCK_TOTAL_PERCENT));
//							glRasterPos3f(xOffset,yStart,0);
//							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),blockTime);
//							yStart += yIncr;
//
//						}
//					}
//					#endif //USE_QUICKPROF


				String s = "mouse to interact";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "LMB=drag, RMB=shoot box, MIDDLE=apply impulse";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;
				
				s = "space to reset";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "cursor keys and z,x to navigate";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "i to toggle simulation, s single step";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "q to quit";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = ". to shoot box or trimesh (MovingConcaveDemo)";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// not yet hooked up again after refactoring...

				s = "d to toggle deactivation";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "g to toggle mesh animation (ConcaveDemo)";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "e to spawn new body (GenericJointDemo)";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "h to toggle help text";
				//drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//buf = "p to toggle profiling (+results to file)";
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//bool useBulletLCP = !(getDebugMode() & btIDebugDraw::DBG_DisableBulletLCP);
				//bool useCCD = (getDebugMode() & btIDebugDraw::DBG_EnableCCD);
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"1 CCD mode (adhoc) = %i",useCCD);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//glRasterPos3f(xOffset, yStart, 0);
				//buf = String.format(%10.2f", ShootBoxInitialSpeed);
				buf.setLength(0);
				buf.append("+- shooting speed = ");
				FastFormat.append(buf, ShootBoxInitialSpeed);
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//#ifdef SHOW_NUM_DEEP_PENETRATIONS
				buf.setLength(0);
				buf.append("gNumDeepPenetrationChecks = ");
				FastFormat.append(buf, BulletStats.gNumDeepPenetrationChecks);
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				buf.setLength(0);
				buf.append("gNumGjkChecks = ");
				FastFormat.append(buf, BulletStats.gNumGjkChecks);
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				buf.setLength(0);
				buf.append("gNumSplitImpulseRecoveries = ");
				FastFormat.append(buf, BulletStats.gNumSplitImpulseRecoveries);
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;
				
				//buf = String.format("gNumAlignedAllocs = %d", BulletGlobals.gNumAlignedAllocs);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("gNumAlignedFree= %d", BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("# alloc-free = %d", BulletGlobals.gNumAlignedAllocs - BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//enable BT_DEBUG_MEMORY_ALLOCATIONS define in Bullet/src/LinearMath/btAlignedAllocator.h for memory leak detection
				//#ifdef BT_DEBUG_MEMORY_ALLOCATIONS
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"gTotalBytesAlignedAllocs = %d",gTotalBytesAlignedAllocs);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;
				//#endif //BT_DEBUG_MEMORY_ALLOCATIONS

				/*
				if (getDynamicsWorld() != null) {
					buf.setLength(0);
					buf.append("# objects = ");
					FastFormat.append(buf, getDynamicsWorld().getNumCollisionObjects());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;
					
					buf.setLength(0);
					buf.append("# pairs = ");
					FastFormat.append(buf, getDynamicsWorld().getBroadphase().getOverlappingPairCache().getNumOverlappingPairs());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;

				}
				*/
				//#endif //SHOW_NUM_DEEP_PENETRATIONS

				// JAVA NOTE: added
				int free = (int)Runtime.getRuntime().freeMemory();
				int total = (int)Runtime.getRuntime().totalMemory();
				buf.setLength(0);
				buf.append("heap = ");
				FastFormat.append(buf, (float)(total - free) / (1024*1024));
				buf.append(" / ");
				FastFormat.append(buf, (float)(total) / (1024*1024));
				buf.append(" MB");
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;
				
				resetPerspectiveProjection();
			}

			gl.glEnable(GL_LIGHTING);
		}
		
		updateCamera();
	}
	
	public static void main(String[] args) throws LWJGLException, IOException {
		BasicDemo ccdDemo = new BasicDemo(LWJGL.getGL());
		ccdDemo.initPhysics();
		ccdDemo.getDynamicsWorld().setDebugDrawer(new GLDebugDrawer(LWJGL.getGL()));

		LWJGL.main(args, 800, 600, "Bullet Physics Demo. http://bullet.sf.net", ccdDemo);
	}
	
}
