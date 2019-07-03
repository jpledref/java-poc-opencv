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
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
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

		tmrVideoProcess = new Timer(200, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!videoCapture.read(frame)) {					
					tmrVideoProcess.stop();
				}
				
				//Include face recognition
				onCameraFrame(frame);
				
				//Processed image push to streaming flow
				httpStreamService.imag = frame;	
			}
		});
		tmrVideoProcess.start();
	}

	//TODO Rework of this part is needed to compress some code
	public static void onCameraFrame(Mat inputFrame){
		// Called by framework with latest frame
		try {
			//String catHaarPath = FSProvider.getInstance().getCurrentPathNormalizedPath() + File.separator+"haarcascade_frontalcatface_extended.xml";
			String faceHaarPath = FSProvider.getInstance().getCurrentPathNormalizedPath() + File.separator+"haarcascade_frontalface_alt.xml";
			//CascadeClassifier cascadeCat = new CascadeClassifier(catHaarPath);
			CascadeClassifier cascadeFace = new CascadeClassifier(faceHaarPath);
			
			//## Detection			
			//CAT
			/*MatOfRect catFaceDetections = new MatOfRect();
			if(!inputFrame.empty())
				cascadeCat.detectMultiScale(inputFrame, catFaceDetections);*/
			//FACE
			MatOfRect faceDetections = new MatOfRect();
			if(!inputFrame.empty())
				cascadeFace.detectMultiScale(inputFrame, faceDetections);
			
			//## Frame results			
			/*for (Rect rect : catFaceDetections.toArray()) {
				Imgproc.rectangle(inputFrame, new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
				String catTxt="cat";//"("+rect.x+","+rect.y+")";
				Imgproc.putText(inputFrame, catTxt, new Point(rect.x, rect.y), Font.BOLD, 2, new Scalar(0, 0, 255));
			}*/
			for (Rect rect : faceDetections.toArray()) {
				Imgproc.rectangle(inputFrame, new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0));
				String faceTxt="face";//"("+rect.x+","+rect.y+")";
				Imgproc.putText(inputFrame, faceTxt, new Point(rect.x, rect.y), Font.BOLD, 2, new Scalar(0, 0, 255));
			}			
			
			//## Output
			StringBuilder strOutput=new StringBuilder("Detected: ");
			/*if(catFaceDetections.toArray().length>0) {
				strOutput.append(String.format("\n - %s cats", catFaceDetections.toArray().length));
			}*/
			if(faceDetections.toArray().length>0) {
				strOutput.append(String.format("\n - %s faces", faceDetections.toArray().length));
			}
			//if(catFaceDetections.toArray().length>0||faceDetections.toArray().length>0) {
			if(faceDetections.toArray().length>0) {
				System.out.println(strOutput);
			}			
			
			//Dirty Fix for OpenCV memory leak
			System.gc();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
