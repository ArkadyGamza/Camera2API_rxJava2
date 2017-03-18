package camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureResult;

class CaptureResultParams {
    final CameraCaptureSession cameraCaptureSession;
    final CaptureResult captureResult;

    CaptureResultParams(CameraCaptureSession cameraCaptureSession, CaptureResult captureResult) {
        this.cameraCaptureSession = cameraCaptureSession;
        this.captureResult = captureResult;
    }
}
