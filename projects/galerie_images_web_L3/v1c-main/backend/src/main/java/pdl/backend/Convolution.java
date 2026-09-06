package pdl.backend;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import java.awt.image.BufferedImage;

public class Convolution {

  public static void meanFilter(GrayU8 input, GrayU8 output, int size) {
    double average = 1.0 / (size * size);
    int n = (size - 1) / 2;

    for (int y = n; y < input.height - n; ++y) {
      for (int x = n; x < input.width - n; ++x) {
        int sum = 0;

        for (int u = -n; u <= n; u++) {
          for (int v = -n; v <= n; v++) {
            sum += input.get(x + u, y + v);
          }
        }

        int result = (int) (sum * average);
        output.set(x, y, result);
      }
    }
  }

  public static void meanFilterColored(Planar<GrayU8> input, Planar<GrayU8> output, int size) {
    double average = 1.0 / (size * size);
    int n = (size - 1) / 2;
    int sum_r, sum_g, sum_b;
    for (int y = n; y < input.height - n; ++y) {
      for (int x = n; x < input.width - n; ++x) {
        sum_r = 0;
        sum_g = 0;
        sum_b = 0;

        for (int u = -n; u <= n; u++) {
          for (int v = -n; v <= n; v++) {
            sum_r += input.getBand(0).get(x + u, y + v);
            sum_g += input.getBand(1).get(x + u, y + v);
            sum_b += input.getBand(2).get(x + u, y + v);
          }
        }

        sum_r = (int) (sum_r * average);
        sum_g = (int) (sum_g * average);
        sum_b = (int) (sum_b * average);
        output.getBand(0).set(x, y, sum_r);
        output.getBand(1).set(x, y, sum_g);
        output.getBand(2).set(x, y, sum_b);
      }
    }
  }



  public static void convolution(GrayU8 input, GrayS16 output, int[][] kernel) {
    int size = kernel.length;
    int n = (size - 1) / 2;
    for (int y = n; y < input.height - n; ++y) {
      for (int x = n; x < input.width - n; ++x) {
        int r = 0;
        for (int u = -n; u <= n; u++) {
          for (int v = -n; v <= n; v++) {
            r += (int) (input.get(x + u, y + v) * kernel[u + n][v + n]);
          }
        }
        output.set(x, y, r);
      }
    }
  }

  public static void gradientImage(GrayU8 input, GrayU8 output, int[][] kernelX, int[][] kernelY) {

    GrayS16 image1 = new GrayS16(output.width, output.height);
    GrayS16 image2 = new GrayS16(output.width, output.height);

    convolution(input, image1, kernelX);
    convolution(input, image2, kernelY);

    int gradx, grady;
    double result;
    for (int y = 0; y < output.height; y++) {
      for (int x = 0; x < output.width; x++) {
        gradx = image1.get(x, y);
        grady = image2.get(x, y);
        result = Math.sqrt(gradx * gradx + grady * grady);
        if (result > 255) {
          result = 255;
        }
        
        output.set(x, y, (int) result);
      }
    }
  }

  public static void gradientImageSobel(GrayU8 input, GrayU8 output) {
    int[][] kernelX = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
    int[][] kernelY = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };
    gradientImage(input, output, kernelX, kernelY);
  }

  public static void gradientImagePrewitt(GrayU8 input, GrayU8 output) {
    int[][] kernelX = { { -1, 0, 1 }, { -1, 0, 1 }, { -1, 0, 1 } };
    int[][] kernelY = { { -1, -1, -1 }, { 0, 0, 0 }, { 1, 1, 1 } };
    gradientImage(input, output, kernelX, kernelY);
  }

  /* Question 6 : 4) Oui c'est possible, voici les kernels correspondants : 
  int[][] kernelX = { { 0, 0, 0 }, { -1, 0, 1 }, { 0, 0, 0 } };
  int[][] kernelY = { { 0, -1, 0 }, { 0, 0, 0 }, { 0, 1, 0 } }; */

  public static void convertU8ToS16(GrayS16 input) {
    int v;
    for (int y = 0; y < input.height; ++y) {
      for (int x = 0; x < input.width; ++x) {
        v = input.get(x, y);
        if (v < 0) {
          v = -v;
        }
        if (v > 255) {
          v = 255;
        }
        input.set(x, y, v);
      }
    }
  }




  public static void main(final String[] args) {
    // load image
    if (args.length < 2) {
      System.out.println("missing input or output image filename");
      System.exit(-1);
    }
    final String inputPath = args[0];
    BufferedImage input_colored = UtilImageIO.loadImage(inputPath);
    Planar<GrayU8> image = ConvertBufferedImage.convertFromPlanar(input_colored, null, true, GrayU8.class);
    Planar<GrayU8> image_output = image.createSameShape();


/*     GrayU8 input = UtilImageIO.loadImage(inputPath, GrayU8.class);

    GrayU8 output = input.createSameShape();
    GrayS16 output2 = new GrayS16(input.width, input.height);
    int[][] kernelX = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
    int[][] kernelY = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } }; */

    // processing

    /*long startTime = System.nanoTime();
 
    meanFilter(input, output, 49);

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println(duration/1000000); */

    /* 
    convolution(input, output2, kernelX);
    convertU8ToS16(output2); */

    //gradientImageSobel(input, output);

    //gradientImagePrewitt(input, output);

    //meanFilterColored(image, image_output, 3);

    

    // save output image
    //ConvertImage.convert(output2, output);
    final String outputPath = args[1];

    //UtilImageIO.saveImage(output, outputPath);
    UtilImageIO.saveImage(image_output, outputPath);
    
    System.out.println("Image saved in: " + outputPath);
  }

}
