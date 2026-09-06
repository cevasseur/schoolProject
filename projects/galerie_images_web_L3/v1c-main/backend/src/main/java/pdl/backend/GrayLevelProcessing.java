package pdl.backend;

import boofcv.alg.color.ColorHsv;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class GrayLevelProcessing {

	public static void threshold(GrayU8 input, int t) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				if (gl < t) {
					gl = 0;
				} else {
					gl = 255;
				}
				input.set(x, y, gl);
			}
		}
	}

	public static void changeLuminosity(GrayU8 input, int delta) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				gl += delta;
				if (gl > 255) {
					gl = 255;
				} else if (gl < 0) {
					gl = 0;
				}
				input.set(x, y, gl);
			}
		}
	}

	public static void makeContrastDyn(GrayU8 input) {
		int min = 255;
		int max = 0;
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				if (gl > max) {
					max = gl;
				}
				if (gl < min) {
					min = gl;
				}
			}
		}

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				gl = (255 * (gl - min)) / (max - min);
				input.set(x, y, gl);
			}
		}
	}

	public static void makeContrastDynLut(GrayU8 input) {
		int min = 255;
		int max = 0;
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				if (gl > max) {
					max = gl;
				}
				if (gl < min) {
					min = gl;
				}
			}
		}

		int[] LUT = new int[256];
		for (int i = 0; i < 256; i++) {
			LUT[i] = (255 * (i - min)) / (max - min);
		}

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				gl = LUT[gl];
				input.set(x, y, gl);
			}
		}
	}

	public static void equalize(GrayU8 input) {
		int[] histo = new int[256];
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				histo[gl] += 1;
			}
		}

		int[] C = new int[256];
		for (int k = 0; k < 256; k++) {
			for (int i = 0; i < k; i++) {
				C[k] += histo[i];
			}
		}

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int gl = input.get(x, y);
				gl = (C[gl] * 255) / (input.height * input.width);
				input.set(x, y, gl);
			}
		}

	}

	public static void changeLuminosityColored(Planar<GrayU8> input, int delta) {
		int r,g,b;
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				r = input.getBand(0).get(x, y);
				g = input.getBand(1).get(x, y);
				b = input.getBand(2).get(x, y);
				r = Math.max(0, Math.min(255, r + delta));
				g = Math.max(0, Math.min(255, g + delta));
				b = Math.max(0, Math.min(255, b + delta));
				input.getBand(0).set(x,y,r);
				input.getBand(1).set(x,y,g);
				input.getBand(2).set(x,y,b);
			}
		}
	}

	public static void makeGrey(Planar<GrayU8> input) {
		int sum_r, sum_g, sum_b, result;
		for (int y = 0; y < input.height; y++) {
		  for (int x = 0; x < input.width; x++) {
			sum_r = input.getBand(0).get(x, y);
			sum_g = input.getBand(1).get(x, y);
			sum_b = input.getBand(2).get(x, y);
			result = (int) (sum_r * 0.3 + sum_g * 0.59 + sum_b * 0.11);
			input.getBand(0).set(x, y, result);
			input.getBand(1).set(x, y, result);
			input.getBand(2).set(x, y, result);
		  }
		}
	  }

	public static void filter(Planar<GrayF32> input, float teinte) {

	Planar<GrayF32> imageHSV = input.createSameShape();
	ColorHsv.rgbToHsv(input, imageHSV);
	for (int y = 0; y < imageHSV.height; y++) {
		for (int x = 0; x < imageHSV.width; x++) {
			imageHSV.getBand(0).set(x,y,(float)(Math.toRadians(teinte)));
		}
	}
	ColorHsv.hsvToRgb(imageHSV, input);}


	public static void histoTeinte(Planar<GrayF32> input, GrayU8 output) {
		int[] histo = new int[360];
		Planar<GrayF32> imageHSV = input.createSameShape();
		ColorHsv.rgbToHsv(input, imageHSV);
		for (int y = 0; y < imageHSV.height; y++) {
			for (int x = 0; x < imageHSV.width; x++) {
				histo[(int) Math.toDegrees(imageHSV.getBand(0).get(x, y))] ++;
			}
		}

		int maxHisto = 0;
		for (int i = 0; i < 360; i++) {
			if (histo[i] > maxHisto) {
				maxHisto = histo[i];
			}
		}

		output.reshape(360,200);
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				if (y == output.height-1) {
					output.set(x,y,255); //Base de pixels blancs tout en bas de l'histogramme.
				}
				else {
					output.set(x, y, 0);
				}
				
			}
		}
		
		for (int x = 0; x < output.width; x++) {
			histo[x] = (histo[x] * output.height) / maxHisto;
			for (int y = 0; y < histo[x] && y != output.height-1; y++) {
				output.set(x, (output.height-2)-y, 255);
			}
		}
	}

	public static void histo2D(Planar<GrayF32> input, GrayU8 output) {
		int[][] histo = new int[360][101];
		Planar<GrayF32> imageHSV = input.createSameShape();
		ColorHsv.rgbToHsv(input, imageHSV);
		for (int y = 0; y < imageHSV.height; y++) {
			for (int x = 0; x < imageHSV.width; x++) {
				histo[(int) Math.toDegrees(imageHSV.getBand(0).get(x, y))][Math.round(imageHSV.getBand(1).get(x, y) * 100)] ++;
			}
		}

		output.reshape(360,101);
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				output.set(x, y, 0);
				if (histo[x][y] > 255) output.set(x,y,255);
				else output.set(x, y, histo[x][y]);
			}
		}
	}

	//Create histogram from an image of type Image.
	public static int[][] imgToHisto2D(Image img) {
		ByteArrayInputStream data = new ByteArrayInputStream(img.getData()); // Convert image data to a stream
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(data); // Read the image into a BufferedImage
		} catch (IOException e) {
			throw new RuntimeException("Error in getting image's data.",e);
		}
		Planar<GrayF32> imageF32 = ConvertBufferedImage.convertFromPlanar(bufferedImage, null, true, GrayF32.class);
		GrayU8 output = new GrayU8(0,0);
		histo2D(imageF32,output);

		int[][] histo2D = new int[360][101];
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				histo2D[x][y] = output.get(x, y);
			}
		}
		return histo2D;
	}


	      /**
     * Converts a colored image into a 3D RGB array.
     * @param input The colored image in RGB format.
     * @return A 3D array, each dimension representing RGB colors.
     */
	public static int[][][] histo3D(Planar<GrayU8> input) {
		if (input == null || input.getNumBands() < 3) {
			throw new IllegalArgumentException("Input image must be non-null and have at least 3 bands (RGB)");
		}
		int[][][] histo = new int[256][256][256];
		int r,g,b;
		GrayU8 bandR = input.getBand(0);
		GrayU8 bandG = input.getBand(1);
		GrayU8 bandB = input.getBand(2);
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				r = (int) bandR.get(x,y);
				g = (int) bandG.get(x,y);
				b = (int) bandB.get(x,y);
				histo[r][g][b]++;
			}
		}
		return histo;
	}


	public static int[][][] imgToHisto3D(Image img) {
		ByteArrayInputStream data = new ByteArrayInputStream(img.getData());
		BufferedImage bufferedImage = null;
		try {
		  bufferedImage = ImageIO.read(data); // Read the image into a BufferedImage
		} catch (IOException e) {
		  throw new RuntimeException("Error in getting image's data.",e);
		}
		Planar<GrayU8> imageU8Planar = ConvertBufferedImage.convertFromPlanar(bufferedImage, null, true, GrayU8.class);
		return GrayLevelProcessing.histo3D(imageU8Planar);
	}


	public static int[] histo2DToHisto1D(int[][] histo2D) {
		int[] histo1D = new int[36360];
		int[] result = new int[3636];
		int index = 0;
	
		// Flatten the 2D histogram into a 1D array
		for (int i = 0; i < 360; i++) {
			for (int j = 0; j < 101; j++) {
				histo1D[index++] = histo2D[i][j];
			}
		}
	
		// Reduce size by summing every 10 elements
		int groupsize = 10;
		for (int i = 0; i < 3636; i++) {
			int sum = 0;
			for (int j = 0; j < groupsize; j++) {
				sum += histo1D[i * groupsize + j];
			}
			result[i] = sum;
		}
	
		return result;
	}

	public static float[] imgToHisto1DCompressedFromHisto2D(Image img) {
		int[][] histo2D = imgToHisto2D(img);
		int[] histo1D = histo2DToHisto1D(histo2D);
		return normalize(histo1D);
	}

	public static int[] histo3DToHisto1D(int[][][] histo3D) {
		int size = 16000;
		int groupSize = (256 * 256 * 256) / size; // 1048 elements per group
		int[] result = new int[size];
	
		int index = 0, sum = 0, count = 0;
	
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 256; y++) {
				for (int z = 0; z < 256; z++) {
					sum += histo3D[x][y][z];
					count++;
	
					if (count == groupSize && index < size) {
						result[index++] = sum;
						sum = 0;
						count = 0;
					}
				}
			}
		}
		result[index-1] = result[index-1] + sum;
		return result;
	}

	public static float[] imgToHisto1DCompressedFromHisto3D(Image img) {
		int[][][] histo3D = imgToHisto3D(img);
		int[] histo1D = histo3DToHisto1D(histo3D);
		return normalize(histo1D);
	}

	public static float[] normalize(int[] vector) {
		float[] normalized = new float[vector.length];
		double norm = 0;
		for (int val : vector) {
			norm += ((double)val) * val;
		}
		norm = Math.sqrt(norm);
		if (norm == 0) norm = 1; // éviter division par 0
	
		for (int i = 0; i < vector.length; i++) {
			normalized[i] = (float)(vector[i] / norm);
		}
		return normalized;
	}
	
	

	public static void main(String[] args) {
		// load image
		if (args.length < 2) {
			System.out.println("missing input or output image filename");
			System.exit(-1);
		}
		final String inputPath = args[0];
		GrayU8 input = UtilImageIO.loadImage(inputPath, GrayU8.class);

		BufferedImage input_colored = UtilImageIO.loadImage(inputPath);
		// Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(input_colored, null, true, GrayU8.class);

		if (input == null) {
			System.err.println("Cannot read input file '" + inputPath);
			System.exit(-1);
		}
		
		// processing
		// threshold(input, 128);
		// changeLuminosity(input, -50);
		// makeContrastDyn(input);
		// makeContrastDynLut(input);
		// equalize(input);
		
		// changeLuminosityColored(image, 50);
		// makeGrey(image);

		/* Question TP4, 4.1 :
		double[] hsv = new double[3];
		ColorHsv.rgbToHsv(10.0,10.0,10.0,hsv);
		System.out.println(hsv[0]);
		System.out.println(hsv[1]);
		System.out.println(hsv[2]);
		Les intervalles de valeur pour h s et v sont :
		 * h = [0, 360]
		 * s = [0, 1]
		 * v = [0, 255]
		 * La conversion d'un pixel gris, par exemple  R = 10.0 et R=G=B donne :
		 * h = NaN (undefined)
		 * s = 0.0
		 * v = 10.0
		 * h représente la teinte de couleur, pour gris il n'y a pas de couleur particulière donc undefined,
		 * s représente la saturation, c'est-à-dire l'intensité de la couleur, il n'y a pas de couleur pour un pixel gris donc 0.0,
		 * v représente la luminosité, ici c'est l'intensité du gris, qui est 10.0.
		 */

		 /* Question TP4, 4.3
		  * Mettre la saturation à 0 rend l'image grise, c'est équivalent au filtre gris (que réalise la fonction makeGrey).
		  */
		Planar<GrayF32> imageRGB = ConvertBufferedImage.convertFromPlanar(input_colored, null, true, GrayF32.class);
		//filter(imageRGB, 270f);
		
		//histoTeinte(imageRGB, input);

		histo2D(imageRGB, input);

		// save output image
		final String outputPath = args[1];
		UtilImageIO.saveImage(input, outputPath); // First parameter is : input for grey images; image for RGB colored images; imageRGB for filter
		System.out.println("Image saved in: " + outputPath);
	}

}