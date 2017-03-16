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

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

/**
 * Helper class, creates Observables from camera async methods.
 */
@TargetApi(21)
public class CameraRxWrapper {

    private static final String TAG = CameraRxWrapper.class.getName();

    static Observable<CaptureResult> fromCapture(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable
            .create(source -> captureSession.capture(request, getSessionListener(source), null));
    }

    static Observable<CaptureResult> fromSetRepeatingRequest(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable
            .create(source -> captureSession.setRepeatingRequest(request, getSessionListener(source), null));
    }

    @NonNull
    private static CameraCaptureSession.CaptureCallback getSessionListener(final ObservableEmitter<CaptureResult> source) {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                if (!source.isDisposed()) {
                    source.onNext(result);
                    source.onComplete();
                }
            }

            @Override
            public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                if (!source.isDisposed()) {
                    source.onError(new CameraCaptureFailedException(failure));
                }
            }
        };
    }

    @NonNull
    public static Observable<CameraCaptureSession> createCaptureSession(
        @NonNull CameraDevice cameraDevice, @NonNull ImageReader imageReader, @NonNull Surface previewSurface) {
        return Observable.create(source -> {
            try {
                Log.d(TAG, "\tcreateCaptureSession");
                List<Surface> outputs = Arrays.asList(previewSurface, imageReader.getSurface());
                cameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onConfigured");
                        if (!source.isDisposed()) {
                            source.onNext(session);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onConfigureFailed");
                        if (!source.isDisposed()) {
                            source.onError(new CameraCaptureSessionException());
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraCaptureSession session) {
                        Log.d(TAG, "\tcreateCaptureSession - onClosed");
                        if (!source.isDisposed()) {
                            source.onComplete();
                        }
                    }
                }, null);
            }
            catch (CameraAccessException e) {
                if (!source.isDisposed()) {
                    source.onError(e);
                }
            }
        });
    }

    public static Observable<CameraDevice> openCamera(@NonNull String cameraId, @NonNull CameraManager cameraManager) {
        return Observable.create(subscriber -> {
            try {
                Log.d(TAG, "\topenCamera");
                cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "\topenCamera - onOpened");
                        if (!subscriber.isDisposed()) {
                            subscriber.onNext(cameraDevice);
                        }
                    }

                    @Override
                    public void onClosed(@NonNull CameraDevice cameraDevice) {
                        Log.d(TAG, "\topenCamera - onClosed");
                        if (!subscriber.isDisposed()) {
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        if (!subscriber.isDisposed()) {
                            subscriber.onError(new CameraDisconnectedException());
                        }
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.d(TAG, "\topenCamera - onError");
                        if (!subscriber.isDisposed()) {
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
