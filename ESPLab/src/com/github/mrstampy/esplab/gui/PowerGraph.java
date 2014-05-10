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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.RangeSlider;

import rx.Observable;

import com.github.mrstampy.esp.dsp.lab.EspWindowFunction;
import com.github.mrstampy.esp.dsp.lab.FFTType;
import com.github.mrstampy.esp.dsp.lab.Lab;
import com.github.mrstampy.esp.dsp.lab.PassFilter;
import com.sun.javafx.collections.ObservableListWrapper;

// TODO: Auto-generated Javadoc
/**
 * The Class PowerGraph.
 */
public class PowerGraph extends AbstractGraph<String> {

	private ComboBox<FFTType> fftType = new ComboBox<FFTType>();
	private Button calculateBaseline = new Button("Calculate Baseline");
	private Button clearBaseline = new Button("Clear Baseline");
	private CheckBox displayGraph = new CheckBox("Display Graph");
	private ComboBox<EspWindowFunction> functions = new ComboBox<EspWindowFunction>();
	private ComboBox<PassFilter> filters = new ComboBox<PassFilter>();
	private CheckBox normalizeSignal = new CheckBox("Normalize Signal");
	private CheckBox normalizeFft = new CheckBox("Normalize FFT");
	private CheckBox absoluteValues = new CheckBox("Absolute Values");

	private RangeSlider bandPassSlider = new RangeSlider(1, 40, 1, 40);

	private Lab lab;

	private AtomicBoolean graphing = new AtomicBoolean(true);

	/**
	 * Instantiates a new power graph.
	 *
	 * @param lab the lab
	 */
	public PowerGraph(Lab lab) {
		super();
		setLab(lab);
		lab.addSignalProcessedListener(t -> plot(t));

		initChart();
		initComboBoxes();
		initCheckboxes();
		initSliders();
		initButtons();

		fftSelected();
		filterSelected();
		toolTips();
	}

	private void toolTips() {
		toolTip(fftType, "Choose the type of FFT to apply to the signal");
		toolTip(calculateBaseline, "Calculate a baseline for the signal");
		toolTip(clearBaseline, "Clear the current baseline from the signal");
		toolTip(displayGraph, "Display or suppress graphing of the processed signal");
		toolTip(functions, "Choose the window function to apply to the signal (for real & log FFT's)");
		toolTip(filters, "Choose the pass filter to apply to the signal");
		toolTip(normalizeFft, "Normalize the FFT values");
		toolTip(absoluteValues, "Use absolute values");
		toolTip(bandPassSlider, "Select the value or range for the pass filters");
		toolTip(normalizeSignal, "Normalize the input signal");
	}

	private void initButtons() {
		calculateBaseline.addEventHandler(ActionEvent.ACTION, t -> calculateBaselineClicked());
		calculateBaseline.setDisable(true);
		clearBaseline.addEventHandler(ActionEvent.ACTION, t -> clearBaselineClicked());
		displayGraph.addEventHandler(ActionEvent.ACTION, t -> displayGraphClicked());
		displayGraph.fire();
	}

	private void displayGraphClicked() {
		graphing.set(displayGraph.isSelected());
		if (!graphing.get()) reset();
	}

	private void clearBaselineClicked() {
		getLab().resetBaseline();
	}

	private void calculateBaselineClicked() {
		calculateBaseline.setDisable(true);
		calculateBaseline.setText("Calculating...");
		getLab().calculateBaseline();
		Observable.timer(10, TimeUnit.SECONDS).subscribe(a -> Platform.runLater(() -> setBaseline()));
	}

	private void setBaseline() {
		getLab().stopCalculateBaseline();
		calculateBaseline.setDisable(false);
		calculateBaseline.setText("Calculate Baseline");
	}

	private void initSliders() {
		initSlider(bandPassSlider);

		bandPassSlider.lowValueProperty().addListener((o, old, newVal) -> lowBandPassValueChanged(newVal.doubleValue()));
		bandPassSlider.highValueProperty().addListener((o, old, newVal) -> highBandPassValueChanged(newVal.doubleValue()));
	}

	private void highBandPassValueChanged(double val) {
		getLab().setHighPassFrequency(val);
	}

	private void lowBandPassValueChanged(double val) {
		getLab().setLowPassFrequency(val);
	}

