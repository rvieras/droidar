package listeners;

import listeners.eventManagerListeners.CamRotationVecUpdateListener;
import listeners.eventManagerListeners.LocationEventListener;
import listeners.eventManagerListeners.OrientationChangedListener;
import listeners.eventManagerListeners.TouchMoveListener;
import listeners.eventManagerListeners.TrackBallEventListener;

/**
 * Listener to will listen for events from multiple sources. 
 */
public interface EventListener extends LocationEventListener,
		OrientationChangedListener, TouchMoveListener, TrackBallEventListener,
		CamRotationVecUpdateListener {

}