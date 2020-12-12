/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.util.Arrays;

/**
 * @author michel
 */
public class GradientVolume {

    /**
     * Constant Zero gradient.
     */
    private final static VoxelGradient ZERO_GRADIENT = new VoxelGradient();

    public GradientVolume(Volume vol) {
        volume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        data = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

    private VoxelGradient getGradient(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }

    /**
     * Gets the corresponding VoxelGradient using Nearest Neighbors.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel gradient.
     */
    public VoxelGradient getGradient(double[] coord) {
        // Get the coordinates
        double dx = coord[0];
        double dy = coord[1];
        double dz = coord[2];

        // Verify they are inside the volume gradient
        if (dx < 0 || dx > (getDimX() - 2) || dy < 0 || dy > (getDimY() - 2)
                || dz < 0 || dz > (getDimZ() - 2)) {

            // If not, just return a zero gradient
            return ZERO_GRADIENT;
        }

        // Get the closest x, y, z to dx, dy, dz that are integers
        // This is important as our data is discrete (not continuous)
        int x = (int) Math.round(dx);
        int y = (int) Math.round(dy);
        int z = (int) Math.round(dz);

        // Finally, get the gradient from GradientVolume for the corresponding coordinates
        return getGradient(x, y, z);
    }

    /**
     * Gets the corresponding VoxelGradient using Tri-linear interpolation.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel gradient.
     */
    public VoxelGradient getGradientTrilinear(double[] coord) {

        if (coord[0] < 0 || coord[0] > (volume.getDimX() - 2) || coord[1] < 0 || coord[1] > (volume.getDimY() - 2)
                || coord[2] < 0 || coord[2] > (volume.getDimZ() - 2)) {
            return ZERO_GRADIENT;
        }
        /* notice that in this framework we assume that the distance between neighbouring voxels is 1 in all directions*/
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

        float xFactor = (float) coord[0] - x;
        float yFactor = (float) coord[1] - y;
        float zFactor = (float) coord[2] - z;

        VoxelGradient g0, g1, g2, g3, g4, g5;

        // Interpolate the x-axis
        g0 = interpolate(getGradient(x, y, z), getGradient(x + 1, y, z), xFactor);
        g1 = interpolate(getGradient(x, y + 1, z), getGradient(x + 1, y + 1, z), xFactor);
        g2 = interpolate(getGradient(x, y, z + 1), getGradient(x + 1, y, z + 1), xFactor);
        g3 = interpolate(getGradient(x, y + 1, z + 1), getGradient(x + 1, y + 1, z + 1), xFactor);

        // Interpolate the y-axis
        g4 = interpolate(g0, g1, yFactor);
        g5 = interpolate(g2, g3, yFactor);

        // Interpolate the z-axis and return the result
        return interpolate(g4, g5, zFactor);
    }


    public void setGradient(int x, int y, int z, VoxelGradient value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, VoxelGradient value) {
        data[i] = value;
    }

    public VoxelGradient getVoxel(int i) {
        return data[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }

    /**
     * Computes the gradient information of the volume according to Levoy's
     * paper.
     */
    private void compute() {
        // this just initializes all gradients to the vector (0,0,0)
        Arrays.fill(data, zero);
        for (int i=1; i< volume.getDimX()-1; i++) {
            for (int j = 1; j < volume.getDimY() - 1; j++) {
                for (int k = 1; k < volume.getDimZ() - 1; k++) {
                    double gx = (volume.getVoxel(i - 1, j, k) - volume.getVoxel(i + 1, j, k)) / 2.0;
                    double gy = (volume.getVoxel(i, j - 1, k) - volume.getVoxel(i, j + 1, k)) / 2.0;
                    double gz = (volume.getVoxel(i, j, k - 1) - volume.getVoxel(i, j, k + 1)) / 2.0;
                    float gxf = (float) gx;
                    float gyf = (float) gy;
                    float gzf = (float) gz;
                    setGradient(i, j, k, new VoxelGradient(gxf, gyf, gzf));
                }
            }
        }

    }

    private VoxelGradient interpolate(VoxelGradient g0, VoxelGradient g1, float factor) {
        VoxelGradient result = new VoxelGradient();
        result.x = g1.x * factor + g0.x * (1 - factor);
        result.y = g1.y * factor + g0.y * (1 - factor);
        result.z = g1.z * factor + g0.z * (1 - factor);
        result.mag = (float) Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);

        return result;
    }

    public double getMaxGradientMagnitude() {
        if (maxmag >= 0) {
            return maxmag;
        } else {
            double magnitude = data[0].mag;
            for (int i = 0; i < data.length; i++) {
                magnitude = data[i].mag > magnitude ? data[i].mag : magnitude;
            }
            maxmag = magnitude;
            return magnitude;
        }
    }

    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] data;
    Volume volume;
    double maxmag;
}
