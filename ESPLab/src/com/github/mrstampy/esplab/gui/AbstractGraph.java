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

public abstract class AbstractGraph<XAXIS extends Object> implements ConnectionEventListener {
	private static final Logger log = LoggerFactory.getLogger(AbstractGraph.class);

	private RawEspConnection connection;
	private Subscription subscription;

	protected XYChart<XAXIS, Number> chart;
	protected Series<XAXIS, Number> series = new Series<XAXIS, Number>();
	protected Axis<XAXIS> xAxis;
	protected NumberAxis yAxis = new NumberAxis();
	protected ToggleButton startStop;

	protected AtomicBoolean running = new AtomicBoolean(false);

	protected Scheduler scheduler = Schedulers.io();
	private rx.Subscription snap;

	private AtomicBoolean errorShowing = new AtomicBoolean();

	public AbstractGraph() {
		initButtons();
	}

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

	protected void preStart() {
		running.set(true);
		snap = scheduler.schedulePeriodically(a -> graphAccept(getConnection().getCurrent()), 250, 250,
				TimeUnit.MILLISECONDS);
		if(! startStop.isSelected()) {
			startStop.setSelected(true);
			startStop.setText("Stop");
		}
	}
	
	public abstract Region getLayout();

	protected void connectionError(State state) {
		showConnectionError(getLostConnectionMasthead());
	}

	protected abstract void start();

	protected abstract void stop();

	protected Pane getButtons() {
		HBox box = new HBox(10);
		box.setAlignment(Pos.CENTER);

		box.getChildren().addAll(startStop);

		return box;
	}

	public RawEspConnection getConnection() {
		return connection;
	}

	public void setConnection(RawEspConnection connection) {
		preSetConnection();
		this.connection = connection;
		postSetConnection();
	}

	protected void preSetConnection() {
		if (connection == null) return;

		connection.removeConnectionEventListener(this);
		if (subscription != null) subscription.unsubscribe();
	}

	protected void postSetConnection() {
		getConnection().addConnectionEventListener(this);
	}

	protected abstract void graphAccept(double[][] samples);

	private void initButtons() {
		startStop = new ToggleButton("Start");

		EventStreams.eventsOf(startStop, MouseEvent.MOUSE_CLICKED).subscribe(t -> startStop());
		toolTip(startStop, "Start / stop the ESP device");
	}

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
