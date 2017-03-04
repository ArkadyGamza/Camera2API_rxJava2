package camera;

import android.hardware.camera2.CaptureResult;

class CaptureResultParams {
    final CameraCaptureSessionParams mCameraCaptureSessionParams;
    final CaptureResult mCaptureResult;

    CaptureResultParams(CameraCaptureSessionParams cameraCaptureSessionParams, CaptureResult captureResult) {
        mCameraCaptureSessionParams = cameraCaptureSessionParams;
        mCaptureResult = captureResult;
    }
}
