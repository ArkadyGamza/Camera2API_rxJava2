package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * Helper class, creates Observables from camera async methods.
 */
@TargetApi(21)
public class CameraRxWrapper {

    private static final String TAG = CameraRxWrapper.class.getName();


    static Observable<CaptureResult> fromCapture(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
//                dumpRequest(request);
                captureSession.capture(request, getSessionListener(subscriber), null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    static Observable<CaptureResult> fromSetRepeatingRequest(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
                captureSession.setRepeatingRequest(request, getSessionListener(subscriber), null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @NonNull
    private static CameraCaptureSession.CaptureCallback getSessionListener(final Subscriber<? super CaptureResult> subscriber) {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(result);
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(new CameraCaptureFailedException(failure));
                }
            }
        };
    }

    @NonNull
    public static Observable<CameraController.State> createCaptureSession(@NonNull ImageReader imageReader, @NonNull CameraController.State state, @NonNull Surface previewSurface) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\tcreateCaptureSession");
                List<Surface> outputs = Arrays.asList(previewSurface, imageReader.getSurface());
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

    public static Observable<CameraController.State> openCamera(@NonNull String cameraId, @NonNull CameraManager cameraManager, @NonNull CameraController.State state) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\topenCamera");
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "\topenCamera - onOpened");
                        state.cameraDevice = cameraDevice;
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(state);
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraDevice cameraDevice) {
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