	private void initSlider(RangeSlider slider) {
		slider.setShowTickMarks(true);
		slider.setSnapToPixel(true);
		slider.setSnapToTicks(true);
		slider.setMajorTickUnit(1);
		slider.setMinWidth(400);
		slider.setBlockIncrement(1);
	}

	private void initCheckboxes() {
		normalizeFft.addEventHandler(ActionEvent.ACTION, t -> normalizeFftSelected());

		normalizeSignal.addEventHandler(ActionEvent.ACTION, t -> normalizeSignalSelected());

		absoluteValues.addEventHandler(ActionEvent.ACTION, t -> absoluteValuesSelected());
	}

	private void absoluteValuesSelected() {
		getLab().setAbsoluteValues(absoluteValues.isSelected());
	}

	private void normalizeSignalSelected() {
		getLab().setNormalizeSignal(normalizeSignal.isSelected());
	}

	private void normalizeFftSelected() {
		getLab().setNormalizeFft(normalizeFft.isSelected());
		setAbsoluteEnabled();
	}

	private void initComboBoxes() {
		functions.setItems(new ObservableListWrapper<>(Arrays.asList(EspWindowFunction.values())));
		functions.setValue(getConnection().getWindowFunction());

		functions.addEventHandler(ActionEvent.ACTION, t -> getConnection().setWindowFunction(functions.getValue()));

		filters.setItems(new ObservableListWrapper<>(Arrays.asList(PassFilter.values())));
		filters.setValue(getLab().getPassFilter());

		filters.addEventHandler(ActionEvent.ACTION, t -> filterSelected());

		fftType.setItems(new ObservableListWrapper<>(Arrays.asList(FFTType.values())));
		fftType.setValue(getLab().getFftType());

		fftType.addEventHandler(ActionEvent.ACTION, t -> fftSelected());
	}

	private void fftSelected() {
		setAbsoluteEnabled();
		setNormalizeFftEnabled();
		getLab().setFftType(fftType.getValue());
	}

	private void setNormalizeFftEnabled() {
		boolean disable = fftType.getValue() == FFTType.no_fft;
		normalizeFft.setDisable(disable);
	}

	private void filterSelected() {
		getLab().setPassFilter(filters.getValue());
		setBandPassSliderEnabled();
	}

	private void setBandPassSliderEnabled() {
		boolean disable = filters.getValue() == PassFilter.no_pass_filter;
		bandPassSlider.setDisable(disable);
		bandPassSlider.setOpacity(disable ? 0.5 : 1);
	}

	private void setAbsoluteEnabled() {
		boolean disable = FFTType.log_fft == fftType.getValue() || normalizeFft.isSelected();
		absoluteValues.setDisable(disable);
	}

	private void initChart() {
		CategoryAxis ca = new CategoryAxis();
		xAxis = ca;
		xAxis.setAutoRanging(true);

		yAxis.setAutoRanging(true);

		BarChart<String, Number> bc = new BarChart<String, Number>(xAxis, yAxis);
		chart = bc;
		chart.setTitle("Power");
		chart.setAnimated(false);
		chart.setMinWidth(900);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);

		bc.setBarGap(0);
		bc.setCategoryGap(2);

		series.setName("Frequency (Hz)");

		for (int i = 1; i <= 40; i++) {
			series.getData().add(new Data<String, Number>(Integer.toString(i), 0));
		}

		chart.getData().add(series);
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esplab.gui.AbstractGraph#getLayout()
	 */
	public Region getLayout() {
		MasterDetailPane pane = new MasterDetailPane(Side.BOTTOM);
		pane.setMinWidth(1000);
		pane.setDividerPosition(0.5);

		pane.setMasterNode(chart);

		VBox box = new VBox(10);
		box.setAlignment(Pos.CENTER);

		box.getChildren().addAll(displayGraph, getPreFftPane());

		Pane buttons = getButtons();
		buttons.getChildren().addAll(calculateBaseline, clearBaseline);
		box.getChildren().addAll(buttons);

		pane.setDetailNode(box);

		return pane;
	}

	/**
	 * Gets the lab.
	 *
	 * @return the lab
	 */
	public Lab getLab() {
		return lab;
	}

