package spacegraph.slam;

import boofcv.abst.fiducial.CalibrationFiducialDetector;
import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.MousePauseHelper;
import boofcv.gui.PanelGridPanel;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The 6-DOF pose of calibration targets can be estimated very accurately[*] once a camera has been calibrated.
 * In this example the high level FiducialDetector interface is used with a chessboard calibration target to
 * process a video sequence. Once the pose of the target is known the location of each calibration point is
 * found in the camera frame and visualized.
 *
 * [*] Accuracy is dependent on a variety of factors. Calibration targets are primarily designed to be viewed up close
 * and their accuracy drops with range, as can be seen in this example.
 *
 * @author Peter Abeles
 */
public class ExamplePoseOfCalibrationTarget {

	public static void main( String args[] ) {

		// Load camera calibration
		CameraPinholeRadial intrinsic =
				//CalibrationIO.load(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/intrinsic.yaml"));
				ExampleStereoTwoViewsOneCamera.intrinsic;
		LensDistortionNarrowFOV lensDistortion = new LensDistortionRadialTangential(intrinsic);

		// load the video file
//		String fileName = UtilIO.pathExample("tracking/chessboard_SonyDSC_01.mjpeg");
//		SimpleImageSequence<GrayF32> video =
//				DefaultMediaManager.INSTANCE.openVideo(fileName, ImageType.single(GrayF32.class));
		SimpleImageSequence<GrayF32> cam = DefaultMediaManager.INSTANCE.openCamera(null, 640, 480, ImageType.single(GrayF32.class));

		// Let's use the FiducialDetector interface since it is much easier than coding up
		// the entire thing ourselves.  Look at FiducialDetector's code if you want to understand how it works.
		CalibrationFiducialDetector<GrayF32> detector =
				FactoryFiducial.calibChessboard(new ConfigChessboard(4, 5, 0.03),GrayF32.class);

		detector.setLensDistortion(lensDistortion,intrinsic.width,intrinsic.height);

		// Get the 2D coordinate of calibration points for visualization purposes
		List<Point2D_F64> calibPts = detector.getCalibrationPoints();

		// Set up visualization
		PointCloudViewer viewer = new PointCloudViewer(intrinsic, 0.01);
		// make the view more interest.  From the side.
		DMatrixRMaj rotY = ConvertRotation3D_F64.rotY(-Math.PI/2.0,null);
		viewer.setWorldToCamera(new Se3_F64(rotY,new Vector3D_F64(0.75,0,1.25)));
		ImagePanel imagePanel = new ImagePanel(intrinsic.width, intrinsic.height);
		viewer.setPreferredSize(new Dimension(intrinsic.width,intrinsic.height));
		PanelGridPanel gui = new PanelGridPanel(1,imagePanel,viewer);
		gui.setMaximumSize(gui.getPreferredSize());
		ShowImages.showWindow(gui,"Calibration Target Pose",true);

		// Allows the user to click on the image and pause
		MousePauseHelper pauseHelper = new MousePauseHelper(gui);

		// saves the target's center location
		List<Point3D_F64> path = new ArrayList<>();

		// Process each frame in the video sequence
		Se3_F64 targetToCamera = new Se3_F64();
		while( true /*video.hasNext() */ ) {

			// detect calibration points
			//detector.detect(video.next());
			detector.detect( cam.next());

			if( detector.totalFound() == 1 ) {
				detector.getFiducialToCamera(0, targetToCamera);

				// Visualization.  Show a path with green points and the calibration points in black
				viewer.reset();

				Point3D_F64 center = new Point3D_F64();
				SePointOps_F64.transform(targetToCamera, center, center);
				path.add(center);

				for (Point3D_F64 p : path) {
					viewer.addPoint(p.x, p.y, p.z, 0x00FF00);
				}

				for (int j = 0; j < calibPts.size(); j++) {
					Point2D_F64 p = calibPts.get(j);
					Point3D_F64 p3 = new Point3D_F64(p.x, p.y, 0);
					SePointOps_F64.transform(targetToCamera, p3, p3);
					viewer.addPoint(p3.x, p3.y, p3.z, 0);
				}
			}

			imagePanel.setImage(cam.getGuiImage());
			viewer.repaint();
			imagePanel.repaint();

//			BoofMiscOps.pause(30);
//			while( pauseHelper.isPaused() ) {
//				BoofMiscOps.pause(30);
//			}
		}
	}
}