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
	final int MAX = 500;

	// frequency heights
	int[] heights = new int[8];

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

				final byte[] buf = new byte[256]; // <--- increase this for higher frequency resolution
				final int numberOfSamples = buf.length / format.getFrameSize();
				final JavaFFT fft = new JavaFFT(numberOfSamples);
				// while (true) {
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

				/*
				 * AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true); try {
				 * TargetDataLine microphone = AudioSystem.getTargetDataLine(format); } catch
				 * (LineUnavailableException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */

				// for (double magnitude : microphone.getMagnitudes()) {
				// System.out.println(magnitude);
				// }

				// clear magnitudes
				for (int i = 0; i < heights.length; i++) {
					heights[i] = 0;
				}

				// sort magnitudes

				for (double magnitude : magnitudes) {
					System.out.println(magnitude);
					if (magnitude >= 0 && magnitude <= 1) {
						heights[0] += 5;
					} else if (magnitude > 1 && magnitude <= 2) {
						heights[1] += 5;
					} else if (magnitude > 2 && magnitude <= 3) {
						heights[2] += 5;
					} else if (magnitude > 4 && magnitude <= 6) {
						heights[3] += 2;
					} else if (magnitude > 6 && magnitude <= 8) {
						heights[4] += 2;
					} else if (magnitude > 8 && magnitude <= 12) {
						heights[5] += 2;
					} else if (magnitude > 12 && magnitude <= 20) {
						heights[6] += 2;
					} else if (magnitude > 20) {
						heights[7] += 2;
					}
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

		for (double i : heights) {
			// System.out.println(i);
		}

		g.drawRect(10, 50, 20, heights[0]);
		g.drawRect(30, 50, 20, heights[1]);
		g.drawRect(50, 50, 20, heights[2]);
		g.drawRect(70, 50, 20, heights[3]);
		g.drawRect(90, 50, 20, heights[4]);
		g.drawRect(110, 50, 20, heights[5]);
		g.drawRect(130, 50, 20, heights[6]);
		g.drawRect(150, 50, 20, heights[7]);

	}

	public void update(Graphics g) {
		g.clearRect(10, 50, 21, MAX);
		g.clearRect(30, 50, 21, MAX);
		g.clearRect(50, 50, 21, MAX);
		g.clearRect(70, 50, 21, MAX);
		g.clearRect(90, 50, 21, MAX);
		g.clearRect(110, 50, 21, MAX);
		g.clearRect(130, 50, 21, MAX);
		g.clearRect(150, 50, 21, MAX);

		// change
		/*
		 * System.out.println("hello");
		 * 
		 * // set power System.out.println(height); if (height + dy >= 150 || height +
		 * dy <= 0) dy = -dy; height += dy;
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
			// normalize to [0,1] (not strictly necessary, but makes things easier)
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
