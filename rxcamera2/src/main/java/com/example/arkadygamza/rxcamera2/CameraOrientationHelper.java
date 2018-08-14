package com.example.arkadygamza.rxcamera2;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

@TargetApi(21)
class CameraOrientationHelper {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray JPEG_ORIENTATIONS = new SparseIntArray();

    static {
        JPEG_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        JPEG_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param screenRotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    static int getJpegOrientation(@NonNull CameraCharacteristics characteristics, int screenRotation) {
        int sensorOrientation = getSensorOrientation(characteristics);

        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (JPEG_ORIENTATIONS.get(screenRotation) + sensorOrientation + 270) % 360;
    }

    /**
     * Returns degrees from 0, 90, 180, 270
     * @see CameraCharacteristics#SENSOR_ORIENTATION
     */
    private static int getSensorOrientation(@NonNull CameraCharacteristics characteristics) {
        Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (sensorOrientation == null) {
            return 0;
        }
        return sensorOrientation;
    }

    /**
     * Converts values provided by {@link Display#getRotation()} into degrees
     * @param windowManager
     */
    public static int rotationInDegrees(@NonNull WindowManager windowManager){
        switch (windowManager.getDefaultDisplay().getRotation()){
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    /**
     * Sensor could be rotated in the device, this method returns normal orientation sensor dimension
     */
    public static Size getSensorSizeRotated(@NonNull CameraCharacteristics characteristics, @NonNull Size sensorSize) {
        int sensorOrientationDegrees = CameraOrientationHelper.getSensorOrientation(characteristics);

        if (sensorOrientationDegrees % 180 == 0) {
            return sensorSize;
        }

        // swap dimensions
        return new Size(sensorSize.getHeight(), sensorSize.getWidth());
    }

}
