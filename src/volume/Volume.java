/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author michel
 */
public class Volume {
    
    public Volume(int xd, int yd, int zd) {
        data = new short[xd*yd*zd];
        dimX = xd;
        dimY = yd;
        dimZ = zd;
    }
    
    public Volume(File file) {
        
        try {
            VolumeIO reader = new VolumeIO(file);
            dimX = reader.getXDim();
            dimY = reader.getYDim();
            dimZ = reader.getZDim();
            data = reader.getData().clone();
            computeHistogram();
        } catch (IOException ex) {
            System.out.println("IO exception");
        }
        
    }
    
    
    public short getVoxel(int x, int y, int z) {
        return data[x + dimX*(y + dimY * z)];
    }
    
    public void setVoxel(int x, int y, int z, short value) {
        data[x + dimX*(y + dimY*z)] = value;
    }

    public void setVoxel(int i, short value) {
        data[i] = value;
    }
    
    public short getVoxel(int i) {
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

    public short getMinimum() {
        short minimum = data[0];
        for (short datum : data) {
            minimum = datum < minimum ? datum : minimum;
        }
        return minimum;
    }

    public short getMaximum() {
        short maximum = data[0];
        for (short datum : data) {
            maximum = datum > maximum ? datum : maximum;
        }
        return maximum;
    }

    public int[] getHistogram() {
        return histogram;
    }

    /**
     * Gets the corresponding voxel using Tri-linear Interpolation.
     *
     * @param coord Pixel coordinate in 3D space of the voxel we want to get.
     * @return The voxel value.
     */
    public float getVoxelTrilinear(double[] coord) {
        if (coord[0] < 0 || coord[0] > (getDimX() - 2) || coord[1] < 0 || coord[1] > (getDimY() - 2)
                || coord[2] < 0 || coord[2] > (getDimZ() - 2)) {
            return 0;
        }
        /* notice that in this framework we assume that the distance between neighbouring voxels is 1 in all directions*/
        int x = (int) Math.floor(coord[0]);
        int y = (int) Math.floor(coord[1]);
        int z = (int) Math.floor(coord[2]);

        float facX = (float) coord[0] - x;
        float facY = (float) coord[1] - y;
        float facZ = (float) coord[2] - z;

        float t0 = interpolateVoxel(getVoxel(x, y, z), getVoxel(x + 1, y, z), facX);
        float t1 = interpolateVoxel(getVoxel(x, y + 1, z), getVoxel(x + 1, y + 1, z), facX);
        float t2 = interpolateVoxel(getVoxel(x, y, z + 1), getVoxel(x + 1, y, z + 1), facX);
        float t3 = interpolateVoxel(getVoxel(x, y + 1, z + 1), getVoxel(x + 1, y + 1, z + 1), facX);
        float t4 = interpolateVoxel(t0, t1, facY);
        float t5 = interpolateVoxel(t2, t3, facY);

        return interpolateVoxel(t4, t5, facZ);
    }

    private float interpolateVoxel(float g0, float g1, float factor) {
        return (1 - factor) * g0 + factor * g1;
    }

    private void computeHistogram() {
        histogram = new int[getMaximum() + 1];
        for (short datum : data) {
            histogram[datum]++;
        }
    }

    private int dimX, dimY, dimZ;
    private short[] data;
    private int[] histogram;
}
