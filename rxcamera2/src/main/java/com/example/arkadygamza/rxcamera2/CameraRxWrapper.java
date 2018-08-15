package com.example.arkadygamza.rxcamera2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/**
 * Helper class, creates Observables from camera async methods.
 */
@TargetApi(21)
public class CameraRxWrapper {

    private static final String TAG = CameraRxWrapper.class.getName();

    /**
     * @see CameraDevice.StateCallback
     */
    public enum DeviceStateEvents {
        /**
         * @see CameraDevice.StateCallback#onOpened(CameraDevice)
         */
        ON_OPENED,
        /**
         * @see CameraDevice.StateCallback#onClosed(CameraDevice)
         */
        ON_CLOSED,
        /**
         * @see CameraDevice.StateCallback#onDisconnected(CameraDevice)
         */
        ON_DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    public static Observable<Pair<DeviceStateEvents, CameraDevice>> openCamera(
        @NonNull String cameraId,
        @NonNull CameraManager cameraManager
    ) {
        return Observable.create(observableEmitter -> {
            Log.d(TAG, "\topenCamera");

            observableEmitter.setCancellable(() -> Log.d(TAG, "\topenCamera - unsubscribed"));

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "\topenCamera - onOpened");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(DeviceStateEvents.ON_OPENED, cameraDevice));
                    }
                }

                @Override
                public void onClosed(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "\topenCamera - onClosed");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(DeviceStateEvents.ON_CLOSED, cameraDevice));
                        observableEmitter.onComplete();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(DeviceStateEvents.ON_DISCONNECTED, cameraDevice));
                        observableEmitter.onComplete();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.d(TAG, "\topenCamera - onError");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(new OpenCameraException(OpenCameraException.Reason.getReason(error)));
                    }
                }
            }, null);
        });
    }

    /**
     * @see CameraCaptureSession.StateCallback
     */
    public enum CaptureSessionStateEvents {
        /**
         * @see CameraCaptureSession.StateCallback#onConfigured(CameraCaptureSession)
         */
        ON_CONFIGURED,
        /**
         * @see CameraCaptureSession.StateCallback#onReady(CameraCaptureSession)
         */
        ON_READY,
        /**
         * @see CameraCaptureSession.StateCallback#onActive(CameraCaptureSession)
         */
        ON_ACTIVE,
        /**
         * @see CameraCaptureSession.StateCallback#onClosed(CameraCaptureSession)
         */
        ON_CLOSED,
        /**
         * @see CameraCaptureSession.StateCallback#onSurfacePrepared(CameraCaptureSession, android.view.Surface)
         */
        ON_SURFACE_PREPARED
    }

    @NonNull
    public static Observable<Pair<CaptureSessionStateEvents, CameraCaptureSession>> createCaptureSession(
        @NonNull CameraDevice cameraDevice,
        @NonNull List<Surface> surfaceList
    ) {
        return Observable.create(observableEmitter -> {
            Log.d(TAG, "\tcreateCaptureSession");
            observableEmitter.setCancellable(() -> Log.d(TAG, "\tcreateCaptureSession - unsubscribed"));

            cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onConfigured");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CaptureSessionStateEvents.ON_CONFIGURED, session));
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onConfigureFailed");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(new CreateCaptureSessionException(session));
                    }
                }

                @Override
                public void onReady(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onReady");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CaptureSessionStateEvents.ON_READY, session));
                    }
                }

                @Override
                public void onActive(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onActive");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CaptureSessionStateEvents.ON_ACTIVE, session));
                    }
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onClosed");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CaptureSessionStateEvents.ON_CLOSED, session));
                        observableEmitter.onComplete();
                    }
                }

                @Override
                public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
                    Log.d(TAG, "\tcreateCaptureSession - onSurfacePrepared");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CaptureSessionStateEvents.ON_SURFACE_PREPARED, session));
                    }
                }
            }, null);
        });
    }

    /**
     * @see CameraCaptureSession.CaptureCallback
     */
    public enum CaptureSessionEvents {
        ON_STARTED,
        ON_PROGRESSED,
        ON_COMPLETED,
        ON_SEQUENCE_COMPLETED,
        ON_SEQUENCE_ABORTED
    }

    public static class CaptureSessionData {
        final CaptureSessionEvents event;
        final CameraCaptureSession session;
        final CaptureRequest request;
        final CaptureResult result;

        CaptureSessionData(CaptureSessionEvents event, CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
            this.event = event;
            this.session = session;
            this.request = request;
            this.result = result;
        }
    }

    /**
     * Warning, emits a lot!
     */
    static Observable<CaptureSessionData> fromSetRepeatingRequest(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable
            .create(observableEmitter -> captureSession.setRepeatingRequest(request, createCaptureCallback(observableEmitter), null));
    }

    static Observable<CaptureSessionData> fromCapture(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable
            .create(observableEmitter -> captureSession.capture(request, createCaptureCallback(observableEmitter), null));
    }

@NonNull
private static CameraCaptureSession.CaptureCallback createCaptureCallback(final ObservableEmitter<CaptureSessionData> observableEmitter) {
    return new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            if (!observableEmitter.isDisposed()) {
                observableEmitter.onNext(new CaptureSessionData(CaptureSessionEvents.ON_COMPLETED, session, request, result));
            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            if (!observableEmitter.isDisposed()) {
                observableEmitter.onError(new CameraCaptureFailedException(failure));
            }
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        }

        @Override
        public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        }
    };
}

    public static class CreateCaptureSessionException extends Exception {
        public final CameraCaptureSession session;

        public CreateCaptureSessionException(CameraCaptureSession session) {
            this.session = session;
        }
    }

    public static class CameraCaptureFailedException extends Exception {

        public final CaptureFailure mFailure;

        public CameraCaptureFailedException(CaptureFailure failure) {
            mFailure = failure;
        }

    }

}
