package de.rwth.setups;

import entry.ISetupEntry;
import gl.GL1Renderer;
import gl.GLFactory;
import gui.InfoScreenSettings;
import setup.DefaultArSetup;
import system.ErrorHandler;
import util.Vec;
import worlddata.AbstractObj;
import worlddata.RenderQuadList;
import worlddata.World;

public class LargeWorldsSetup extends DefaultArSetup {
	private static final int NUMBER_OF_OBJECTS = 1000;

	@Override
	public void initFieldsIfNecessary() {
		super.initFieldsIfNecessary();
		// allow the user to send error reports to the developer:
		ErrorHandler.enableEmailReports("droidar.rwth@gmail.com",
				"Error in LargeWorldsSetup");
	}

	@Override
	public void addInfoScreen(InfoScreenSettings infoScreenData) {
		infoScreenData
				.addText("This setup will demonstrate a possible culling-strategy for very large virtual worlds.");
		infoScreenData
				.addText((NUMBER_OF_OBJECTS)
						+ " textured individual objects will be added to the virtual world.");
	}

	@Override
	public void addObjectsTo(GL1Renderer renderer, World world,
			GLFactory objectFactory) {
		RenderQuadList list = new RenderQuadList(getCamera(), 100, 10);
		for (int x = (int) Math.sqrt(NUMBER_OF_OBJECTS); x >= 0; x--) {
			for (int y = (int) Math.sqrt(NUMBER_OF_OBJECTS); y >= 0; y--) {
				list.add(newObj(x * 5, y * 5));
			}
		}
		// when the quad-list is created, add it to the world:
		world.add(list);
	}

	private AbstractObj newObj(int x, int y) {
		String name = "x=" + x + ",y=" + y;
		return GLFactory.getInstance().newTextObject(name, new Vec(x, y, 0),
				getActivity(), getCamera());

	}
}
