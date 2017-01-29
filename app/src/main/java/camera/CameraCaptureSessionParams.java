package camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;

class CameraCaptureSessionParams {
    final CameraDevice cameraDevice;
    final CameraCaptureSession cameraCaptureSession;

    CameraCaptureSessionParams(CameraDevice cameraDevice, CameraCaptureSession cameraCaptureSession) {
        this.cameraDevice = cameraDevice;
        this.cameraCaptureSession = cameraCaptureSession;
    }
}
