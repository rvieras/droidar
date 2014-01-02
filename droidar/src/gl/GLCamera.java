package gl;

import geo.GeoObj;

import javax.microedition.khronos.opengles.GL10;

import system.EventManager;
import util.Calculus;
import util.HasDebugInformation;
import util.Log;
import util.Vec;
import worlddata.MoveComp;
import worlddata.Updateable;
import actions.ActionUseCameraAngles2;
import android.location.Location;
import android.opengl.Matrix;

/**
 * This is the virtual camera needed to display a virtual world. The 3 important
 * properties you might want to change manually are its position, rotation and
 * offset. Do this via {@link GLCamera#setNewPosition(Vec)},
 * {@link GLCamera#setNewRotation(Vec)} and {@link GLCamera#setNewOffset(Vec)}
 * 
 * 
 * Notes: mRotationMatrix y is always the angle from floor to top (rotation
 * around green achsis counterclockwise) and x always the clockwise rotation
 * (like when you lean to the side on a motorcycle) angle of the camera.
 * 
 * to move from the green to the red axis (clockwise) you would have to add 90
 * degree.
 * 
 * @author Spobo
 * 
 */
public class GLCamera implements Updateable, HasDebugInformation, Renderable,
		HasPosition, HasRotation, GLCamRotationController {

	private static final String LOG_TAG = "GLCamera";


	private Vec mOffset = null;
	private Vec mNewOffset = new Vec(0, 0, 0);
	private Vec mPosition = new Vec(0, 0, 0);
	private Vec mRotationVec = new Vec(0, 0, 0);

	private boolean mSensorInputEnabled = true;
	private float[] mRotationMatrix = Calculus.createIdentityMatrix();
	private final Object mRotMatrLock = new Object();
	private int mMatrixOffset = 0;
	private final float[] mInvRotMatrix = Calculus.createIdentityMatrix();
	private float[] mInitDir = new float[4];
	private final float[] mRotDirection = new float[4];
	private final MoveComp mMover = new MoveComp(3);


	/**
	 * use a {@link ActionUseCameraAngles2} instead
	 * 
	 * The order is z,x,y achses.
	 * 
	 * The camera rotation angles (positive and COUNTERCLOCKWISE !!) extracted
	 * from the rotation matrix. These values will only be calculated if an
	 * angleUpdateListener is set or {@link GLCamera#forceAngleCalculation} is
	 * set to true
	 */
	@Deprecated
	private final float[] cameraAnglesInDegree = new float[3];
	@Deprecated
	public Vec myNewRotationVec;


	/**
	 * Default constructor.
	 */
	public GLCamera() {
	}

	/**
	 * Constructor.
	 * 
	 * @param initialCameraPosition
	 *            The {@link Vec} stating the starting position of the camera.
	 */
	public GLCamera(Vec initialCameraPosition) {
		setNewPosition(initialCameraPosition);
	}

	@Override
	public boolean update(float timeDelta, Updateable parent) {

		if ((mRotationVec != null) && (myNewRotationVec != null)) {
			Vec.morphToNewAngleVec(mRotationVec, myNewRotationVec, timeDelta);
		}

		if ((mOffset != null) && (mNewOffset != null)) {
			Vec.morphToNewVec(mOffset, mNewOffset, timeDelta);
		}

		if (mPosition != null) {
			mMover.update(timeDelta, this);
		}

		return true;
	}

	@Override
	public Vec getRotation() {
		return mRotationVec;
	}

	@Override
	@Deprecated
	public void setRotation(Vec rotation) {
		if (mRotationVec == null) {
			mRotationVec = rotation;
		} else {
			mRotationVec.setToVec(rotation);
		}
	}

	public void setNewPosition(Vec cameraPosition) {
		if (mPosition == null) {
			mPosition = new Vec();
		}
		mMover.mTargetPos = cameraPosition;
	}

	/**
	 * x positive means east of zero pos (latitude direction) <br>
	 * y positive means north of zero pos (longitude direction) <br>
	 * z the height of the camera
	 * 
	 * @return the {@link Vec} (x,y,z)
	 */
	public Vec getMyNewPosition() {
		return mMover.mTargetPos;
	}

	public void setNewCameraOffset(Vec newCameraOffset) {
		if (newCameraOffset != null) {
			if (mNewOffset == null) {
				mNewOffset = new Vec(newCameraOffset);
				if (mOffset == null) {
					mOffset = new Vec();
				}
			} else {
				mNewOffset.setToVec(newCameraOffset);
			}
		}
	}

	@Deprecated
	public void setNewRotation(Vec cameraRotation) {
		if (cameraRotation != null) {
			if (myNewRotationVec == null) {
				myNewRotationVec = new Vec(cameraRotation);
			} else {
				myNewRotationVec.setToVec(cameraRotation);
			}
		}
	}

	/**
	 * @param rayPosition
	 *            the vector where the ray pos will be stored in, so pass a
	 *            vector here that can be overwritten. Normally this value will
	 *            be the same as {@link GLCamera#mPosition} but if a marker is
	 *            used to move the {@link GLCamera} the translation will be
	 *            contained in the matrix as well and therefore the rayPosition
	 *            will be this translation in relation to the marker
	 * @param rayDirection
	 *            the vector where the ray direction will be stored in, so pass
	 *            a vector here that can be overwritten (don't pass null!)
	 * @param x
	 *            the horizontal screen-coordinates (from 0 to screen-width)
	 * @param y
	 *            the vertical screen-coordinates (from 0 to screen-height).
	 *            Just pass the value you get from the Android onClick event
	 */
	public void getPickingRay(Vec rayPosition, Vec rayDirection, float x,
			float y) {

		if (rayDirection == null) {
			Log.e(LOG_TAG, "Passed direction vector object was null");
			return;
		}

		// convert to opengl screen coords:
		x = (x - GLRenderer.halfWidth) / GLRenderer.halfWidth;
		y = (GLRenderer.height - y - GLRenderer.halfHeight)
				/ GLRenderer.halfHeight;

		Matrix.invertM(mInvRotMatrix, 0, mRotationMatrix, mMatrixOffset);

		if (rayPosition != null) {
			float[] rayPos = new float[4];
			float[] initPos = { 0.0f, 0.0f, 0.0f, 1.0f };
			Matrix.multiplyMV(rayPos, 0, mInvRotMatrix, 0, initPos, 0);
			rayPosition.x = rayPos[0];
			rayPosition.y = rayPos[1];
			rayPosition.z = rayPos[2];
			if (mPosition != null) {
				rayPosition.add(mPosition);
			}
		}
		float[] rayDir = new float[4];
		float[] initDir = { x * GLRenderer.nearHeight * GLRenderer.aspectRatio,
				y * GLRenderer.nearHeight, -GLRenderer.minViewDistance, 0.0f };
		Matrix.multiplyMV(rayDir, 0, mInvRotMatrix, 0, initDir, 0);
		rayDirection.x = rayDir[0];
		rayDirection.y = rayDir[1];
		rayDirection.z = rayDir[2];
	}

	/**
	 * not jet ready for use
	 * 
	 * @param virtualWorldPosition
	 * @return
	 */
	@Deprecated
	public float[] getScreenCoordinatesFor(Vec virtualWorldPosition) {
		float[] rayPos = new float[4];
		float[] initPos = { virtualWorldPosition.x, virtualWorldPosition.y,
				virtualWorldPosition.z, 1.0f };
		Matrix.multiplyMV(rayPos, 0, mRotationMatrix, mMatrixOffset, initPos, 0);
		// TODO
		return rayPos;
	}

	public int getMatrixOffset() {
		return mMatrixOffset;
	}

	/**
	 * "Ground" means the plane where z is 0
	 * 
	 * Nearly the same code as
	 * {@link GLCamera#getPickingRay(Vec, Vec, float, float)} just a little bit
	 * optimized
	 * 
	 * @return the position in the virtual world in the xy plane (so z is 0)
	 *         where the camera is looking at
	 */
	public Vec getPositionOnGroundWhereTheCameraIsLookingAt() {
		/*
		 * This is an optimized version of the getPickingRay method. The good
		 * readable code would look like this:
		 * 
		 * Vec pos = new Vec(); Vec dir = new Vec();
		 * 
		 * camera.getPickingRay(pos, dir, GLRenderer.halfWidth,
		 * GLRenderer.halfHeight);
		 * 
		 * now the calculation where the direction vec hits the ground plane.
		 * can be reduced to intersection of two lines where only the z values
		 * of start and direction are different
		 * 
		 * when you break down the intersection of two lines with nearly the
		 * same direction vectors and nearly the same start vectors then you get
		 * this:
		 * 
		 * dir.mult(-pos.z / dir.z);
		 * 
		 * dir.add(pos);
		 * 
		 * dir is the position on the ground which then can be returned
		 */

		float[] rayPos = new float[4];
		float[] rayDir = new float[4];
		getCameraViewDirectionRay(rayPos, rayDir);

		/*
		 * then calc intersection with ground
		 */
		float f = -rayPos[2] / rayDir[2];
		return new Vec((f * rayDir[0]) + rayPos[0], (f * rayDir[1]) + rayPos[1], 0);
	}

	/**
	 * This will return a starting-point and direction of the line which comes
	 * out of the camera.
	 * 
	 * @param rayPos
	 *            here the rayPos will be stored, pass a new float[4]. The
	 *            result will contain {@link GLCamera#mPosition} so you dont
	 *            need to add it manually! Can be NULL if you only need the
	 *            ray-direction
	 * @param rayDir
	 *            here the rayDir will be stored, pass a new float[4]
	 * @return
	 */
	public void getCameraViewDirectionRay(float[] rayPos, float[] rayDir) {
		Matrix.invertM(mInvRotMatrix, 0, mRotationMatrix, mMatrixOffset);
		if (rayPos != null) {
			float[] initPos = { 0.0f, 0.0f, 0.0f, 1.0f };
			Matrix.multiplyMV(rayPos, 0, mInvRotMatrix, 0, initPos, 0);
			/*
			 * TODO is raypos != 0 if initPos ist the 0 vector?? is this calc.
			 * redundant?
			 */
			rayPos[0] += mPosition.x;
			rayPos[1] += mPosition.y;
			rayPos[2] += mPosition.z;
		}
		float[] initDir = { 0, 0, -GLRenderer.minViewDistance, 0.0f };
		Matrix.multiplyMV(rayDir, 0, mInvRotMatrix, 0, initDir, 0);
	}

	/**
	 * This method will be called by the virtual world to load the camera
	 * parameters like the position and the rotation
	 * 
	 * @param gl
	 * @param parent
	 * @param
	 */
	@Override
	public synchronized void render(GL10 gl, Renderable parent) {

		// if the camera sould not be in the center of the rotation it has to be
		// moved out before rotating:
		glLoadPosition(gl, mOffset);

		synchronized (mRotMatrLock) {
			// load rotation matrix:
			gl.glMultMatrixf(mRotationMatrix, mMatrixOffset);
		}

		// rotate Camera TODO use for manual rotation:
		glLoadRotation(gl, mRotationVec);

		// set the point where to rotate around
		//ARLogger.debug("GLCAMERA","Render Camera Position:\nx:" + mPosition.x+"\ny:"+mPosition.y+"\nz:"+mPosition.z);
		glLoadPosition(gl, mPosition);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gl.GLCamRotationController#setRotationMatrix(float[], int)
	 */
	@Override
	public void setRotationMatrix(float[] rotMatrix, int offset) {
		synchronized (mRotMatrLock) {
			mRotationMatrix = rotMatrix;
			mMatrixOffset = offset;
		}
	}

	/**
	 * Use a {@link ActionUseCameraAngles2} subclass instead
	 * 
	 * Currently only the azimuth is calculated here
	 * 
	 * @return [0]=azimuth (0 is north and 90 is east)
	 */
	@Deprecated
	public float[] getCameraAnglesInDegree() {
		updateCameraAngles();
		return cameraAnglesInDegree;
	}

	@Deprecated
	private void updateCameraAngles() {
		Calculus.invertM(mInvRotMatrix, 0, mRotationMatrix, mMatrixOffset);
		mInitDir[0] = 0;
		mInitDir[1] = 0;
		mInitDir[2] = -GLRenderer.minViewDistance;
		mInitDir[3] = 0;
		// TODO not a good idea to use myAnglesInRadians2 here, maybe additional
		// helper var?:
		Matrix.multiplyMV(mRotDirection, 0, mInvRotMatrix, 0, mInitDir, 0);
		cameraAnglesInDegree[0] = Vec.getRotationAroundZAxis(mRotDirection[1], mRotDirection[0]);
	}

	private void glLoadPosition(GL10 gl, Vec vec) {
		if (vec != null) {
			// if you want to set the center to 0 0 5 you have to move the
			// camera -5 units OUT of the screen
			gl.glTranslatef(-vec.x, -vec.y, -vec.z);
		}
	}

	private void glLoadRotation(GL10 gl, Vec vec) {
		/*
		 * a very important point is that its something completely different
		 * when you change the rotation order to x y z ! the order y x z is
		 * needed to use extract the angles from the rotation matrix with:
		 * 
		 * SensorManager.getOrientation(mRotationMatrix, anglesInRadians);
		 * 
		 * so remember this oder when doing own rotations.
		 * 
		 * y is always the angle from floor to top and x always the clockwise
		 * rotation (like when you lean to the side on a motorcycle) angle of
		 * the camera.
		 */
		if (vec != null) {
			gl.glRotatef(vec.y, 0, 1, 0);
			gl.glRotatef(vec.x, 1, 0, 0);
			gl.glRotatef(vec.z, 0, 0, 1);
		}
	}

	/**
	 * y is always the angle from floor to top (rotation around green achsis
	 * counterclockwise) and x always the clockwise rotation (like when you lean
	 * to the side on a motorcycle) angle of the camera.
	 * 
	 * to rotate from the green to the red axis (clockwise) you would have to
	 * add 90 degree.
	 * 
	 * @param xAngle
	 *            0 means the car drives straight forward, positive values (0 to
	 *            90) mean that the car turns left, negative values mean that
	 *            the car turns right
	 * @param yAngle
	 *            0 means the camera targets the ground, 180 the camera looks
	 *            into the sky
	 * @param zAngle
	 *            like a compass (0=north, 90 east and so on
	 */
	public void setRotation(float xAngle, float yAngle, float zAngle) {
		mRotationVec.x = xAngle;
		mRotationVec.y = yAngle;
		mRotationVec.z = zAngle;
	}

	@Deprecated
	public void setNewRotation(float xAngle, float yAngle, float zAngle) {
		if (myNewRotationVec == null) {
			myNewRotationVec = new Vec(xAngle, yAngle, zAngle);
		} else {
			myNewRotationVec.x = xAngle;
			myNewRotationVec.y = yAngle;
			myNewRotationVec.z = zAngle;
		}
	}

	/**
	 * change camera position relative to the actual camera rotation around the
	 * z axis, so the the camera is moved along the camera coordinate system and
	 * not the world coordinate system
	 * 
	 * @param deltaX
	 * @param deltaY
	 */
	public synchronized void changeXYPositionBuffered(float deltaX, float deltaY) {
		mMover.mTargetPos.add(deltaX, deltaY, 0);
	}

	/**
	 * 
	 * This will change the x and y position values instantly by
	 * adding/subtracting the passed values! If you want a smooth buffered
	 * movement to the new position, use
	 * {@link GLCamera#setNewPosition(float, float)}
	 * 
	 * @param deltaX
	 *            its important that this is not the absolute value. Its only
	 *            the value wich will be added/subtracted to the current one
	 * @param deltaY
	 *            see deltaX description
	 */
	public synchronized void changePositionUnbuffered(float deltaX, float deltaY) {
		mPosition.x += deltaX;
		mPosition.y += deltaY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gl.GLCamRotationController#resetBufferedAngle()
	 */
	@Override
	@Deprecated
	public void resetBufferedAngle() {
		Log.d(LOG_TAG, "Reseting camera rotation in resetBufferedAngle()!");
		if ((myNewRotationVec != null) && (mSensorInputEnabled)) {
			myNewRotationVec.setToZero();
		}
	}

	/**
	 * This will change the z value of the camera-rotation instantly without
	 * buffering by adding/subtracting the specified deltaZ value. The buffered
	 * version of this method is called
	 * {@link GLCamera#changeXYPositionBuffered(float, float)}
	 * 
	 * @param deltaZ
	 */
	public void changeZAngleUnbuffered(float deltaZ) {
		mRotationVec.z += deltaZ;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gl.GLCamRotationController#changeZAngleBuffered(float)
	 */
	@Override
	@Deprecated
	public void changeZAngleBuffered(float deltaZ) {
		if (myNewRotationVec == null) {
			myNewRotationVec = new Vec();
		}
		myNewRotationVec.z += deltaZ;

	}

	/**
	 * This will change the z value of the camera-position by adding/subtracting
	 * the specified deltaZ value.
	 * 
	 * @param deltaZ
	 *            eg. -10 to move the camera 10 meters down
	 */
	public void changeZPositionBuffered(float deltaZ) {
		mMover.mTargetPos.add(0, 0, deltaZ);
	}

	/**
	 * @param resetZValueToo
	 *            if you just want to reset x and y set this to false
	 */
	public void resetPosition(boolean resetZValueToo) {
		float pz = mPosition.z;
		float npz = mMover.mTargetPos.z;
		mPosition.setToZero();
		mMover.mTargetPos.setToZero();
		if (!resetZValueToo) {
			mPosition.z = pz;
			mMover.mTargetPos.z = npz;
		}
	}

	/**
	 * This will reset the camera postion to (0,0,0)
	 */
	public void resetPosition() {
		resetPosition(true);
	}

	public void changeNewPosition(float deltaX, float deltaY, float deltaZ) {
		mMover.mTargetPos.add(deltaX, deltaY, deltaZ);
	}

	/**
	 * @param x
	 *            positive means east of zero pos (longitude direction)
	 * @param y
	 *            positive means north of zero pos (latitude direction)
	 * @param z
	 *            the height of the camera
	 */
	public void setNewPosition(float x, float y, float z) {
		mMover.mTargetPos.setTo(x, y, z);

	}

	/**
	 * @param x
	 *            positive means east of zero pos (longitude direction)
	 * @param y
	 *            positive means north of zero pos (latitude direction)
	 */
	public void setNewPosition(float x, float y) {
		mMover.mTargetPos.setTo(x, y);
	}

	public Vec getNewCameraOffset() {
		return mNewOffset;
	}

	public void setNewOffset(Vec myNewOffset) {
		this.mNewOffset = myNewOffset;
	}

	/**
	 * @return the position in the virtual world. This vec could be used as the
	 *         users postion e.g. <br>
	 * <br>
	 *         x positive means east of zero pos (latitude direction) <br>
	 *         y positive means north of zero pos (longitude direction) <br>
	 *         z the height of the camera
	 */
	@Override
	public Vec getPosition() {
		return mPosition;
	}

	@Override
	public void setPosition(Vec position) {
		if (mPosition == null) {
			mPosition = position;
		} else {
			mPosition.setToVec(position);
		}
	}

	/**
	 * @return The position where the camera moves to. Will be NULL if new
	 *         position never set before!
	 */
	// public Vec getMyNewPosition() {
	//
	// return myNewPosition;
	// }

	/**
	 * The resulting coordinates can differ from
	 * {@link EventManager#getCurrentLocationObject()} if the camera was not
	 * moved according to the GPS input (eg moved via trackball).
	 * 
	 * @return
	 */
	public Location getGPSLocation() {
		Vec coords = getGPSPositionVec();
		Location pos = new Location("customCreated");
		pos.setLatitude(coords.y);
		pos.setLongitude(coords.x);
		pos.setAltitude(coords.z);
		return pos;
	}

	/**
	 * The resulting coordinates can differ from
	 * {@link EventManager#getCurrentLocationObject()} if the camera was not
	 * moved according to the GPS input (eg moved via trackball).
	 * 
	 * @return a Vector with x=Longitude, y=Latitude, z=Altitude
	 */
	public Vec getGPSPositionVec() {
		GeoObj zeroPos = EventManager.getInstance()
				.getZeroPositionLocationObject();
		return GeoObj.calcGPSPosition(this.getPosition(),
				zeroPos.getLatitude(), zeroPos.getLongitude(),
				zeroPos.getAltitude());
	}

	public void setGpsPos(GeoObj newPos) {
		Vec pos = newPos.getVirtualPosition(EventManager.getInstance()
				.getZeroPositionLocationObject());
		setNewPosition(pos);
	}

	/**
	 * The resulting coordinates can differ from
	 * {@link EventManager#getCurrentLocationObject()} if the camera was not
	 * moved according to the GPS input (eg moved via trackball).
	 * 
	 * @return
	 */
	public GeoObj getGPSPositionAsGeoObj() {
		Vec v = getGPSPositionVec();
		return new GeoObj(v.y, v.x, v.z);
	}

	public float[] getRotationMatrix() {
		return mRotationMatrix;
	}

	@Override
	public void showDebugInformation() {
		Log.w(LOG_TAG, "Infos about GLCamera:");
		Log.w(LOG_TAG, "   > mPosition=" + mPosition);
		Log.w(LOG_TAG, "   > myMover.myTargetPos=" + mMover.mTargetPos);
		Log.w(LOG_TAG, "   > mOffset=" + mOffset);
		Log.w(LOG_TAG, "   > mNewOffset=" + mNewOffset);
		Log.w(LOG_TAG, "   > mRotationVec=" + mRotationVec);
		Log.w(LOG_TAG, "   > myNewRotationVec=" + myNewRotationVec);
		Log.w(LOG_TAG, "   > mRotationMatrix=" + mRotationMatrix);
	}

	public boolean isSensorInputEnabled() {
		return mSensorInputEnabled;
	}

	/**
	 * @param mSensorInputEnabled
	 *            set false tell the camera to ignore sensor input. You can
	 *            still use the methods like
	 *            {@link GLCamera#setNewPosition(Vec)}
	 *            {@link GLCamera#setNewRotation(Vec)} to move the camera but
	 *            the AR impression will be lost. Use this for games and defined
	 *            movement through a virtual world.
	 */
	public void setSensorInputEnabled(boolean sensorInputEnabled) {
		this.mSensorInputEnabled = sensorInputEnabled;
		if (!sensorInputEnabled) {
			// reset rotation matrix
			mRotationMatrix = Calculus.createIdentityMatrix();
		}
	}

}