	/**
	 * Sets the lab.
	 *
	 * @param lab the new lab
	 */
	public void setLab(Lab lab) {
		this.lab = lab;
		setConnection(lab.getConnection());
	}

	private GridPane getPreFftPane() {
		GridPane gp = new GridPane();

		Label wf = new Label("Window Functions");
		Label pf = new Label("Pass Filters");
		Label lbl = new Label("Pass Filter Frequencies");
		Label fft = new Label("FFT Type");

		GridHelper gh = new GridHelper();

		gridify(gh, fft, HPos.RIGHT, VPos.CENTER);

		gh.incrX();
		gridify(gh, fftType, HPos.LEFT, VPos.CENTER);

		gh.incrX();
		gridify(gh, normalizeFft, HPos.LEFT, VPos.CENTER);

		gh.newLine();
		gridify(gh, wf, HPos.RIGHT, VPos.CENTER);

		gh.incrX();
		gridify(gh, functions, HPos.LEFT, VPos.CENTER);

		gh.incrX();
		gridify(gh, absoluteValues, HPos.LEFT, VPos.CENTER);

		gh.newLine();
		gridify(gh, pf, HPos.RIGHT, VPos.CENTER);

		gh.incrX();
		gridify(gh, filters, HPos.LEFT, VPos.CENTER);

		gh.incrX();
		gridify(gh, normalizeSignal, HPos.LEFT, VPos.CENTER);

		gh.width = 3;
		gh.newLine();
		gridify(gh, lbl, HPos.CENTER, VPos.CENTER);

		gh.newLine();
		HBox bp = getBandPassSlider();
		gridify(gh, bp, HPos.CENTER, VPos.CENTER);

		gp.setAlignment(Pos.CENTER);
		gp.getChildren().addAll(fft, fftType, displayGraph, wf, functions, pf, filters, absoluteValues, lbl, bp,
				normalizeSignal, normalizeFft);

		return gp;
	}

	private void gridify(GridHelper gh, Node node, HPos hPos, VPos vPos) {
		GridPane.setConstraints(node, gh.x, gh.y, gh.width, gh.height, hPos, vPos, Priority.NEVER, Priority.NEVER,
				new Insets(5));
	}

	private HBox getBandPassSlider() {
		HBox box = new HBox(10);
		box.getChildren().addAll(bandPassSlider);

		return box;
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esplab.gui.AbstractGraph#start()
	 */
	@Override
	protected void start() {
		calculateBaseline.setDisable(false);
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esplab.gui.AbstractGraph#stop()
	 */
	@Override
	protected void stop() {
		Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(a -> reset());
		if (!startStop.isSelected()) {
			startStop.setSelected(false);
			startStop();
		}
		calculateBaseline.setDisable(true);
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esplab.gui.AbstractGraph#postSetConnection()
	 */
	protected void postSetConnection() {
		super.postSetConnection();
		getLab().setConnection(getConnection());
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.esplab.gui.AbstractGraph#graphAccept(double[][])
	 */
	@Override
	protected void graphAccept(double[][] samples) {
		Observable.just(samples).subscribe(t1 -> getLab().process(t1));
	}

	private void reset() {
		double[] zero = new double[getLab().getNumBands()];
		Arrays.fill(zero, 0);
		Platform.runLater(() -> paintPowers(zero));
	}

	private void plot(double[] wmad) {
		if (graphing.get()) Platform.runLater(() -> paintPowers(wmad));
	}

	private void paintPowers(double[] wmad) {
		ObservableList<Data<String, Number>> data = series.getData();
		for (int i = 0; i < wmad.length - 1; i++) {
			Data<String, Number> point = data.get(i);
			point.setYValue(wmad[i + 1]);
		}
	}

	/**
	 * The Class GridHelper.
	 */
	class GridHelper {
		
		/** The height. */
		int x, y, width, height;

		/**
		 * Instantiates a new grid helper.
		 */
		GridHelper() {
			width = 1;
			height = 1;
		}

		/**
		 * Incr x.
		 */
		void incrX() {
			x++;
		}

		/**
		 * Incr y.
		 */
		void incrY() {
			y++;
		}

		/**
		 * New line.
		 */
		void newLine() {
			incrY();
			x = 0;
		}

		/**
		 * Reset wh.
		 */
		void resetWH() {
			width = 1;
			height = 1;
		}
	}

}