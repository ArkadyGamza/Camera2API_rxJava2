package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

import rx.Observable;

/**
 * Helper class, creates Observables from camera async methods.
 */
@TargetApi(21)
public class CameraRxWrapper {

    private static final String TAG = CameraRxWrapper.class.getName();

    public static Observable<CameraController.State> capture(@NonNull CameraController.State state, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
                state.captureSession.stopRepeating();
                state.captureSession.capture(request, new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                            subscriber.onCompleted();
                        }
                    }

                    @Override
                    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(new CameraCaptureFailedException(failure));
                        }
                    }
                }, null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<CameraController.State> setRepeatingRequest(@NonNull CameraController.State state, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\tsetRepeatingRequest");

                if (!state.previewSurface.isValid()) {
                    Log.d(TAG, "\tsetRepeatingRequest - surface is not valid!");
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                    return;
                }

                state.captureSession.stopRepeating();
                state.captureSession.setRepeatingRequest(request, new CameraCaptureSession.CaptureCallback() {

                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }

                    @Override
                    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                        subscriber.onError(new CameraCaptureFailedException(failure));
                    }

                }, null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @NonNull
    public static Observable<CameraController.State> createCaptureSession(@NonNull CameraController.State state) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\tcreateCaptureSession");
                List<Surface> outputs = Arrays.asList(state.previewSurface, state.imageReader.getSurface());
                state.cameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onConfigured");
                        state.captureSession = session;
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onConfigureFailed");
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(new CameraCaptureSessionException());
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onClosed");
                        state.captureSession = null;
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }
                }, null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<CameraController.State> openCamera(@NonNull CameraController.State state) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\topenCamera");
                state.cameraManager.openCamera(state.cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        Log.d(TAG, "\topenCamera - onOpened");
                        state.cameraDevice = camera;
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraDevice camera) {
                        Log.d(TAG, "\topenCamera - onClosed");
                        state.cameraDevice = null;
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(new CameraDisconnectedException());
                        }
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.d(TAG, "\topenCamera - onError");
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(new OpenCameraException(OpenCameraException.Reason.getReason(error)));
                        }
                    }
                }, null);
            }
            catch (CameraAccessException e) {
                subscriber.onError(e);
            }
        });

    }

    public static class CameraCaptureFailedException extends Exception {
        public final CaptureFailure mFailure;

        public CameraCaptureFailedException(CaptureFailure failure) {
            mFailure = failure;
        }
    }

    public static class CameraCaptureSessionException extends Exception {
    }

    public static class CameraDisconnectedException extends Exception {
    }

}
