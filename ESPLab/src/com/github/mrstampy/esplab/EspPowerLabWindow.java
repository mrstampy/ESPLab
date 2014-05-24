package com.github.mrstampy.esplab;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.github.mrstampy.esp.dsp.lab.Lab;
import com.github.mrstampy.esplab.gui.PowerGraph;

public class EspPowerLabWindow extends Stage {

	private PowerGraph graph;

	public EspPowerLabWindow(Lab lab) {
		super();
		graph = new PowerGraph(lab);
		setScene(new Scene(graph.getLayout()));
		setTitle(lab.getConnection().getName() + " Laboratory" + getChannelText(lab));
		addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> close());
	}

	private String getChannelText(Lab lab) {
		return ", " + lab.getConnection().getChannel(lab.getChannel()).getDescription();
	}

	public void close() {
		graph.getConnection().removeConnectionEventListener(graph);
		graph.setConnection(null);
		super.close();
	}
}
