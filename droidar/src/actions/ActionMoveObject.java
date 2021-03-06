package actions;

import gl.GLCamera;
import util.Vec;
import util.Wrapper;
import worlddata.MoveComp;
import worlddata.Obj;

/**
 * An action to move an object along the axis.
 *
 */
public class ActionMoveObject extends ActionDoAlongAxis {

	private Wrapper mTarget;

	/**
	 * Constructor.
	 * @param wrapper - {@link util.Wrapper}
	 * @param camera - {@link gl.GLCamera}
	 * @param trackballFactor
	 *            should be around 2-15
	 * @param touchscreenFactor
	 *            25 would be good value to start.The higher the value the
	 *            slower the movement
	 */
	public ActionMoveObject(Wrapper wrapper, GLCamera camera,
			float trackballFactor, float touchscreenFactor) {
		super(camera, trackballFactor, touchscreenFactor);
		mTarget = wrapper;
	}

	@Override
	public void doAlongViewAxis(float x, float y) {
		if (mTarget != null && mTarget.getObject() instanceof Obj) {
			foundObj((Obj) mTarget.getObject(), x, y);
		}
	}

	private void foundObj(Obj obj, float x, float y) {
		MoveComp mc = obj.getComp(MoveComp.class);
		Vec pos = obj.getPosition();
		if (mc != null) {
			if (mc.mTargetPos == null && pos != null) {
				mc.mTargetPos = pos.copy();
			}
			mc.mTargetPos.add(x, y, 0.0f);
		} else if (pos != null) {
			/*
			 * if no move comp was found in the target object, the mesh itself
			 * will be used
			 */
			pos.add(x, y, 0);
		}
	}
}
