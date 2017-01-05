package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
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


    public static Observable<CaptureResult> fromCapture(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
                dumpRequest(request);
                captureSession.capture(request, new MyLogger(getSessionListener(subscriber)), null);
            }
            catch (CameraAccessException e) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public static Observable<CaptureResult> fromSetRepeatingRequest(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Observable.create(subscriber -> {
            try {
                dumpRequest(request);
                captureSession.setRepeatingRequest(request, new MyLogger(getSessionListener(subscriber)), null);
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
            public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                super.onCaptureProgressed(session, request, partialResult);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(partialResult);
                }
            }

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

            @Override
            public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            }

            @Override
            public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
                super.onCaptureSequenceAborted(session, sequenceId);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
            }
        };
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

    private static void dumpResult(String tag, CaptureResult result) {
        List<CaptureResult.Key<?>> keys = result.getKeys();
        for (CaptureResult.Key<?> key : keys) {
            Object value = result.get(key);
            if (value != null) {
                Log.d("MyLogger", "!!!\t" + tag + "\t" + key + "\t" + value);
            }
        }
    }

    private static void dumpRequest(CaptureRequest request) {
        List<CaptureRequest.Key<?>> keys = request.getKeys();
        for (CaptureRequest.Key<?> key : keys) {
            Object value = request.get(key);
            if (value != null) {
                Log.d("MyLogger", "!!!\t" + request + "\t" + key + "\t" + value);
            }
        }
    }

    private static class MyLogger extends CameraCaptureSession.CaptureCallback {
        private final CameraCaptureSession.CaptureCallback mCallback;

        private MyLogger(CameraCaptureSession.CaptureCallback callback) {
            mCallback = callback;
        }

        private int hasProgressed = 0;
        private static final int PROGRESSED_LIMIT = 3;
        private int hasCompleted = 0;

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request,
                                     long timestamp, long frameNumber) {
            mCallback.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                        CaptureResult partialResult) {
            if (hasProgressed < PROGRESSED_LIMIT) {
                dumpResult("progress \t#" + hasProgressed + "\t for " + request, partialResult);
                hasProgressed++;
            }

            mCallback.onCaptureProgressed(session, request,
                partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result) {
            if (hasCompleted < PROGRESSED_LIMIT) {
                dumpResult("result \t#" + hasCompleted + "\t for " + request, result);
                hasCompleted++;
            }
            mCallback.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request,
                                    CaptureFailure failure) {
            mCallback.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId,
                                               long frameNumber) {
            mCallback.onCaptureSequenceCompleted(session,
                sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            mCallback.onCaptureSequenceAborted(session, sequenceId);
        }
    }


}
