package com.github.mrstampy.esplab;

import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.github.mrstampy.esp.dsp.lab.DefaultLab;
import com.github.mrstampy.esp.dsp.lab.Lab;
import com.github.mrstampy.esp.dsp.lab.RawEspConnection;
import com.github.mrstampy.esplab.gui.AbstractGraph;
import com.github.mrstampy.esplab.gui.PowerGraph;

public abstract class EspLab extends Application {

	public EspLab() {
		super();
	}

	public void start(Stage stage) throws Exception {
		try {
			AbstractGraph<?> rdg = getGraph(getConnection());
			
			Scene scene = new Scene(rdg.getLayout());
			stage.setScene(scene);

			stage.centerOnScreen();
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected abstract RawEspConnection getConnection() throws IOException;
	
	private AbstractGraph<?> getGraph(RawEspConnection connection) {
		// RawDataGraph rdg = new RawDataGraph();
		// rdg.setConnection(connection);
		// return rdg;
		Lab lab = new DefaultLab(41);
		lab.setConnection(connection);
		return new PowerGraph(lab);
	}

	public void stop() {
		System.exit(0);
	}

	public static void main(String[] args) {
		launch(args);
	}

}
