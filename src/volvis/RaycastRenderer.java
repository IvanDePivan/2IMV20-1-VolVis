package volvis;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

import java.awt.image.BufferedImage;

/**
 * Raycast Renderer.
 *
 * @author Michel Westenberg
 * @author Anna Vilanova
 * @author Nicola Pezzotti
 * @author Humberto Garcia
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    /**
     * Volume that is loaded and visualized.
     */
    private Volume volume = null;

    /**
     * Rendered image.
     */
    private BufferedImage image;

    /**
     * Gradient information of the loaded volume.
     */
    private GradientVolume gradients = null;

    /**
     * Reference to the GUI panel.
     */
    RaycastRendererPanel panelFront;

    /**
     * Transfer Function.
     */
    TransferFunction tFuncFront;

    /**
     * Reference to the GUI transfer function editor.
     */
    TransferFunctionEditor tfEditor;

    /**
     * Transfer Function 2D.
     */
    TransferFunction2D tFunc2DFront;

    /**
     * Reference to the GUI 2D transfer function editor.
     */
    TransferFunction2DEditor tfEditor2DFront;

    /**
     * Mode of our raycast. See {@link RaycastMode}
     */
    private RaycastMode modeFront;

    /**
     * Whether we are in cutting plane mode or not.
     */
    private boolean cuttingPlaneMode = false;

    /**
     * Whether we are in shading mode or not.
     */
    private boolean shadingMode = false;

    /**
     * Iso value to use in Isosurface rendering.
     */
    private float isoValueFront = 95f;

    /**
     * Color used for the isosurface rendering.
     */
    private TFColor isoColorFront;

    // Below cutting plane specific attributes
    /**
     * Cutting plane normal vector.
     */
    private final double[] planeNorm = new double[]{0d, 0d, 1d};

    /**
     * Cutting plane point.
     */
    private final double[] planePoint = new double[]{0d, 0d, 0d};

    /**
     * Back mode of our raycast for cutting plane.
     */
    private RaycastMode modeBack;

    /**
     * Iso value to use in Isosurface rendering for cutting plane.
     */
    private float isoValueBack = 95f;

    /**
     * Color used for the isosurface rendering for cutting plane.
     */
    private TFColor isoColorBack;

    /**
     * Transfer Function for cutting plane.
     */
    TransferFunction tFuncBack;

    /**
     * Reference to the GUI transfer function editor for cutting plane.
     */
    TransferFunctionEditor tfEditorBack;

    /**
     * Transfer Function 2D for cutting plane.
     */
    TransferFunction2D tFunc2DBack;

    /**
     * Reference to the GUI 2D transfer function editor for cutting plane.
     */
    TransferFunction2DEditor tfEditor2DBack;

    /**
     * Gets the corresponding voxel using Nearest Neighbors.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel value.
     */
    private short getVoxel(double[] coord) {
        // Get coordinates
        double dx = coord[0], dy = coord[1], dz = coord[2];

        // Verify they are inside the volume
        if (dx < 0 || dx >= volume.getDimX() || dy < 0 || dy >= volume.getDimY()
                || dz < 0 || dz >= volume.getDimZ()) {

            // If not, jus return 0
            return 0;
        }

        // Get the closest x, y, z to dx, dy, dz that are integers
        // This is important as our data is discrete (not continuous)
        int x = (int) Math.floor(dx);
        int y = (int) Math.floor(dy);
        int z = (int) Math.floor(dz);

        // Finally, get the voxel from the Volume for the corresponding coordinates
        return volume.getVoxel(x, y, z);
    }

    /**
     * Updates {@link #image} attribute (result of rendering) using the slicing
     * technique.
     *
     * @param viewMatrix OpenGL View matrix {
     * @see <a href="www.songho.ca/opengl/gl_transform.html#modelview">link</a>}.
     */
    private void slicer(double[] viewMatrix) {

        // Clear the image
        resetImage();

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec which is going from the view point towards the object
        // uVec contains the up vector of the camera in world coordinates (image vertical)
        // vVec contains the horizontal vector in world coordinates (image horizontal)
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // We get the size of the image/texture we will be putting the result of the
        // volume rendering operation.
        int imageW = image.getWidth();
        int imageH = image.getHeight();

        int[] imageCenter = new int[2];
        // Center of the image/texture 
        imageCenter[0] = imageW / 2;
        imageCenter[1] = imageH / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();

        TFColor pixelColor = new TFColor();

        for (int j = imageCenter[1] - imageH / 2; j < imageCenter[1] + imageH / 2; j++) {
            for (int i = imageCenter[0] - imageW / 2; i < imageCenter[0] + imageW / 2; i++) {
                // computes the pixelCoord which contains the 3D coordinates of the pixels (i,j)
                computePixelCoordinatesFloat(pixelCoord, volumeCenter, uVec, vVec, i, j);

                //NOTE: you have to implement this function to get the tri-linear interpolation
                int val = (int) volume.getVoxelTrilinear(pixelCoord);

                // Map the intensity to a grey value by linear scaling
                pixelColor.r = val / max;
                pixelColor.g = pixelColor.r;
                pixelColor.b = pixelColor.r;
                pixelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                // Alternatively, apply the transfer function to obtain a color
                // pixelColor = tFuncFront.getColor(val);

                //BufferedImage/image/texture expects a pixel color packed as ARGB in an int
                //use the function computeImageColor to convert your double color in the range 0-1 to the format need by the image
                int packedPixelColor = computePackedPixelColor(pixelColor.r, pixelColor.g, pixelColor.b, pixelColor.a);
                image.setRGB(i, j, packedPixelColor);
            }
        }
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Updates {@link #image} attribute (result of rendering) using MIP
     * raycasting. It returns the color assigned to a ray/pixel given its
     * starting and ending points, and the direction of the ray.
     *
     * @param entryPoint Starting point of the ray.
     * @param exitPoint  Last point of the ray.
     * @param rayVector  Direction of the ray.
     * @param sampleStep Sample step of the ray.
     * @return Color assigned to a ray/pixel.
     */
    private TFColor traceRayMIP(double[] currentPos, double[] increments, int nrSamples) {
        double maximum = 0;
        do {
            double value = getVoxel(currentPos) / 255.;
            if (value > maximum) {
                maximum = value;
            }
            for (int i = 0; i < 3; i++) {
                currentPos[i] += increments[i];
            }
            nrSamples--;
        } while (nrSamples > 0);

        double alpha;
        double r, g, b;
        if (maximum > 0.0) { // if the maximum = 0 make the voxel transparent
            alpha = 1.0;
        } else {
            alpha = 0.0;
        }
        r = g = b = maximum;
        return new TFColor(r, g, b, alpha);
    }


    /**
     * Finds a more accurate point in space where the isoValue is crossed
     *
     * @param currentPos
     * @param increments
     * @param minSampleStep
     * @param value
     * @param isoValue
     * @return the updated position
     */
    private double[] bisectionAccuracy(double[] currentPos, double[] increments, double minSampleStep, double value, float isoValue) {
        //get value halfway between currentPos and currentPos-increments
        VectorMath.setVector(increments, increments[0] / 2, increments[1] / 2, increments[2] / 2);

        //set currentPos halfway between previousPos and currentPos
        double[] maxPos = currentPos;
        for (int i = 0; i < 3; i++) {
            currentPos[i] -= increments[i];
        }

        //once the step is small enough, return the found position
        if (increments[0] < minSampleStep) {
            //return position
            return currentPos;
        }

        //get value at mid point
        value = volume.getVoxelTrilinear(currentPos);

        //mid value >= isoValue - > go left (smaller values)
        if (value >= isoValue) {
            return bisectionAccuracy(currentPos, increments, minSampleStep, value, isoValue);
        }

        //mid value < isoValue -> go right (larger values)
        if (value < isoValue) {
            return bisectionAccuracy(maxPos, increments, minSampleStep, value, isoValue);
        }
        //should not be possible to reach this line
        return currentPos;
    }

    /**
     * Updates {@link #image} attribute (result of rendering) using the
     * Isosurface raycasting. It returns the color assigned to a ray/pixel given
     * its starting and ending points, and the direction of the ray.
     *
     * @param entryPoint Starting point of the ray.
     * @param exitPoint  Last point of the ray.
     * @param sampleStep Sample step of the ray.
     * @return Color assigned to a ray/pixel.
     */
    private TFColor traceRayIso(double[] currentPos, double[] increments, int nrSamples, boolean isFrontMode) {
        float isoValue = (isFrontMode ? isoValueFront : isoValueBack);
        TFColor isoColor = (isFrontMode ? isoColorFront : isoColorBack);
        // TODO 3: Implement isosurface rendering.
        //Initialization of the colors as floating point values
        double r, g, b, alpha;
        r = g = b = alpha = 0.0;

        double isoThreshold;
        do {
            double value = volume.getVoxelTrilinear(currentPos);
            isoThreshold = value - isoValue;

            if (isoThreshold >= 0) {
                //get more accurate position with bisection accuracy:
                double minSampleStep = 0.01;
                currentPos = bisectionAccuracy(currentPos, increments, minSampleStep, value, isoValue);

                // isoColor contains the isosurface color from the interface
                r = isoColor.r;
                g = isoColor.g;
                b = isoColor.b;
                alpha = 1.0; // TODO check if this is correct

                break;
            }
            for (int i = 0; i < 3; i++) {
                currentPos[i] += increments[i];
            }
            nrSamples--;
        } while (nrSamples > 0);

        return new TFColor(r, g, b, alpha);
    }

    /**
     * Calculates the composite color
     *
     * @param nrSamples  how many points to sample along the ray
     * @param currentPos The current position in the ray
     * @param increments The direction a step on the ray is in
     * @param tFunction  Which transfer function values to use in the calculation
     * @return
     */
    public TFColor compositeCalculationRGB(int nrSamples, double[] currentPos, double[] increments, TransferFunction tFunction) {
        TFColor voxel_color = new TFColor();
        double value = volume.getVoxelTrilinear(currentPos);
        int intValue = (int) value;
        // get transfer function value at current position
        TFColor colorAux = tFunction.getColor(intValue);

        //move forwards along the ray
        for (int i = 0; i < 3; i++) {
            currentPos[i] += increments[i];
        }
        nrSamples--;

        //when at the end of the samples or 'more or less nat full opacity level'
        if (nrSamples == 0 || colorAux.a > 0.99) {
            voxel_color.r = colorAux.r * colorAux.a;
            voxel_color.b = colorAux.b * colorAux.a;
            voxel_color.g = colorAux.g * colorAux.a;
            //lowest level return
            return voxel_color;
        }
        //recursive call
        TFColor nextVoxelColor = compositeCalculationRGB(nrSamples, currentPos, increments, tFunction);
        //the compositing formula
        voxel_color.r = colorAux.r * colorAux.a + (1 - colorAux.a) * nextVoxelColor.r;
        voxel_color.b = colorAux.b * colorAux.a + (1 - colorAux.a) * nextVoxelColor.b;
        voxel_color.g = colorAux.g * colorAux.a + (1 - colorAux.a) * nextVoxelColor.g;
        return voxel_color;
    }

    TFColor computeTF2DColor(TransferFunction2D function2D, TFColor color, double[] currentPos, double[] increments, int nrSamples) {
        //base case: stop at end of ray OR when opacity is close to max
        if (nrSamples <= 0 || color.a >= 0.999) {
            return color;
        }

        //calculate gradient magnitude and intensity of current voxel
        VoxelGradient voxelGradient = gradients.getGradientTrilinear(currentPos);
        double voxelIntensity = volume.getVoxelTrilinear(currentPos);

        //calculate opacity of current voxel
        double opacity = computeOpacity2DTF(function2D.baseIntensity, function2D.radius, voxelIntensity, voxelGradient.mag);

        //composite current opacity and previous voxel component
        color = compositeColors2D(function2D.color, color, opacity);

        //increment position
        for (int i = 0; i < 3; i++) {
            currentPos[i] += increments[i];
        }
        //recursive call
        return computeTF2DColor(function2D, color, currentPos, increments, nrSamples - 1);
    }

    public TFColor compositeColors2D(TFColor functionColor, TFColor color, double opacityNextVoxel) {
        //update color with voxel component
        color.r += (1 - color.a) * functionColor.r * opacityNextVoxel;
        color.g += (1 - color.a) * functionColor.g * opacityNextVoxel;
        color.b += (1 - color.a) * functionColor.b * opacityNextVoxel;
        color.a += (1 - color.a) * functionColor.a * opacityNextVoxel;

        return color;
    }


    /**
     * Computes the opacity based on the value of the pixel and values of the
     * triangle widget. {@link #tFunc2DFront} contains the values of the base
     * intensity and radius. {@link TransferFunction2D#baseIntensity} and
     * {@link TransferFunction2D#radius} are in image intensity units.
     *
     * @param intensity     Value of the material.
     * @param radius        Radius of the material.
     * @param voxelValue    Voxel value.
     * @param gradMagnitude Gradient magnitude.
     * @return
     */
    public double computeOpacity2DTF(double intensity, double radius,
                                     double voxelValue, double gradMagnitude) {
        double angle = Math.atan(gradMagnitude / Math.abs(voxelValue - intensity));

        double wedgeAngle = Math.atan(radius / intensity);
        if (wedgeAngle < angle) {
            double s = Math.abs(radius * (gradMagnitude / gradients.getMaxGradientMagnitude()));
            if (voxelValue > intensity - s && voxelValue < intensity + s) {

                return 1 - wedgeAngle / angle;
            }
        }
        return 0;
    }

    /**
     * Compute Phong Shading given the voxel color (material color), gradient,
     * light vector and view vector.
     *
     * @param voxelColor  Voxel color (material color).
     * @param gradient    Gradient voxel.
     * @param lightVector Light vector.
     * @param rayVector   View vector.
     * @return Computed color for Phong Shading.
     */
    private TFColor computePhongShading(TFColor voxelColor, VoxelGradient gradient, double[] lightVector,
                                        double[] rayVector) {

        // TODO 7: Implement Phong Shading.
        //a 'reflective surface' will always have a not-null gradient magnitude
        if (gradient.mag == 0) {
            return voxelColor;
        }

        //reflectiveness constants
        double ambientFactor = 0.1;
        double diffuseFactor = 0.7;
        double specularFactor = 0.2;
        double alpha = 50;


        //formula implemented:
        //intensity = ambientFactor*ia + diffuseFactor*(L^ dot N^ )*id + specularFactor*(r^ dot v^)^a*is;

        //set the colors; compute the 3 bands separately
        double rVoxel = voxelColor.r;
        double gVoxel = voxelColor.g;
        double bVoxel = voxelColor.b;

        //setup the necessary variables
        double[] toLightNormal = new double[3];
        VectorMath.normalize(lightVector, toLightNormal);

        double[] toView = {rayVector[0], rayVector[1], rayVector[2]};
        double[] toViewNormal = new double[3];
        VectorMath.normalize(toView, toViewNormal);

        double[] gradientNormal = new double[3];
        double[] invertGradient = {-gradient.x, -gradient.y, -gradient.z};
        VectorMath.normalize(invertGradient, gradientNormal);


        //compute light reflection
        double[] scaled = new double[3];
        double dotProduct = VectorMath.dotproduct(toLightNormal, gradientNormal);
        double lambertian = Math.max(dotProduct, 0.0);


        VectorMath.multiply(gradientNormal, 2 * dotProduct, scaled);

        // reflectionNormal is the the direction taken by a perfect reflection of the light source on the surface
        double[] reflectionNormal = new double[3];
        VectorMath.difference(scaled, toLightNormal, reflectionNormal);

        //store ambient color
        double rAmbient = ambientFactor * rVoxel;
        double gAmbient = ambientFactor * gVoxel;
        double bAmbient = ambientFactor * bVoxel;

        //check if normal is in correct direction, if light is orthogonal(or larger angle) to the surface only use ambient lighting
        if (lambertian <= 0.0) {
            return new TFColor(rAmbient, gAmbient, bAmbient, voxelColor.a);
        }

        //store diffuse color
        double rDiffuse = diffuseFactor * dotProduct * rVoxel;
        double gDiffuse = diffuseFactor * dotProduct * gVoxel;
        double bDiffuse = diffuseFactor * dotProduct * bVoxel;

        //final step in computing the specular light reflection
        double specAngle = VectorMath.dotproduct(reflectionNormal, toViewNormal);
        double specPow = Math.pow(specAngle, alpha);
        //store specular color
        double rSpecular = specularFactor * specPow * rVoxel;
        double gSpecular = specularFactor * specPow * gVoxel;
        double bSpecular = specularFactor * specPow * bVoxel;

        //store the final color
        double newColorR = rAmbient + rDiffuse + rSpecular;
        double newColorG = gAmbient + gDiffuse + gSpecular;
        double newColorB = bAmbient + bDiffuse + bSpecular;

        //keep transparency of color passed as argument
        return new TFColor(newColorR, newColorG, newColorB, voxelColor.a);
    }

    /**
     * Implements the basic tracing of rays through the image given the camera
     * transformation. It calls the functions depending on the raycasting mode.
     *
     * @param viewMatrix
     */
    void raycast(double[] viewMatrix) {
        //data allocation
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        double[] pixelCoord = new double[3];
        double[] entryPoint = new double[3];
        double[] exitPoint = new double[3];

        // TODO 5: Limited modification is needed
        // increment in the pixel domain in pixel units
        int increment = 1;
        // sample step in voxel units
        int sampleStep = 1;
        if (interactiveMode) {
            increment = 3;
            sampleStep = 3;
        }

        // reset the image to black
        resetImage();

        // vector uVec and vVec define a plane through the origin,
        // perpendicular to the view vector viewVec which is going from the view point towards the object
        // uVec contains the up vector of the camera in world coordinates (image vertical)
        // vVec contains the horizontal vector in world coordinates (image horizontal)
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // We get the size of the image/texture we will be putting the result of the
        // volume rendering operation.
        int imageW = image.getWidth();
        int imageH = image.getHeight();

        int[] imageCenter = new int[2];
        // Center of the image/texture
        imageCenter[0] = imageW / 2;
        imageCenter[1] = imageH / 2;

        //The rayVector is pointing towards the scene
        double[] rayVector = new double[3];
        rayVector[0] = viewVec[0];
        rayVector[1] = viewVec[1];
        rayVector[2] = viewVec[2];

        // compute the volume center
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);

        // ray computation for each pixel
        for (int j = imageCenter[1] - imageH / 2; j < imageCenter[1] + imageH / 2; j += increment) {
            for (int i = imageCenter[0] - imageW / 2; i < imageCenter[0] + imageW / 2; i += increment) {
                // compute starting points of rays in a plane shifted backwards to a position behind the data set
                computePixelCoordinatesBehindFloat(pixelCoord, viewVec, uVec, vVec, i, j);
                // compute the entry and exit point of the ray
                computeEntryAndExit(pixelCoord, rayVector, entryPoint, exitPoint);

                // TODO 9: Implement logic for cutting plane.
                if ((entryPoint[0] > -1.0) && (exitPoint[0] > -1.0)) {
                    boolean isFrontMode = !cuttingPlaneMode || isFrontSlice(entryPoint);
                    int val = traceRay(entryPoint, exitPoint, sampleStep, rayVector, isFrontMode);

                    for (int ii = i; ii < i + increment && ii < imageH; ii++) {
                        for (int jj = j; jj < j + increment && jj < imageW; jj++) {
                            image.setRGB(ii, jj, val);
                        }
                    }
                }

            }
        }
    }

    private int traceRay(double[] entryPoint, double[] exitPoint, int sampleStep, double[] rayVector, boolean isFrontMode) {
        //We define the light vector as directed toward the view point (which is the source of the light)
        // another light vector would be possible
        double[] lightVector = new double[3];
        VectorMath.setVector(lightVector, -rayVector[0], -rayVector[1], -rayVector[2]);

        //the current position is initialized as the exit point
        double[] currentPos = new double[3];
        VectorMath.setVector(currentPos, exitPoint[0], exitPoint[1], exitPoint[2]);

        //compute the increment and the number of samples
        double[] increments = new double[3];
        computeIncrementsB2F(increments, rayVector, sampleStep);


        int nrSamples = 1 + (int) Math.floor(VectorMath.distance(entryPoint, exitPoint) / sampleStep);

        RaycastMode mode = getRaycastMode(isFrontMode);
        TFColor color = new TFColor(0, 0, 0, 0);
        switch (mode) {
            case COMPOSITING:
                TransferFunction tFunction = isFrontMode ? tFuncFront : tFuncBack;
                color = compositeCalculationRGB(nrSamples, currentPos, increments, tFunction);
                break;
            case TRANSFER2D:
                TransferFunction2D tFunction2D = isFrontMode ? tFunc2DFront : tFunc2DBack;
                TFColor tfColor = new TFColor(tFunction2D.color.r, tFunction2D.color.g, tFunction2D.color.b, tFunction2D.color.a);
                color = computeTF2DColor(tFunction2D, tfColor, currentPos, increments, nrSamples);
                break;
            case MIP:
                color = traceRayMIP(currentPos, increments, nrSamples);
                break;
            case ISO_SURFACE:
                color = traceRayIso(currentPos, increments, nrSamples, isFrontMode);
                break;
        }
        if (shadingMode && (mode.equals(RaycastMode.COMPOSITING) || mode.equals(RaycastMode.ISO_SURFACE))) {
            TFColor currentColor = new TFColor(color.r, color.g, color.b, color.a);
            VoxelGradient voxGrad = gradients.getGradientTrilinear(currentPos);
            color = computePhongShading(currentColor, voxGrad, lightVector, rayVector);
        }
        return computePackedPixelColor(color.r, color.g, color.b, color.a);
    }

    private boolean isFrontSlice(double[] pointCoord) {
        double[] intersectionPoint = new double[3];
        intersectLinePlane(planePoint, planeNorm, pointCoord, planeNorm, intersectionPoint);
        double[] difference = new double[3];
        VectorMath.difference(pointCoord, intersectionPoint, difference);
        return VectorMath.dotproduct(difference, planeNorm) > 0;
    }

    /**
     * Class constructor. Initializes attributes.
     */
    public RaycastRenderer() {
        panelFront = new RaycastRendererPanel(this);
        panelFront.setSpeedLabel("0");

        isoColorFront = new TFColor();
        isoColorFront.r = 1.0;
        isoColorFront.g = 1.0;
        isoColorFront.b = 0.0;
        isoColorFront.a = 1.0;

        isoColorBack = new TFColor();
        isoColorBack.r = 1.0;
        isoColorBack.g = 1.0;
        isoColorBack.b = 0.0;
        isoColorBack.a = 1.0;

        modeFront = RaycastMode.SLICER;
        modeBack = RaycastMode.SLICER;
    }

    /**
     * Sets the volume to be visualized. It creates the Image buffer for the
     * size of the volume. Initializes the transfers functions
     *
     * @param vol Volume to be visualized.
     */
    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }

        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);

        // Initialize transfer function and GUI panels
        tFuncFront = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFuncFront.setTestFunc();
        tFuncFront.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFuncFront, volume.getHistogram());

        tFunc2DFront = new TransferFunction2D((short) (volume.getMaximum() / 2), 0.2 * volume.getMaximum());
        tfEditor2DFront = new TransferFunction2DEditor(tFunc2DFront, volume, gradients);
        tfEditor2DFront.addTFChangeListener(this);

        // Initialize transfer function and GUI panels for cutting plane
        tFuncBack = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFuncBack.setTestFunc();
        tFuncBack.addTFChangeListener(this);
        tfEditorBack = new TransferFunctionEditor(tFuncBack, volume.getHistogram());

        tFunc2DBack = new TransferFunction2D((short) (volume.getMaximum() / 2), 0.2 * volume.getMaximum());
        tfEditor2DBack = new TransferFunction2DEditor(tFunc2DBack, volume, gradients);
        tfEditor2DBack.addTFChangeListener(this);

        // Set plane point
        VectorMath.setVector(planePoint, volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    /**
     * Do NOT modify.
     * <p>
     * Visualizes the volume. It calls the corresponding render functions.
     *
     * @param gl OpenGL API.
     */
    @Override
    public void visualize(GL2 gl) {
        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        // If mode is Cutting Plane, draw the cutting plane.
        if (cuttingPlaneMode) {
            drawCuttingPlane(gl);
        }

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, _viewMatrix, 0);

        long startTime = System.currentTimeMillis();

        if (RaycastMode.SLICER.equals(modeFront)) {
            slicer(_viewMatrix);
        } else {// Default case raycast
            raycast(_viewMatrix);
        }

        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panelFront.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();

        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }
    }

    public RaycastMode getRaycastMode(boolean isFrontMode) {
        return isFrontMode ? modeFront : modeBack;
    }

    /**
     * Sets the raycast mode to the specified one.
     *
     * @param mode New Raycast mode.
     */
    public void setRaycastModeFront(RaycastMode mode) {
        this.modeFront = mode;
    }

    public void setRaycastModeBack(RaycastMode mode) {
        this.modeBack = mode;
    }

    @Override
    public void changed() {
        for (TFChangeListener listener : listeners) {
            listener.changed();
        }
    }

    /**
     * Do NOT modify.
     * <p>
     * Updates the vectors that represent the cutting plane.
     *
     * @param d View Matrix.
     */
    public void updateCuttingPlaneVectors(double[] d) {
        VectorMath.setVector(_planeU, d[1], d[5], d[9]);
        VectorMath.setVector(_planeV, d[2], d[6], d[10]);
        VectorMath.setVector(planeNorm, d[0], d[4], d[8]);
    }

    /**
     * Sets the cutting plane mode flag.
     *
     * @param cuttingPlaneMode
     */
    public void setCuttingPlaneMode(boolean cuttingPlaneMode) {
        this.cuttingPlaneMode = cuttingPlaneMode;
    }

    public boolean isCuttingPlaneMode() {
        return cuttingPlaneMode;
    }

    /**
     * Sets shading mode flag.
     *
     * @param shadingMode
     */
    public void setShadingMode(boolean shadingMode) {
        this.shadingMode = shadingMode;
    }

    public RaycastRendererPanel getPanel() {
        return panelFront;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2DFront;
    }

    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }

    public TransferFunction2DEditor getTF2DPanelBack() {
        return tfEditor2DBack;
    }

    public TransferFunctionEditor getTFPanelBack() {
        return tfEditorBack;
    }

    //////////////////////////////////////////////////////////////////////
    /////////////////// PRIVATE FUNCTIONS AND ATTRIBUTES /////////////////
    //////////////////////////////////////////////////////////////////////
    /**
     * OpenGL View Matrix. The shape (4x4) remains constant.
     */
    private final double[] _viewMatrix = new double[4 * 4];

    /**
     * Vector used to draw the cutting plane.
     */
    private final double[] _planeU = new double[3];

    /**
     * Vector used to draw the cutting plane.
     */
    private final double[] _planeV = new double[3];

    /**
     * Do NOT modify.
     * <p>
     * Draws the bounding box around the volume.
     *
     * @param gl OpenGL API.
     */
    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();
    }

    /**
     * Do NOT modify.
     * <p>
     * Draws the cutting plane through.
     *
     * @param gl OpenGL API.
     */
    private void drawCuttingPlane(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(2f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        double D = Math.sqrt(Math.pow(volume.getDimX(), 2) + Math.pow(volume.getDimY(), 2) + Math.pow(volume.getDimZ(), 2)) / 2;

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-_planeU[0] * D - _planeV[0] * D, -_planeU[1] * D - _planeV[1] * D, -_planeU[2] * D - _planeV[2] * D);
        gl.glVertex3d(_planeU[0] * D - _planeV[0] * D, _planeU[1] * D - _planeV[1] * D, _planeU[2] * D - _planeV[2] * D);
        gl.glVertex3d(_planeU[0] * D + _planeV[0] * D, _planeU[1] * D + _planeV[1] * D, _planeU[2] * D + _planeV[2] * D);
        gl.glVertex3d(-_planeU[0] * D + _planeV[0] * D, -_planeU[1] * D + _planeV[1] * D, -_planeU[2] * D + _planeV[2] * D);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Sets the Iso value for the front function.
     *
     * @param isoValueFront the new iso value
     */
    public void setIsoValueFront(float isoValueFront) {
        this.isoValueFront = isoValueFront;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Sets the Iso value for the back function.
     *
     * @param isoValueBack new Iso value
     */
    public void setIsoValueBack(float isoValueBack) {
        this.isoValueBack = isoValueBack;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Sets the Iso Color for the front function.
     *
     * @param newColor color that becomes the new Iso color
     */
    public void setIsoColorFront(TFColor newColor) {
        this.isoColorFront.r = newColor.r;
        this.isoColorFront.g = newColor.g;
        this.isoColorFront.b = newColor.b;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Sets the Iso Color for the back function.
     *
     * @param newColor color that becomes the new Iso color
     */
    public void setIsoColorBack(TFColor newColor) {
        this.isoColorBack.r = newColor.r;
        this.isoColorBack.g = newColor.g;
        this.isoColorBack.b = newColor.b;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Resets the image with 0 values.
     */
    private void resetImage() {
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Computes the increments according to sample step and stores the result in
     * increments.
     *
     * @param increments Vector to store the result.
     * @param rayVector  Ray vector.
     * @param sampleStep Sample step.
     */
    private void computeIncrementsB2F(double[] increments, double[] rayVector, double sampleStep) {
        // we compute a back to front compositing so we start increments in the opposite direction than the pixel ray
        VectorMath.setVector(increments, -rayVector[0] * sampleStep, -rayVector[1] * sampleStep, -rayVector[2] * sampleStep);
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Packs a color into a Integer.
     *
     * @param r Red component of the color.
     * @param g Green component of the color.
     * @param b Blue component of the color.
     * @param a Alpha component of the color.
     * @return
     */
    private static int computePackedPixelColor(double r, double g, double b, double a) {
        int c_alpha = a <= 1.0 ? (int) Math.floor(a * 255) : 255;
        int c_red = r <= 1.0 ? (int) Math.floor(r * 255) : 255;
        int c_green = g <= 1.0 ? (int) Math.floor(g * 255) : 255;
        int c_blue = b <= 1.0 ? (int) Math.floor(b * 255) : 255;

        return (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Computes the entry and exit of a view vector with respect the faces of
     * the volume.
     *
     * @param p          Point of the ray.
     * @param viewVec    Direction of the ray.
     * @param entryPoint Vector to store entry point.
     * @param exitPoint  Vector to store exit point.
     */
    private void computeEntryAndExit(double[] p, double[] viewVec, double[] entryPoint, double[] exitPoint) {

        for (int i = 0; i < 3; i++) {
            entryPoint[i] = -1;
            exitPoint[i] = -1;
        }

        double[] plane_pos = new double[3];
        double[] plane_normal = new double[3];
        double[] intersection = new double[3];

        VectorMath.setVector(plane_pos, volume.getDimX(), 0, 0);
        VectorMath.setVector(plane_normal, 1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, -1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, volume.getDimY(), 0);
        VectorMath.setVector(plane_normal, 0, 1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, -1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, volume.getDimZ());
        VectorMath.setVector(plane_normal, 0, 0, 1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, 0, -1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Checks if a line intersects a plane.
     *
     * @param plane_pos    Position of plane.
     * @param plane_normal Normal of plane.
     * @param line_pos     Position of line.
     * @param line_dir     Direction of line.
     * @param intersection Vector to store intersection.
     * @return True if intersection happens. False otherwise.
     */
    private static boolean intersectLinePlane(double[] plane_pos, double[] plane_normal,
                                              double[] line_pos, double[] line_dir, double[] intersection) {

        double[] tmp = new double[3];

        for (int i = 0; i < 3; i++) {
            tmp[i] = plane_pos[i] - line_pos[i];
        }

        double denom = VectorMath.dotproduct(line_dir, plane_normal);
        if (Math.abs(denom) < 1.0e-8) {
            return false;
        }

        double t = VectorMath.dotproduct(tmp, plane_normal) / denom;

        for (int i = 0; i < 3; i++) {
            intersection[i] = line_pos[i] + t * line_dir[i];
        }

        return true;
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Checks if it is a valid intersection.
     *
     * @param intersection Vector with the intersection point.
     * @param xb           beginning of the x component
     * @param xe           end of the x component
     * @param yb           beginning of the y component
     * @param ye           end of the y component
     * @param zb           beginning of the z component
     * @param ze           end of the z component
     * @return
     */
    private static boolean validIntersection(double[] intersection, double xb, double xe, double yb,
                                             double ye, double zb, double ze) {

        return (((xb - 0.5) <= intersection[0]) && (intersection[0] <= (xe + 0.5))
                && ((yb - 0.5) <= intersection[1]) && (intersection[1] <= (ye + 0.5))
                && ((zb - 0.5) <= intersection[2]) && (intersection[2] <= (ze + 0.5)));

    }

    /**
     * Do NOT modify this function.
     * <p>
     * Checks the intersection of a line with a plane and returns entry and exit
     * points in case intersection happens.
     *
     * @param plane_pos    Position of plane.
     * @param plane_normal Normal vector of plane.
     * @param line_pos     Position of line.
     * @param line_dir     Direction of line.
     * @param intersection Vector to store the intersection point.
     * @param entryPoint   Vector to store the entry point.
     * @param exitPoint    Vector to store the exit point.
     */
    private void intersectFace(double[] plane_pos, double[] plane_normal,
                               double[] line_pos, double[] line_dir, double[] intersection,
                               double[] entryPoint, double[] exitPoint) {

        boolean intersect = intersectLinePlane(plane_pos, plane_normal, line_pos, line_dir,
                intersection);
        if (intersect) {

            double xpos0 = 0;
            double xpos1 = volume.getDimX();
            double ypos0 = 0;
            double ypos1 = volume.getDimY();
            double zpos0 = 0;
            double zpos1 = volume.getDimZ();

            if (validIntersection(intersection, xpos0, xpos1, ypos0, ypos1,
                    zpos0, zpos1)) {
                if (VectorMath.dotproduct(line_dir, plane_normal) < 0) {
                    entryPoint[0] = intersection[0];
                    entryPoint[1] = intersection[1];
                    entryPoint[2] = intersection[2];
                } else {
                    exitPoint[0] = intersection[0];
                    exitPoint[1] = intersection[1];
                    exitPoint[2] = intersection[2];
                }
            }
        }
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Calculates the pixel coordinate for the given parameters.
     *
     * @param pixelCoord   Vector to store the result.
     * @param volumeCenter Location of the center of the volume.
     * @param uVec         uVector.
     * @param vVec         vVector.
     * @param i            Pixel i.
     * @param j            Pixel j.
     */
    private void computePixelCoordinatesFloat(double[] pixelCoord, double[] volumeCenter, double[] uVec, double[] vVec, float i, float j) {
        // Coordinates of a plane centered at the center of the volume (volumeCenter and oriented according to the plane defined by uVec and vVec
        float imageCenter = image.getWidth() / 2f;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + volumeCenter[0];
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + volumeCenter[1];
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + volumeCenter[2];
    }

    /**
     * Do NOT modify this function.
     * <p>
     * Calculates the pixel coordinate for the given parameters. It calculates
     * the coordinate having the center (0,0) of the view plane aligned with the
     * center of the volume and moved a distance equivalent to the diagonal to
     * make sure we are far enough.
     *
     * @param pixelCoord Vector to store the result.
     * @param viewVec    View vector (ray).
     * @param uVec       uVector.
     * @param vVec       vVector.
     * @param i          Pixel i.
     * @param j          Pixel j.
     */
    private void computePixelCoordinatesBehindFloat(double[] pixelCoord, double[] viewVec, double[] uVec, double[] vVec, float i, float j) {
        int imageCenter = image.getWidth() / 2;
        // Pixel coordinate is calculate having the center (0,0) of the view plane aligned with the center of the volume and moved a distance equivalent
        // to the diagonal to make sure I am far away enough.

        double diagonal = Math.sqrt((volume.getDimX() * volume.getDimX()) + (volume.getDimY() * volume.getDimY()) + (volume.getDimZ() * volume.getDimZ())) / 2;
        pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) + viewVec[0] * diagonal + volume.getDimX() / 2.0;
        pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) + viewVec[1] * diagonal + volume.getDimY() / 2.0;
        pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) + viewVec[2] * diagonal + volume.getDimZ() / 2.0;
    }

}
