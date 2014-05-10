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
package com.github.mrstampy.esplab.gui;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.chart.Axis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Control;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import org.controlsfx.dialog.Dialogs;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import com.github.mrstampy.esp.dsp.lab.RawEspConnection;
import com.github.mrstampy.esp.multiconnectionsocket.ConnectionEvent;
import com.github.mrstampy.esp.multiconnectionsocket.ConnectionEvent.State;
import com.github.mrstampy.esp.multiconnectionsocket.ConnectionEventListener;
import com.github.mrstampy.esp.multiconnectionsocket.MultiConnectionSocketException;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractGraph.
 *
 * @param <XAXIS> the generic type
 */
public abstract class AbstractGraph<XAXIS extends Object> implements ConnectionEventListener {
	private static final Logger log = LoggerFactory.getLogger(AbstractGraph.class);

	private RawEspConnection connection;
	private Subscription subscription;

	/** The chart. */
	protected XYChart<XAXIS, Number> chart;
	
	/** The series. */
	protected Series<XAXIS, Number> series = new Series<XAXIS, Number>();
	
	/** The x axis. */
	protected Axis<XAXIS> xAxis;
	
	/** The y axis. */
	protected NumberAxis yAxis = new NumberAxis();
	
	/** The start stop. */
	protected ToggleButton startStop;

	/** The running. */
	protected AtomicBoolean running = new AtomicBoolean(false);

	/** The scheduler. */
	protected Scheduler scheduler = Schedulers.io();
	private rx.Subscription snap;

	private AtomicBoolean errorShowing = new AtomicBoolean();

	/**
	 * Instantiates a new abstract graph.
	 */
	public AbstractGraph() {
		initButtons();
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esp.multiconnectionsocket.ConnectionEventListener#connectionEventPerformed(com.github.mrstampy.esp.multiconnectionsocket.ConnectionEvent)
	 */
	@Override
	public void connectionEventPerformed(ConnectionEvent e) {
		switch (e.getState()) {
		case STARTED:
			preStart();
			start();
			break;
		case STOPPED:
			preStop();
			stop();
			break;
		case ERROR_STOPPED:
		case ERROR_UNBOUND:
			preStop();
			stop();
			connectionError(e.getState());
			break;
		default:
			break;
		}
	}

	/**
	 * Tool tip.
	 *
	 * @param node the node
	 * @param tip the tip
	 */
	protected void toolTip(Control node, String tip) {
		node.setTooltip(new Tooltip(tip));
	}

	private void preStop() {
		running.set(false);
		if (snap != null) snap.unsubscribe();
		if(startStop.isSelected()) {
			startStop.setSelected(false);
			startStop.setText("Start");
		}
	}

	/**
	 * Pre start.
	 */
	protected void preStart() {
		running.set(true);
		snap = scheduler.schedulePeriodically(a -> graphAccept(getConnection().getCurrent()), 250, 250,
				TimeUnit.MILLISECONDS);
		if(! startStop.isSelected()) {
			startStop.setSelected(true);
			startStop.setText("Stop");
		}
	}
	
	/**
	 * Gets the layout.
	 *
	 * @return the layout
	 */
	public abstract Region getLayout();

	/**
	 * Connection error.
	 *
	 * @param state the state
	 */
	protected void connectionError(State state) {
		showConnectionError(getLostConnectionMasthead());
	}

	/**
	 * Start.
	 */
	protected abstract void start();

	/**
	 * Stop.
	 */
	protected abstract void stop();

	/**
	 * Gets the buttons.
	 *
	 * @return the buttons
	 */
	protected Pane getButtons() {
		HBox box = new HBox(10);
		box.setAlignment(Pos.CENTER);

		box.getChildren().addAll(startStop);

		return box;
	}

	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
	public RawEspConnection getConnection() {
		return connection;
	}

	/**
	 * Sets the connection.
	 *
	 * @param connection the new connection
	 */
	public void setConnection(RawEspConnection connection) {
		preSetConnection();
		this.connection = connection;
		postSetConnection();
	}

	/**
	 * Pre set connection.
	 */
	protected void preSetConnection() {
		if (connection == null) return;

		connection.removeConnectionEventListener(this);
		if (subscription != null) subscription.unsubscribe();
	}

	/**
	 * Post set connection.
	 */
	protected void postSetConnection() {
		getConnection().addConnectionEventListener(this);
	}

	/**
	 * Graph accept.
	 *
	 * @param samples the samples
	 */
	protected abstract void graphAccept(double[][] samples);

	private void initButtons() {
		startStop = new ToggleButton("Start");

		EventStreams.eventsOf(startStop, MouseEvent.MOUSE_CLICKED).subscribe(t -> startStop());
		toolTip(startStop, "Start / stop the ESP device");
	}

	/**
	 * Start stop.
	 */
	protected void startStop() {
		if (startStop.isSelected()) {
			startButtonClicked();
		} else {
			stopButtonClicked();
		}
	}

	private void stopButtonClicked() {
		getConnection().stop();
		startStop.setText("Start");
		if (startStop.isSelected()) startStop.setSelected(false);
	}

	private void startButtonClicked() {
		try {
			getConnection().start();
			startStop.setText("Stop");
		} catch (MultiConnectionSocketException e) {
			log.error("Unexpected exception starting connection", e);
			showConnectionError(getMasthead());
		}
	}

	/**
	 * Show connection error.
	 *
	 * @param masthead the masthead
	 */
	protected void showConnectionError(final String masthead) {
		if (errorShowing.get()) return;

		if (Platform.isFxApplicationThread()) {
			connectionError(masthead);
		} else {
			Platform.runLater(() -> connectionError(masthead));
		}
	}

	private void connectionError(String masthead) {
		errorShowing.set(true);
		stopButtonClicked();
		Dialogs.create().masthead(masthead).title("Device Unavailable").owner(startStop).showError();
		errorShowing.set(false);
	}

	private String getMasthead() {
		return "Could not connect to the " + getConnection().getName() + ". Set/reset the connection and try again.";
	}

	private String getLostConnectionMasthead() {
		return "Lost connection to the " + getConnection().getName() + ". Reset the connection and try again.";
	}

}
