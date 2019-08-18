package com.shcompany.java.poc.opencv;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enums.Extension;
import com.utils.Utils;
import javax.swing.*;

//## OpenCV Memory Leak
//Something is wrong with opencv in linux, after a few minutes cumulated app memory exceed the maximum java heap size.
//There must be a memory leak into detectMultiScale() method. (org.opencv.objdetect.CascadeClassifier.detectMultiScale(Mat image, MatOfRect objects))
//For now, this call of Garbage collector is a dirty fix.

//## Code snippet to save file ## 
//SimpleDateFormat dt = new SimpleDateFormat("yyyyMMdd_hhmmss");
//String filename = dt.format(new Date())+"_faceDetection.png";
//Imgcodecs.imwrite(filename, inputFrame);
public class App {
	static final Logger LOG = LoggerFactory.getLogger(App.class);

	// region Properties
	public static Mat frame = null;
	public static Mat frameSh = null;
	private static HttpStreamServer httpStreamService;
	static VideoCapture videoCapture;
	static Timer tmrVideoProcess;
	static int absoluteFaceSize;
	static MatOfRect faceDetections;
	// endregion

	public static void main(String[] args) throws InterruptedException {
		// Prepare java.library.path
		// Copy over opencv_javaXXX if not present
		prepareLib();

		// Start Streaming to port 8085
		startStream();
	}

	private static void prepareLib() {
		try {
			System.load(Core.NATIVE_LIBRARY_NAME);
			return;
		} catch (UnsatisfiedLinkError e) {
			// NOOP
		}

		// Set extension
		String ext = Extension.NIX.getLabel();
		if (Utils.isWindows())
			ext = Extension.WIN.getLabel();

		// Set a fixed path for opencv_javaXXX
		String fixedLD = null;
		File f = FSProvider.getInstance().getCurrentPathNormalizedFile();
		fixedLD = f.getPath() + File.separator + Core.NATIVE_LIBRARY_NAME + ext;

		if (!new File(fixedLD).exists()) {
			copyConf();
		}

		// Load opencv_javaXXX library
		System.load(fixedLD);
	}

	private static void copyConf() {
		// Check the presence of LD directory
		String dst = "";
		File f = new File(dst);
		if (!f.exists()) {
			String src = "LD/";
			FSProvider.getInstance().extractFile(src, dst);
		}
	}

	public static void startStream() {
		videoCapture = new VideoCapture();

		videoCapture.open(0);

		videoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
		videoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
		videoCapture.set(Videoio.CAP_PROP_FPS, 10);

		if (!videoCapture.isOpened()) {
			return;
		}

		frame = new Mat();
		httpStreamService = new HttpStreamServer(frame);
		new Thread(httpStreamService).start();

		Thread thread = new Thread() {
			public void run() {
				while (true) {
					long start = System.currentTimeMillis();

					try {
						videoCapture.read(frame);

						if (frameSh == null) {
							for (Rect rect : faceDetections.toArray()) {
								/*
								 * Imgproc.rectangle(inputFrame, new Point(rect.x, rect.y), new Point(rect.x +
								 * rect.width, rect.y + rect.height), new Scalar(255, 0, 0),2);
								 */
								Imgproc.rectangle(frame, new Point(rect.x, rect.y),
										new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 2);
								String faceTxt = "face";// "("+rect.x+","+rect.y+")";
								Imgproc.putText(frame, faceTxt, new Point(rect.x, rect.y), Font.BOLD, 2,
										new Scalar(255, 0, 0), 2);
							}

							httpStreamService.imag = frame;

						}
					} catch (Exception e) {
						// TODO: handle exception
					}

					long finish = System.currentTimeMillis();
					long timeElapsed = finish - start;
					LOG.debug("a:" + timeElapsed + "ms");

				}
			}
		};
		thread.start();

		Thread thread2 = new Thread() {
			public void run() {
				while (true) {
					long start = System.currentTimeMillis();

					try {
						// frameSh=frame.clone();
						frameSh = null;
						onCameraFrame(frame);
						httpStreamService.imag = frameSh;
					} catch (Exception e) {
						// TODO: handle exception
					}

					long finish = System.currentTimeMillis();
					long timeElapsed = finish - start;
					LOG.info("b:" + timeElapsed + "ms");
				}
			}
		};
		thread2.start();
	}

	// TODO Rework of this part is needed to compress some code
	public static void onCameraFrame(Mat inputFrame) {
		// Called by framework with latest frame
		try {
			Mat grayFrame = inputFrame.clone();
			// convert the frame in gray scale
			Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
			// equalize the frame histogram to improve the result
			Imgproc.equalizeHist(grayFrame, grayFrame);

			// set minimum size of the face
			if (absoluteFaceSize == 0) {
				int height = grayFrame.rows();
				if (Math.round(height * 0.05f) > 0) {
					absoluteFaceSize = Math.round(height * 0.05f);
				}
			}
			
			String faceHaarPath = FSProvider.getInstance().getCurrentPathNormalizedPath() + File.separator
					+ "haarcascade_frontalface_alt2.xml";
			CascadeClassifier cascadeFace = new CascadeClassifier(faceHaarPath);

			// ## Detection			
			// FACE
			faceDetections = new MatOfRect();
			if (!inputFrame.empty())
				cascadeFace.detectMultiScale(grayFrame, faceDetections, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
						new Size(absoluteFaceSize, absoluteFaceSize), new Size());			

			// ## Frame results			
			for (Rect rect : faceDetections.toArray()) {
				Imgproc.rectangle(inputFrame, new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 2);
				String faceTxt = "face";// "("+rect.x+","+rect.y+")";
				Imgproc.putText(inputFrame, faceTxt, new Point(rect.x, rect.y), Font.BOLD, 2, new Scalar(255, 0, 0), 2);
			}

			// ## Output
			StringBuilder strOutput = new StringBuilder("Detected: ");
			if (faceDetections.toArray().length > 0) {
				strOutput.append(String.format("\n - %s faces", faceDetections.toArray().length));
			}			
			if (faceDetections.toArray().length > 0) {
				LOG.info(strOutput.toString());
			}

			// Dirty Fix for OpenCV memory leak
			System.gc();

			// Imgproc.resize(grayFrame, grayFrame, new Size(320,240));
			frameSh = inputFrame;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
