package com.example.arkadygamza.rxcamera2;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.support.annotation.Nullable;

public class OpenCameraException extends Exception {
    @Nullable
    private final Reason mReason;

    public OpenCameraException(@Nullable Reason reason) {
        mReason = reason;
    }

    @Nullable
    public Reason getReason() {
        return mReason;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public enum Reason {
        ERROR_CAMERA_IN_USE(CameraDevice.StateCallback.ERROR_CAMERA_IN_USE),
        ERROR_MAX_CAMERAS_IN_USE(CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE),
        ERROR_CAMERA_DISABLED(CameraDevice.StateCallback.ERROR_CAMERA_DISABLED),
        ERROR_CAMERA_DEVICE(CameraDevice.StateCallback.ERROR_CAMERA_DEVICE),
        ERROR_CAMERA_SERVICE(CameraDevice.StateCallback.ERROR_CAMERA_SERVICE);

        private final int mCameraErrorCode;

        Reason(int cameraErrorCode) {
            mCameraErrorCode = cameraErrorCode;
        }

        @Nullable
        public static Reason getReason(int cameraErrorCode) {
            for (Reason reason : Reason.values()) {
                if (reason.mCameraErrorCode == cameraErrorCode) {
                    return reason;
                }
            }
            return null;
        }
    }
}
