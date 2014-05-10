package com.github.mrstampy.esplab.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.controlsfx.control.MasterDetailPane;

import rx.Observable;

public class RawDataGraph extends AbstractGraph<Number> {
	private static final int MAX_X = 100;

	private AnimationTimer timer;

	private AtomicInteger counter = new AtomicInteger();
	private AtomicInteger xPos = new AtomicInteger();
	private ArrayBlockingQueue<Double> queue = new ArrayBlockingQueue<>(10000);

	public RawDataGraph() {
		super();
		initChart();
	}

	public Region getLayout() {
		MasterDetailPane pane = new MasterDetailPane(Side.BOTTOM);
		pane.setMinWidth(1000);
		pane.setDividerPosition(0.8);

		pane.setMasterNode(chart);
		VBox box = new VBox(10, startStop);
		box.setAlignment(Pos.CENTER);
		pane.setDetailNode(box);

		return pane;
	}

	protected void graphAccept(double[][] t) {
		if (!running.get() || t.length == 0) return;

		if (counter.get() % 4 == 0) {
			for (int i = 0; i < t[0].length; i++) {
				queue.add(t[0][i]);
			}
		}

		counter.getAndIncrement();
	}

	protected void start() {
		counter.set(0);
		queue.clear();
		series.getData().clear();
		xPos.set(0);
		timer.start();
	}

	protected void stop() {
		timer.stop();
	}

	private void initChart() {
		NumberAxis na = new NumberAxis();
		xAxis = na;
		xAxis.setAutoRanging(true);
		na.setForceZeroInRange(false);
		na.setMinorTickVisible(false);
		xAxis.setTickMarkVisible(false);
		xAxis.setTickLabelsVisible(false);

		yAxis.setAutoRanging(true);

		chart = new LineChart<Number, Number>(na, yAxis);
		chart.setTitle("Raw Data");
		chart.setAnimated(false);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);

		series.setName("Raw Data");

		chart.getData().add(series);

		timer = new AnimationTimer() {

			@Override
			public void handle(long arg0) {
				List<Double> list = new ArrayList<>();
				queue.drainTo(list);

				list.forEach(t -> updateChartImpl(t));
			}
		};
	}

	private void updateChartImpl(double val) {
		Observable.timer(100, TimeUnit.MICROSECONDS).subscribe(t -> Platform.runLater(() -> updateChart(val)));
	}

	private void updateChart(double val) {
		ObservableList<Data<Number, Number>> data = series.getData();

		data.add(new Data<Number, Number>(xPos.getAndIncrement(), val));

		if (data.size() > MAX_X) data.remove(0, data.size() - MAX_X);

		int x = xPos.get();

		NumberAxis na = (NumberAxis) xAxis;
		na.setLowerBound(x - MAX_X);
		na.setUpperBound(x);
	}
}
