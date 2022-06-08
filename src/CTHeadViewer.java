import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.*;


public class CTHeadViewer extends Application {

	/* Initialisation */

	// CTHead Data Set
	short ctHeadRawDataSet[][][]; // 3D Volume data set
	float ctHeadImageDataSet[][][]; // 3D volume data set converted to normalised 0-1 values (copiable to image)
	short ctHeadRawDataSetMinValue; // Min value in the 3D Volume data set
	short ctHeadRawDataSetMaxValue; // Max value in the 3D Volume data set

	// Main image display
	ImageView topDownImageView;
	Image topDownImage;

	// Functionality status values
	static double gammaCorrectionMultiplier = 1.0; // Current gamma correction value
	static double imageResizeSliderDoubleValue = 256.0; // Main view image dimensions as decimal
	static int imageResizeSliderIntValue = 256; // Main view image dimensions as integer
	static boolean resizeMode = false;	// Current resize mode (false: nearest neighbour, true: bilinear)
	static int currentSliceNumber = 76; // Current CTHead scan slice to be displayed in the main image view

	// Lookup table
	static float[] gammaLookupTable = new float[256];


	/* GUI Builder */

	@Override
	public void start(Stage stage) throws FileNotFoundException {

		// Set the window title
		stage.setTitle("CThead Viewer");

		// Try to open the file
		try {
			ReadData();
		} catch (IOException e) {
			System.out.println("Error: The CThead file is not in the working directory");
			System.out.println("Working Directory = " + System.getProperty("user.dir"));
			return;
		}

		// Create the image using the current CTHead scan slice image. No need to gamma correct during initialisation
		topDownImage = GetCTScanImageSlice(currentSliceNumber);

		// Create a view of that image
		topDownImageView = new ImageView(topDownImage); // and then see 3. below

		// Resize mode toggle group
		final ToggleGroup resizeModeButtonGroup = new ToggleGroup();

		// Nearest neighbour resize option
		RadioButton nearestNeighbourOption = new RadioButton("Nearest neighbour");
		nearestNeighbourOption.setToggleGroup(resizeModeButtonGroup);

		// Bilinear resize option
		RadioButton bilinearOption = new RadioButton("Bilinear");
		bilinearOption.setToggleGroup(resizeModeButtonGroup);

		// Nearest neighbour by default
		nearestNeighbourOption.setSelected(true);

		// Image resize slider
		// Min: 32, Max: 1024, Default: 256
		Slider imageResizeSlider = new Slider(32, 1024, 256);

		// Gamma correction slider
		// Min: 0.1, Max: 4.0, Default: 1.0
		Slider gammaCorrectionSlider = new Slider(0.1, 4.0, 1.0);


		/* GUI Functionality */

		// Radio button GUI listener - changes between nearest neighbour and bilinear resize modes
		resizeModeButtonGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

			// This function is called every time the slider is moved
			public void changed(ObservableValue<? extends Toggle> ob, Toggle o, Toggle n) {

				// If the user has selected the nearest neighbour resize mode
				if (nearestNeighbourOption.isSelected()) {

					// Update the global resize mode status
					resizeMode = false;

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the nearest neighbour resize method
					// This ensures immediate updating of the image as soon as the resize method is changed
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(nearestNeighbourResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
							                  imageResizeSliderIntValue));

				// Otherwise, if the user has selected the bilinear mode
				} else if (bilinearOption.isSelected()) {

					// Update the global resize mode status
					resizeMode = true;

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the bilinear resize method
					// This ensures immediate updating of the image as soon as the resize method is changed
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(bilinearResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
											  imageResizeSliderIntValue));

				}
			}

		});


		// Image resize slider GUI listener - resizes the main image view's image
		imageResizeSlider.valueProperty().addListener(new ChangeListener<Number>() {

			// This function is called every time the slider is moved
			public void changed(ObservableValue <? extends Number > observable, Number oldValue, Number newValue) {

				// Update the global image dimension status values
				imageResizeSliderDoubleValue = newValue.doubleValue();
				imageResizeSliderIntValue = newValue.intValue();

				// If the user has selected the nearest neighbour resize mode
				if (resizeModeButtonGroup.getSelectedToggle() == nearestNeighbourOption) {

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the nearest neighbour resize method
					// This ensures immediate updating of the image as soon as the resize slider is moved
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(nearestNeighbourResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
											  imageResizeSliderIntValue));

				// Otherwise, if the user has selected the bilinear mode
				} else if (resizeModeButtonGroup.getSelectedToggle() == bilinearOption) {

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the bilinear resize method
					// This ensures immediate updating of the image as soon as the resize slider is moved
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(bilinearResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
											  imageResizeSliderIntValue));

				}
			}
		});


		// Gamma correction slider GUI listener - adjusts gamma on the main image view's image
		gammaCorrectionSlider.valueProperty().addListener(new ChangeListener<Number>() {

			// This function is called every time the slider is moved
			public void changed(ObservableValue <? extends Number > observable, Number oldValue, Number newValue) {

				// Update the global current gamma correction multiplier status value
				gammaCorrectionMultiplier = newValue.doubleValue();

				// Update the gamma correction lookup table re-basing it off the new gamma correction multiplier
				create_GammaLookupTable();

				// If the user has selected the nearest neighbour resize mode
				if (resizeModeButtonGroup.getSelectedToggle() == nearestNeighbourOption) {

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the nearest neighbour resize method
					// This ensures immediate updating of the image as soon as the resize slider is moved
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(nearestNeighbourResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
											  imageResizeSliderIntValue));

				// Otherwise, if the user has selected the bilinear mode
				} else if (resizeModeButtonGroup.getSelectedToggle() == bilinearOption) {

					// Set the image view's image to a fresh copy of the current CTHead scan slice,
					// resized to the target value with the bilinear resize method
					// This ensures immediate updating of the image as soon as the resize slider is moved
					// We get the CTHead scan image via copy of the method that does not gamma correct
					// as the resize method already has hardcoded gamma correction to the global
					// current gamma correction multiplier value.
					// Since we are getting a fresh copy of the slice and not referencing the ImageView's
					// current image, there is no need to clear the ImageView's current image beforehand
					// by setting it to null.
					topDownImageView.setImage(bilinearResize(
											  GetCTScanImageSlice(currentSliceNumber),
											  imageResizeSliderDoubleValue,
											  imageResizeSliderIntValue));

				}
			}
		});


		// Add everything to the GUI
		VBox guiRootFrame = new VBox(); // Main image view layout

		// Add all the GUI elements
		guiRootFrame.getChildren().addAll(nearestNeighbourOption,	// Nearest neighbour resize mode option
										  bilinearOption, 			// Bilinear resize mode option
										  gammaCorrectionSlider, 	// Gamma correction slider
										  imageResizeSlider, 		// Image resize slider
										  topDownImageView);		// Main image view



		// Display the GUI elements onto the scene
		Scene scene = new Scene(guiRootFrame, 1024, 768);	// Main image view
		stage.setScene(scene);										// Set the stage's scene
		stage.setX(100);											// Position it at the left of the screen
		stage.setY(100);											// Position it at the left of the screen
		stage.show();												// Show the stage


		// Create the thumbnail window positioned to the right of the main image view
		ThumbWindow(stage.getX()+1050, stage.getY()+100);

		// Initialise the gamma lookup table so that if the slice is changed on the default gamma it works fine
		create_GammaLookupTable();


	}


	// Gets a specific slice (zero based index) from the CTHead scan data set and returns it as a 256x256 image
	public Image GetCTScanImageSlice(int sliceNumber) {

		// Create a writeable image of the same dimensions as the image stored in the CTHead data set
		WritableImage ctScanSliceImage = new WritableImage(256, 256);

		// Find the width and height of the image to be processed
		int imageWidth = (int)ctScanSliceImage.getWidth();
		int imageHeight = (int)ctScanSliceImage.getHeight();
		float colorValue;

		// Get an interface to write to that image in memory
		PixelWriter ctScanSliceImageWriter = ctScanSliceImage.getPixelWriter();

		// Iterate over the coordinates of all pixels in the image by height and width
		// For each column from the left, for each row in that column:
		/* ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 */

		for (int y = 0; y < imageHeight; y++) {

			for (int x = 0; x < imageWidth; x++) {

				// For each pixel, get the pixel's colour from the specified CTHead slice number in the normalised data set
				colorValue = ctHeadImageDataSet[sliceNumber][y][x];

				// Create a greyscale color object from this colour value
				// Since it's greyscale, all of RGB uses the same value
				Color color = Color.color(colorValue,colorValue,colorValue);

				// Set the colour of the pixel at the current coordinate iteration point to this colour
				ctScanSliceImageWriter.setColor(x, y, color);
			}
		}
		return ctScanSliceImage;
	}


	// Function to resize a square image with the nearest neighbour algorithm to a specific dimension
	public Image nearestNeighbourResize(Image originalImage, double doubleResizeDimensions, int intResizeDimensions) {

		// Get the dimensions of the original image
		double originalImageWidth = originalImage.getWidth();
		double originalImageHeight = originalImage.getHeight();

		// Create a the new resized image to be written to
		WritableImage newImage = new WritableImage(intResizeDimensions, intResizeDimensions);

		// Get an interface to write to it
		PixelWriter newImageWriter = newImage.getPixelWriter();

		// Get an interface to read from the original image's memory
		PixelReader originalImageReader = originalImage.getPixelReader();

		// Algorithm to resize the thumbnail images to the desired size.
		// Iterates over the range of pixels of the desired image size and copies all colours from the original
		// thumbnail size, scaled down
		// Uses the dimensions of the new image as integers for loop conditions
		// For each column from the left, for each row in that column:
		/* ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 */
		for (int j = 0; j < intResizeDimensions; j++) {
			for (int i = 0; i < intResizeDimensions; i++) {

				// Scale up the pixels from the original image size to the new image size
				// This means that at this point we are iterating over the original image's pixels... *
				double pixelYCoord = j * originalImageHeight / doubleResizeDimensions;
				double pixelXCoord = i * originalImageWidth / doubleResizeDimensions;
									// 	int: orig image dimensions / double: new image dimensions as double

				// * ...grabbing the colour from there... *
				// Get the pixel color value from the dataset at the scaled up coordinate
				Color colorToBeSet = originalImageReader.getColor((int)pixelYCoord, (int)pixelXCoord);

				// Apply gamma correction
				// Lookup the gamma correction table
				float newColour = lookup_GammaLookupTable((float)colorToBeSet.getRed());
				Color gammaCorrectedColorToBeSet = Color.color(newColour,newColour,newColour);

				// * ...and setting that pixel colour at the new scaled position
				newImageWriter.setColor(j, i, gammaCorrectedColorToBeSet);
			}
		}
		return newImage;
	}


	// Function to perform linear interpolation at a specific position between two points on the X axis,
	// given the positions of all three points and the colours of the boundary points
	//
	//		V1 ------------------------------------------ V ------------------------------------------ V2
	//		boundary point								  |										  boundary point
	//													  |
	//													  |
	//													  |
	//			centre point (we need to calculate the colour of this point based on V1 and V2)
	//
	public static Color calculatelinearInterpX(Color colorV1, Color colorV2, double v1x, double v2x, double vx) {

		// Get the color at point V1
		double colorV1Red = colorV1.getRed();

		// Get the color at point V2
		double colorV2Red = colorV2.getRed();

		// Subtract the color at V2 from the colour at V1 *
		double newColorV1Red = colorV2Red - colorV1Red;

		// Last part of the linear interpolation equation:
		// Subtract the coordinates of V1 from V, then divide
		// it by the coordinates V2 subtract the coordinates of V1
		//
		// (v-v1)
		// -------
		// (v2-v1)
		//
		double positionMultiplier = (vx - v1x) / (v2x - v1x);

		// Multiply that by the color (v2-v1) *
		newColorV1Red = newColorV1Red * positionMultiplier;

		// Add the original colour of V1 onto this all
		newColorV1Red = colorV1Red + newColorV1Red;

		// Make a greyscale colour out of this calculated colour value
		Color finalColour = Color.color(newColorV1Red, newColorV1Red, newColorV1Red);

		return finalColour;
	}


	// Function to perform linear interpolation at a specific position between two points on the Y axis,
	// given the positions of all three points and the colours of the boundary points
	//
	//		V2		boundary point
	//		|
	//		V		centre point (we need to calculate the colour of this point based on V1 and V2)
	//		|
	//		V1		boundary point
	//
	public static Color calculatelinearInterpY(Color colorV1, Color colorV2, double v1y, double v2y, double vy) {

		// Get the color at point V1
		double colorV1Red = colorV1.getRed();

		// Get the color at point V2
		double colorV2Red = colorV2.getRed();

		// Subtract the color at V2 from the colour at V1 *
		double newColorV1Red = colorV2Red - colorV1Red;

		// Last part of the linear interpolation equation:
		// Subtract the coordinates of V1 from V, then divide
		// it by the coordinates V2 subtract the coordinates of V1
		//
		// (v-v1)
		// -------
		// (v2-v1)
		//
		double positionMultiplier = (vy - v1y) / (v2y - v1y);

		// Multiply that by the color (v2-v1) *
		newColorV1Red = newColorV1Red * positionMultiplier;

		// Add the original colour of V1 onto this all
		newColorV1Red = colorV1Red + newColorV1Red;

		// Make a greyscale colour out of this calculated colour value
		Color finalColour = Color.color(newColorV1Red, newColorV1Red, newColorV1Red);

		return finalColour;
	}


	// Function to resize a square image with the bilinear interpolation algorithm to a specific dimension
	public Image bilinearResize(Image originalImage, double doubleResizeDimensions, int intResizeDimensions) {

		// Get the required dimensions
		double originalImageWidth = originalImage.getWidth();
		double originalImageHeight = originalImage.getHeight();

		// Create a new image with the desired dimensions to be written to
		WritableImage newImage = new WritableImage(intResizeDimensions, intResizeDimensions);

		// Get an interface to write to it
		PixelWriter newImageWriter = newImage.getPixelWriter();

		// Read in the bytes / pixels from the original image
		PixelReader originalImageReader = originalImage.getPixelReader();

		// Algorithm to resize the thumbnail images to the desired size.
		// Iterates over the range of pixels of the desired image size and copies all colours from the original
		// thumbnail size, scaled down
		// Uses the dimensions of the new image as integers for loop conditions
		// For each column from the left, for each row in that column:
		/* ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 */
		for (int j = 0; j < intResizeDimensions; j++) {
			for (int i = 0; i < intResizeDimensions; i++) {

				//		We want to find the colour at G
				//
				//		B ------------- F ----- C
				//		|				|		|
				//		|				G		|
				//		|				|		|
				//		|				|		|
				//		|				|		|
				//		|				|		|
				//		A ------------- E ----- D
				//

				// Scale up the pixels from the original image size to the new image size
				// This means that at this point we are iterating over the original image's pixels...
				double gx = i * originalImageWidth / doubleResizeDimensions;
				double gy = j * originalImageHeight / doubleResizeDimensions;
							// 	int: orig image dimensions / double: new image dimensions as double


				// Calculate the coordinates of A, B, C, D by flooring or ceiling the coordinates of G.
				// G could be between two integers, which is why we do this. e.g.,:
				// E = 3.0, G = 3.7, F = 4.0
				// Instead of ceiling, we floor the value with 1 added, to avoid the values becoming exactly the same
				// due to floating point precision flaws. If we don't do this, two coordinates may become exactly the
				// same, so subtracting them results in 0, which can result in division by zero errors further down
				// the equation, which produces full black colours.

				double ax = Math.floor(gx);
				double ay = Math.floor(gy);

				double bx = Math.floor(gx);
				double by = Math.floor(gy)+1;

				double cx = Math.floor(gx)+1;
				double cy = Math.floor(gy)+1;

				double dx = Math.floor(gx)+1;
				double dy = Math.floor(gy);


				// Calculate the coordinates of A, B, C, D by flooring or ceiling the relevant coordinates of G.
				// Given that F and E will be at the same point on the X axis as G, we only modify the Y coordinates.
				double fx = gx;
				double fy = Math.floor(gy)+1;

				double ex = gx;
				double ey = Math.floor(gy);


				// If we go out of bounds of the image, write the nearest pixel beforehand (to avoid a black outline).
				// This avoids out of bounds errors.
				if (ax > 255.0) {
					ax = 254.0;
				}

				if (ay > 255.0) {
					ay = 254.0;
				}

				if (bx > 255.0) {
					bx = 254.0;
				}

				if (by > 255.0) {
					by = 254.0;
				}

				if (cx > 255.0) {
					cx = 254.0;
				}

				if (cy > 255.0) {
					cy = 254.0;
				}

				if (dx > 255.0) {
					dx = 254.0;
				}

				if (dy > 255.0) {
					dy = 254.0;
				}

				if (fx > 255.0) {
					fx = 254.0;
				}

				if (fy > 255.0) {
					fy = 254.0;
				}

				if (ex > 255.0) {
					ex = 254.0;
				}

				if (ey > 255.0) {
					ey = 254.0;
				}


				if (gx > 255.0) {
					gx = 254.0;
				}

				if (gy > 255.0) {
					gy = 254.0;
				}


				// Calculate the colour at position F with linear interpolation using the colour
				// at points B and C - since the positions of these only differ on the X axis
				// we use X axis / horizontal linear interpolation
				Color f = calculatelinearInterpX(originalImageReader.getColor((int)bx, (int)by),
												 originalImageReader.getColor((int)cx, (int)cy),
												 bx, cx, fx);


				// Calculate the colour at position E with linear interpolation using the colour
				// at points A and D - since the positions of these only differ on the X axis
				// we use X axis / horizontal linear interpolation
				Color e = calculatelinearInterpX(originalImageReader.getColor((int)ax, (int)ay),
												 originalImageReader.getColor((int)dx, (int)dy),
												 ax, dx, ex);


				// Calculate the colour at position E with linear interpolation using the colours
				// at points E and F that we calculated just above - since the positions of these
				// only differ on the Y axis we use Y axis / vertical linear interpolation
				Color g = calculatelinearInterpY(e,
												 f,
												 ey, fy, gy);

				// Apply gamma correction
				// Lookup the gamma correction table
				float newColour = lookup_GammaLookupTable((float)g.getRed());
				Color gammaCorrectedColorToBeSet = Color.color(newColour,newColour,newColour);

				// Set the pixel colour at the new scaled position
				newImageWriter.setColor(i, j, gammaCorrectedColorToBeSet);

			}
		}
		return newImage;
	}


	// Internal functionality provided in the assignment's supporting framework: read in CTHead data set
	public void ReadData() throws IOException {

		// File name is hardcoded here - much nicer to have a dialog to select it and capture the size from the user
		File file = new File("CThead");

		// Read the data quickly via a buffer (in C++ you can just do a single fread - I couldn't find the equivalent in Java)
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

		// loop through the 3D data set
		int i;
		int j;
		int k;

		// set to extreme values
		ctHeadRawDataSetMinValue = Short.MAX_VALUE; ctHeadRawDataSetMaxValue = Short.MIN_VALUE;

		// value read in
		short read;

		// data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around
		int b1;
		int b2;

		// allocate the memory - note this is fixed for this data set
		ctHeadRawDataSet = new short[113][256][256];
		ctHeadImageDataSet = new float[113][256][256];

		// loop through the data reading it in
		for (k = 0; k<113; k++) {

			for (j = 0; j<256; j++) {

				for (i = 0; i<256; i++) {

					// because the Endianess is wrong, it needs to be read byte at a time and swapped

					// Make unsigned
					// the 0xff is because Java does not have unsigned types (C++ is so much easier!)
					b1 = ((int)in.readByte()) & 0xff;
					b2 = ((int)in.readByte()) & 0xff;

					read = (short)((b2<<8) | b1); // and swizzle the bytes around

					if (read< ctHeadRawDataSetMinValue) ctHeadRawDataSetMinValue = read; // update the minimum
					if (read> ctHeadRawDataSetMaxValue) ctHeadRawDataSetMaxValue = read; // update the maximum

					ctHeadRawDataSet[k][j][i] = read; // put the short into memory (in C++ you can replace all this code with one fread)

				}
			}
		}


//		System.out.println(ctHeadRawDataSetMinValue +" "+ ctHeadRawDataSetMaxValue); // diagnostic - for CThead this should be -1117, 2248
		// (i.e. there are 3366 levels of grey, and now we will normalise them to 0-1 for display purposes

		// Normalise
		// I know the min and max already, so I could have put the normalisation in the above loop, but I put it separate here
		for (k = 0; k<113; k++) {
			for (j = 0; j<256; j++) {
				for (i = 0; i<256; i++) {
					ctHeadImageDataSet[k][j][i] = ((float) ctHeadRawDataSet[k][j][i]-(float) ctHeadRawDataSetMinValue)/((float) ctHeadRawDataSetMaxValue -(float) ctHeadRawDataSetMinValue);
				}
			}
		}

	}



	// Constructor for the thumbnail window - creates the window, Layout, ImageView and Image
	public void ThumbWindow(double atX, double atY) {

		// Layout where the imagine holding all the thumbnails will be
		StackPane ThumbLayout = new StackPane();

		// Metrics the thumbnail image window will base its functionality from
		int noOfThumbnails = 112;
		int noOfRows = 10;
		int noOfColumns = 12;
		int thumbnailSize = 50;
		int gapPixelSize = 10;
		// 11 gaps between columns
		// 9 gaps between rows

		// Resolution: imageWidth=710 x imageHeight=590
		// Calculates the width and height of the image to hold all thumbnails based off
		// the size of each thumbnail, the number of columns and rows in the arrangement,
		// and the size of the gaps between all of the images.
		int imageWidth = (thumbnailSize * noOfColumns) + (gapPixelSize * (noOfColumns - 1));
		int imageHeight = (thumbnailSize * noOfRows) + (gapPixelSize * (noOfRows - 1));

		// Create the image to hold all the thumbnails of this size
		WritableImage thumbnailMasterImage = new WritableImage(imageWidth, imageHeight);

		// Create an ImageView of this image
		ImageView thumbnailMasterImageView = new ImageView(thumbnailMasterImage);

		// Add the ImageView to the window layout
		ThumbLayout.getChildren().add(thumbnailMasterImageView);

		// Get a writeable interface to that image's memory
		PixelWriter thumbnailImageWriter = thumbnailMasterImage.getPixelWriter();

		// Make the image white as a base by iterating over all
		//  of the pixels and setting their colour to white
		for (int y = 0; y < thumbnailMasterImage.getHeight(); y++) {
			for (int x = 0; x < thumbnailMasterImage.getWidth(); x++) {
				// Apply the new colour
				thumbnailImageWriter.setColor(x, y, Color.color(1, 1, 1));
			}
		}

		// Variable denoting the current CTHead data slice image to be displayed in the thumbnail
		int s = 0;

		// Iterate over the image that holds all of the thumbnails
		for (int y = 0; y <= imageHeight; y++) {
			for (int x = 0; x <= imageWidth; x++) {

				thumbnailMasterImage = WriteThumbnailImage(thumbnailMasterImage, x, y, 50, 50, s);

				// Move to the next slice in the CTHead data set
				s++;

				// If we reach the last slice, don't increment any further
				// to prevent going out of bounds of the data set
				if (s > noOfThumbnails) {
					s = noOfThumbnails;
				}

				// If we reach the last thumbnail column slot, pad by 1 pixel to
				// prevent going past the bounds of the image and prevent infinite recursion
				if (x < imageWidth - thumbnailSize) {
					x += thumbnailSize + gapPixelSize - 1;

				// Otherwise, if we are currently writing the last thumbnail column slot, stop incrementing
				// the current pixel position, to avoid overwriting the image over and over causing a black result
				} else {
					break;
				}
			}

			// If we reach the last thumbnail row slot, pad by 1 pixel to
			// prevent going past the bounds of the image and prevent infinite recursion
			if (y < imageHeight - thumbnailSize) {
				y += thumbnailSize + gapPixelSize - 1;

			// Otherwise, if we are currently writing the last thumbnail column slot, stop incrementing
			// the current pixel position, to avoid overwriting the image over and over causing a black result
			} else {
				break;
			}
		}

		// Make the scene for the image holding all the thumbnails to be displayed in based on its dimensions
		Scene ThumbScene = new Scene(ThumbLayout, thumbnailMasterImage.getWidth(), thumbnailMasterImage.getHeight());

		// Add mouse over event handler - this lets the main image window's image change to the currently hovered thumbnail
		thumbnailMasterImageView.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_MOVED, event -> {

			// The below code check the coordinates of the mouse against the positions of the thumbnails

			// Row 0
			// If the cursor Y position is less than the thumbnailSize, it must be in the first row.
			// So we can now check the X position of the cursor against the bounds for each thumbnail in the rows of that row.
			if (event.getY() < thumbnailSize) {
				updateMainViewImageByRow(event.getX(), 0, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 1
			// If the cursor Y position is above than 1 copy of the thumbnailSize + gap size,
			// but less than 2 copies of the thumbnailSize + the gap size (2 rows) it must be in the second row.
			// So we can now check the X position of the cursor against the bounds for each thumbnail in the rows of that row.
			if (event.getY() > (thumbnailSize + gapPixelSize) && event.getY() < (2 * thumbnailSize + gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 1, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 2
			if (event.getY() > 2 * (thumbnailSize + gapPixelSize) && event.getY() < (3 * thumbnailSize + 2 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 2, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 3
			if (event.getY() > 3 * (thumbnailSize + gapPixelSize) && event.getY() < (4 * thumbnailSize + 3 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 3, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 4
			if (event.getY() > 4 * (thumbnailSize + gapPixelSize) && event.getY() < (5 * thumbnailSize + 4 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 4, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 5
			if (event.getY() > 5 * (thumbnailSize + gapPixelSize) && event.getY() < (6 * thumbnailSize + 5 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 5, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 6
			// If the cursor Y position is above than 6 copies of the thumbnailSize + gap size,
			// but less than 7 copies of the thumbnailSize + 6 copies of the gap size (6 rows) it must be in the sixth row.
			// So we can now check the X position of the cursor against the bounds for each thumbnail in the rows of that row.
			if (event.getY() > 6 * (thumbnailSize + gapPixelSize) && event.getY() < (7 * thumbnailSize + 6 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 6, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 7
			if (event.getY() > 7 * (thumbnailSize + gapPixelSize) && event.getY() < (8 * thumbnailSize + 7 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 7, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 8
			if (event.getY() > 8 * (thumbnailSize + gapPixelSize) && event.getY() < (9 * thumbnailSize + 8 * gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 8, false, thumbnailSize, gapPixelSize, noOfColumns);
			}

			// Row 9
			if (event.getY() > 9 * (thumbnailSize + gapPixelSize)) {
				updateMainViewImageByRow(event.getX(), 9, true, thumbnailSize, gapPixelSize, noOfColumns);
			}

			event.consume();


		});

		// Build and display the new window
		Stage newWindow = new Stage();
		newWindow.setTitle("CThead Slices");
		newWindow.setScene(ThumbScene);

		// Set position of second window, related to primary window.
		newWindow.setX(atX);
		newWindow.setY(atY);

		newWindow.show();
	}


	// Writes a thumbnail to another image at a specified position
	public WritableImage WriteThumbnailImage(WritableImage thumbnailMasterImage,	// The image to write the thumbnail to
											 int xCoord,					// The x coordinate of the top left of the thumbnail.
											 int yCoord,					// The y coordinate of the top left of the thumbnail.
											 int desiredThumbnailWidth,		// The width of the thumbnail.
											 int desiredThumbnailHeight,	// The height of the thumbnail.
											 int sliceNumber) {				// The slice index from the CTHead data set

		// The greyscale colour value to be written
		float colorValue;

		// Get an interface to write to the image memory of the main image to write the thumbnail to
		PixelWriter thumbnailImageWriter = thumbnailMasterImage.getPixelWriter();

		// Dimensions of the resulting image from a GetCTScanImageSlice(sliceNumber) call
		double originalThumbImageWidth = 256.0;
		double originalThumbImageHeight = 256.0;

		// Re-implementation of nearest neighbour resizing algorithm to resize the thumbnail images to the desired size.
		// Iterates over the range of pixels of the desired thumbnail size and copies all colours from the original
		// thumbnail size, scaled down
		// For each column from the left, for each row in that column:
		/* ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 * ------------
		 */

		for (int j = 0; j < desiredThumbnailHeight; j++) {
			for (int i = 0; i < desiredThumbnailWidth; i++) {

				// Scale up the pixels from the desired 50x50 image to the new 256x256 image
				// This means that at this point we are iterating over the original 256x256 image's pixels... *
				double pixelYCoord = j * originalThumbImageHeight / (double)desiredThumbnailHeight;
				double pixelXCoord = i * originalThumbImageWidth / (double)desiredThumbnailWidth;
									// 	int: orig image dimensions / double: new image dimensions as double

				// * ...grabbing the colour from there... *
				// Get the pixel color value from the dataset at the scaled up coordinate
				colorValue = ctHeadImageDataSet[sliceNumber][(int)pixelYCoord][(int)pixelXCoord];

				// Apply gamma correction using the current gamma correction value
				colorValue = (float) Math.pow(colorValue, (1.0 / gammaCorrectionMultiplier));

				// Create an RGB pixel colour with the colour from that position
				// Since it's greyscale, the same value can be applied to all RGB
				Color colorToBeSet = Color.color(colorValue,colorValue,colorValue);

				// Offset the coordinates from (0,0) by the requested coordinates to
				// draw the thumbnail at to ensure it's drawn in the correct position.
				int iOffsetted = i+xCoord;
				int jOffsetted = j+yCoord;

				// If we have reached the bounds of the main image holding all the thumbnails to write to,
				// Move back to the bounds to prevent out of bounds errors.
				// Data written outside of the bounds is not meaningful anyway.
				if (iOffsetted >= (int)thumbnailMasterImage.getWidth() + 1) {
					iOffsetted = (int)thumbnailMasterImage.getWidth();
				}

				if (jOffsetted >= (int)thumbnailMasterImage.getHeight() + 1) {
					jOffsetted = (int)thumbnailMasterImage.getHeight();
				}


				// Stop writing once we have reached the last slice at the last row
				// To prevent the last slice from being repeated or incrementing over the maximum.
				//290.0  585.0 = slice 113 end (zero based index))
				if ((i+xCoord > 290) && (j+yCoord > 539)) {
					break;
				}

				// * ...and setting that pixel colour at the new scaled position
				thumbnailImageWriter.setColor(iOffsetted, jOffsetted, colorToBeSet);

			}
		}
		return thumbnailMasterImage;
	}


	// Function to swap out the image in the main image view to another image of the same slice and gamma,
	// but a different slice/image index/number from the CTHead data set.
	public void updateMainViewImage(int sliceIndex) {

		// If the bilinear resize button is currently selected, change the main ImageView's
		// image to an image of the same size with the new slice number with the nearest neighbour method
		if (!resizeMode) {
			topDownImage = nearestNeighbourResize(GetCTScanImageSlice(sliceIndex), imageResizeSliderDoubleValue, imageResizeSliderIntValue);
			topDownImageView.setImage(topDownImage);

		// If the bilinear resize button is currently selected, change the main ImageView's
		// image to an image of the same size with the new slice number with the bilinear resize method
		} else {
			topDownImage = bilinearResize(GetCTScanImageSlice(sliceIndex), imageResizeSliderDoubleValue, imageResizeSliderIntValue);
			topDownImageView.setImage(topDownImage);
		}

	}


	// Function to decide which exact thumbnail is being hovered over across a whole row of thumbnails.
	// Determines which slice it is, then feeds it to updateMainViewImage().
	public void updateMainViewImageByRow(double xCoord, int rowIndex, boolean isRow9, int thumbnailSize, int gapPixelSize, int noOfColumns) {

		// The below code check the coordinates of the mouse against the positions of the thumbnails

		// Thumbnail 0
		// If the cursor X position is less than the thumbnailSize, it must be the thumbnail in the first column.
		if (xCoord < thumbnailSize) {
			updateMainViewImage((noOfColumns * rowIndex));
			currentSliceNumber = noOfColumns * rowIndex;
		}

		// Thumbnail 1
		// If the cursor Y position is above than 1 copy of the thumbnailSize + gap size,
		// but less than 2 copies of the thumbnailSize + the gap size (2 columns) it must be the thumbnail in the second column.
		if (xCoord > (thumbnailSize + gapPixelSize) && xCoord < (2 * thumbnailSize + gapPixelSize)) {
			updateMainViewImage(1 + (noOfColumns * rowIndex));
			currentSliceNumber = 1 + (noOfColumns * rowIndex);
		}

		// Thumbnail 2
		if (xCoord > 2 * (thumbnailSize + gapPixelSize) && xCoord < (3 * thumbnailSize + 2 * gapPixelSize)) {
			updateMainViewImage(2 + (noOfColumns * rowIndex));
			currentSliceNumber = 2 + (noOfColumns * rowIndex);
		}

		// Thumbnail 3
		if (xCoord > 3 * (thumbnailSize + gapPixelSize) && xCoord < (4 * thumbnailSize + 3 * gapPixelSize)) {
			updateMainViewImage(3 + (noOfColumns * rowIndex));
			currentSliceNumber = 3 + (noOfColumns * rowIndex);
		}

		// Thumbnail 4
		if (xCoord > 4 * (thumbnailSize + gapPixelSize) && xCoord < (5 * thumbnailSize + 4 * gapPixelSize)) {
			updateMainViewImage(4 + (noOfColumns * rowIndex));
			currentSliceNumber = 4 + (noOfColumns * rowIndex);
		}

		// If we aren't in the last row, continue.
		// Otherwise, stop here as we have reached the limit of displayed thumbnails.
		if (!isRow9) {

			// Thumbnail 5
			if (xCoord > 5 * (thumbnailSize + gapPixelSize) && xCoord < (6 * thumbnailSize + 5 * gapPixelSize)) {
				updateMainViewImage(5 + (noOfColumns * rowIndex));
				currentSliceNumber = 5 + (noOfColumns * rowIndex);
			}

			// Thumbnail 6
			if (xCoord > 6 * (thumbnailSize + gapPixelSize) && xCoord < (7 * thumbnailSize + 6 * gapPixelSize)) {
				updateMainViewImage(6 + (noOfColumns * rowIndex));
				currentSliceNumber = 6 + (noOfColumns * rowIndex);
			}

			// Thumbnail 7
			// If the cursor Y position is above than 7 copies of the thumbnailSize + gap size,
			// but less than 8 copies of the thumbnailSize + 7 copies of the gap size (6 columns)
			// it must be the thumbnail in the sixth column.
			if (xCoord > 7 * (thumbnailSize + gapPixelSize) && xCoord < (8 * thumbnailSize + 7 * gapPixelSize)) {
				updateMainViewImage(7 + (noOfColumns * rowIndex));
				currentSliceNumber = 7 + (noOfColumns * rowIndex);
			}

			// Thumbnail 8
			if (xCoord > 8 * (thumbnailSize + gapPixelSize) && xCoord < (9 * thumbnailSize + 8 * gapPixelSize)) {
				updateMainViewImage(8 + (noOfColumns * rowIndex));
				currentSliceNumber = 8 + (noOfColumns * rowIndex);
			}

			// Thumbnail 9
			if (xCoord > 9 * (thumbnailSize + gapPixelSize) && xCoord < (10 * thumbnailSize + 9 * gapPixelSize)) {
				updateMainViewImage(9 + (noOfColumns * rowIndex));
				currentSliceNumber = 9 + (noOfColumns * rowIndex);
			}

			// Thumbnail 10
			if (xCoord > 10 * (thumbnailSize + gapPixelSize) && xCoord < (11 * thumbnailSize + 10 * gapPixelSize)) {
				updateMainViewImage(10 + (noOfColumns * rowIndex));
				currentSliceNumber = 10 + (noOfColumns * rowIndex);
			}

			// Thumbnail 11
			if (xCoord > 11 * (thumbnailSize + gapPixelSize) && xCoord < (12 * thumbnailSize + 11 * gapPixelSize)) {
				updateMainViewImage(11 + (noOfColumns * rowIndex));
				currentSliceNumber = 11 + (noOfColumns * rowIndex);
			}

		}

	}


	// Function to lookup the gamma correction table at a specific colour, and get the gamma corrected value from it
	// Since the table is already based on the gamma correction multiplier it doesn't need to be used here
	public static float lookup_GammaLookupTable(float colour) {

		// Convert from Java 0.0-1.0 colour to 0.0-255.0
		int finalColour = (int)(colour * 255);

		// Return the this colour index from the gamma lookup table
		return gammaLookupTable[finalColour];

	}


	// Function to populate the gamma correction lookup table, based on the current gamma correction multiplier
	public static void create_GammaLookupTable() {

		// Iterates through the gamma lookup table, and applies the
		// gamma correction formula to each colour value from 0-255.
		for (int i = 0; i < gammaLookupTable.length; i++) {
			gammaLookupTable[i] = (float)Math.pow(i / 256.0, (1.0/gammaCorrectionMultiplier));
		}

	}


	// Main method
	public static void main(String[] args) { launch(); }

}