package main;

/**
 * CircleApplet2.java
 * Click mouse on applet to show circles
 * This version shows running the drawing methods in a separate thread.
 * Interestingly, the inner class which catches the mouse clicked event
 * implements the interface Runnable.
 * In contrast with CirclesApplet1a.java which puts the AWT to sleep and
 * achieves nothing with respect to controlling repaint() requests, this program
 * puts the  programmer defined thread to sleep. This thread runs the loop which
 * is generating the repaint() calls. While it is sleeping, it cannot invoke repaint().
 * This pause gives the AWT thread time to process previous repaint() requests and
 * execute update() and paint().
 *
 *  Sleep times should normally be 10 ms or longer.
 */

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Main extends Applet {
	Canvas canvas;

	public void init() {
		setLayout(new GridLayout());
		setSize(800, 1000);
		canvas = new MyCanvas();
		canvas.setSize(800, 1000);
		add(canvas);
	}
}

class MyCanvas extends Canvas {

	private static final float NORMALIZATION_FACTOR_2_BYTES = Short.MAX_VALUE + 1.0f;
	double[] magnitudes = null;

	Point currentCenter;
	Dimension canvasSize;
	final int RADIUS = 20;
	int height = 10;
	int dy = 10;
	final int MAX = 2000;

	// frequency heights
	int[] heights = new int[7];

	public MyCanvas() {
		setBackground(Color.WHITE);
		currentCenter = new Point(-20, -20);
		addMouseListener(new HandleMouse());
	}

	class HandleMouse extends MouseAdapter implements Runnable {

		Thread circleThread = null;

		public void mousePressed(MouseEvent e) {
			circleThread = new Thread(this);
			circleThread.start();
		}

		public void run() {
			canvasSize = getSize();

			// microphone code sourced from
			// https://stackoverflow.com/questions/53997426/java-how-to-get-current-frequency-of-audio-input

			while (true) {

				// use only 1 channel, to make this easier
				final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100,
						false);
				final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine targetLine = null;
				try {
					targetLine = (TargetDataLine) AudioSystem.getLine(info);
				} catch (LineUnavailableException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					targetLine.open();
				} catch (LineUnavailableException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				targetLine.start();
				final AudioInputStream audioStream = new AudioInputStream(targetLine);

				final byte[] buf = new byte[1024]; // <--- increase this for
													// higher frequency
													// resolution
				final int numberOfSamples = buf.length / format.getFrameSize();
				final JavaFFT fft = new JavaFFT(numberOfSamples);
				// in real impl, don't just ignore how many bytes you read
				try {
					audioStream.read(buf);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// the stream represents each sample as two bytes -> decode
				final float[] samples = decode(buf, format);
				final float[][] transformed = fft.transform(samples);
				final float[] realPart = transformed[0];
				final float[] imaginaryPart = transformed[1];

				magnitudes = toMagnitudes(realPart, imaginaryPart);

				// clear magnitudes
				// for (int i = 0; i < heights.length; i++) {
				// heights[i] = 0;
				// }

				int max_index = -1;
				// sort magnitudes
				// get max frequency
				int frequency = 0;
				double max_magnitude = -1;
				for (int i = 1; i < magnitudes.length / 2 - 1; i++) {
					if (magnitudes[i] > max_magnitude) {
						max_index = i;
						max_magnitude = magnitudes[i];
					}
				}
				System.out.println(max_magnitude);

				frequency = max_index * 44100 / 1024;
				// System.out.println(frequency);

				// decrease magnitude as time goes by
				for (int i = 0; i < heights.length; i++) {
					if (heights[i] > 5) {
						heights[i] -= 20;
					}
				}

				// sort frequencies
				if (frequency >= 20 && frequency <= 60) {
					heights[0] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 60 && frequency <= 250) {
					heights[1] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 250 && frequency <= 500) {
					heights[2] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 500 && frequency <= 2000) {
					heights[3] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 2000 && frequency <= 4000) {
					heights[4] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 4000 && frequency <= 6000) {
					heights[5] = (int) (magnitudes[max_index] / 1);
				} else if (frequency > 6000 && frequency <= 20000) {
					heights[6] = (int) (magnitudes[max_index] / 5);
				}

				repaint();

				try {
					Thread.sleep(10);
				} catch (InterruptedException evt) {
				}

				try {
					audioStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	private void drawFillCircle(Graphics g, Color c, Point center, int radius) {
		g.setColor(c);
		int leftX = center.x - radius;
		int leftY = center.y - radius;
		g.fillOval(leftX, leftY, 2 * radius, 2 * radius);
	}

	public void paint(Graphics g) {

		g.setColor(Color.RED);
		g.fill3DRect(10, (475 - heights[0]), 40, heights[0], true);
		g.setColor(Color.ORANGE);
		g.fill3DRect(50, (475 - heights[1]), 40, heights[1], true);
		g.setColor(Color.YELLOW);
		g.fill3DRect(90, (475 - heights[2]), 40, heights[2], true);
		g.setColor(Color.GREEN);
		g.fill3DRect(130, (475 - heights[3]), 40, heights[3], true);
		g.setColor(Color.CYAN);
		g.fill3DRect(170, (475 - heights[4]), 40, heights[4], true);
		g.setColor(Color.BLUE);
		g.fill3DRect(210, (475 - heights[5]), 40, heights[5], true);
		g.setColor(Color.MAGENTA);
		g.fill3DRect(250, (475 - heights[6]), 40, heights[6], true);
		// g.setColor(Color.PINK);
		// g.fill3DRect(290, (475 - heights[7]), 40, heights[7], true);

	}

	public void update(Graphics g) {
		g.clearRect(0, 0, 350, MAX);

		// change
		/*
		 * System.out.println("hello");
		 * 
		 * // set power System.out.println(height); if (height + dy >= 150 ||
		 * height + dy <= 0) dy = -dy; height += dy;
		 */
		paint(g);
	}

	///////////////////////////////////////// Microphone
	///////////////////////////////////////// stuff/////////////////////////
	// return magnitudes
	private static float[] decode(final byte[] buf, final AudioFormat format) {
		final float[] fbuf = new float[buf.length / format.getFrameSize()];
		for (int pos = 0; pos < buf.length; pos += format.getFrameSize()) {
			final int sample = format.isBigEndian() ? byteToIntBigEndian(buf, pos, format.getFrameSize())
					: byteToIntLittleEndian(buf, pos, format.getFrameSize());
			// normalize to [0,1] (not strictly necessary, but makes things
			// easier)
			fbuf[pos / format.getFrameSize()] = sample / NORMALIZATION_FACTOR_2_BYTES;
		}
		return fbuf;
	}

	private static double[] toMagnitudes(final float[] realPart, final float[] imaginaryPart) {
		final double[] powers = new double[realPart.length / 2];
		for (int i = 0; i < powers.length; i++) {
			powers[i] = Math.sqrt(realPart[i] * realPart[i] + imaginaryPart[i] * imaginaryPart[i]);
		}
		return powers;
	}

	private static int byteToIntLittleEndian(final byte[] buf, final int offset, final int bytesPerSample) {
		int sample = 0;
		for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
			final int aByte = buf[offset + byteIndex] & 0xff;
			sample += aByte << 8 * (byteIndex);
		}
		return sample;
	}

	private static int byteToIntBigEndian(final byte[] buf, final int offset, final int bytesPerSample) {
		int sample = 0;
		for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
			final int aByte = buf[offset + byteIndex] & 0xff;
			sample += aByte << (8 * (bytesPerSample - byteIndex - 1));
		}
		return sample;
	}

}
