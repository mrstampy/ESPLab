/*
 * Copyright (C) ESPLab 2014 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
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

// TODO: Auto-generated Javadoc
/**
 * The Class EspLab provides an example of how to use the graphs. Included for
 * reference.
 */
public abstract class EspLab extends Application {

	/**
	 * Instantiates a new esp lab.
	 */
	public EspLab() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
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

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 * @throws IOException
	 *           Signals that an I/O exception has occurred.
	 */
	protected abstract RawEspConnection getConnection() throws IOException;

	private AbstractGraph<?> getGraph(RawEspConnection connection) {
		// RawDataGraph rdg = new RawDataGraph();
		// rdg.setConnection(connection);
		// return rdg;
		Lab lab = new DefaultLab(41);
		lab.setConnection(connection);
		return new PowerGraph(lab);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.application.Application#stop()
	 */
	public void stop() {
		System.exit(0);
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *          the arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
