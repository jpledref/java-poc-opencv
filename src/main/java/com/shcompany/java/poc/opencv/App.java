package com.shcompany.java.poc.opencv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Console;
import java.io.File;
import java.net.URISyntaxException;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import com.enums.Extension;
import com.utils.Utils;
import javax.swing.*;

public class App {

	// region Properties
	public static Mat frame = null;
	private static HttpStreamServer httpStreamService;
	static VideoCapture videoCapture;
	static Timer tmrVideoProcess;
	// endregion
	
	public static void main(String[] args) throws InterruptedException {
		// Prepare java.library.path
		// Copy over opencv_javaXXX if not present
		prepareLib();

		Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
		System.out.println("mat = " + mat.dump());

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
		if (!videoCapture.isOpened()) {
			return;
		}

		frame = new Mat();

		httpStreamService = new HttpStreamServer(frame);
		new Thread(httpStreamService).start();

		tmrVideoProcess = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!videoCapture.read(frame)) {
					tmrVideoProcess.stop();
				}
				//Include face recognition
				frame = onCameraFrame(frame);
				// procesed image
				httpStreamService.imag = frame;
			}
		});
		tmrVideoProcess.start();
	}

	public static Mat onCameraFrame(Mat inputFrame) // Called by framework with latest frame
	{
		try {
			String haarPath = FSProvider.getInstance().getCurrentPathNormalizedPath() + File.separator
					+ "haarcascade_frontalface_alt.xml";
			CascadeClassifier cascade = new CascadeClassifier(haarPath);

			MatOfRect faceDetections = new MatOfRect();
			cascade.detectMultiScale(inputFrame, faceDetections);

			System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));

			for (Rect rect : faceDetections.toArray()) {
				Imgproc.rectangle(inputFrame, new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
			}

			// Save the visualized detection.
			String filename = "faceDetection.png";
			System.out.println(String.format("Writing %s", filename));
			Imgcodecs.imwrite(filename, inputFrame);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return inputFrame;

	}

}
