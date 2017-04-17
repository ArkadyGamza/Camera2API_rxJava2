package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import java.util.Arrays;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.Observable;

/**
 * Helper class, creates Observables from camera async methods.
 */
@TargetApi(21)
public class CameraRxWrapper {

    private static final String TAG = CameraRxWrapper.class.getName();

    /**
     * @see CameraDevice.StateCallback
     */
    public enum OpenCameraEvents {
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

    public static Observable<Pair<OpenCameraEvents, CameraDevice>> openCamera(
        @NonNull String cameraId,
        @NonNull CameraManager cameraManager
    ) {
        return Observable.create(observableEmitter -> {
            Log.d(TAG, "\topenCamera");

            observableEmitter.setCancellable(() -> Log.d(TAG, "\topenCamera - unsubscribed")); //todo think of close camera here

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "\topenCamera - onOpened");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(OpenCameraEvents.ON_OPENED, cameraDevice));
                    }
                }

                @Override
                public void onClosed(@NonNull CameraDevice cameraDevice) {
                    Log.d(TAG, "\topenCamera - onClosed");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(OpenCameraEvents.ON_CLOSED, cameraDevice));
                        observableEmitter.onComplete();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(OpenCameraEvents.ON_DISCONNECTED, cameraDevice));
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
    public enum CreateCaptureSessionEvents {
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
    public static Observable<Pair<CreateCaptureSessionEvents, CameraCaptureSession>> createCaptureSession(
        @NonNull CameraDevice cameraDevice,
        @NonNull ImageReader imageReader,
        @NonNull Surface previewSurface
    ) {
        return Observable.create(observableEmitter -> {
            Log.d(TAG, "\tcreateCaptureSession");
            List<Surface> outputs = Arrays.asList(previewSurface, imageReader.getSurface());

            observableEmitter.setCancellable(() -> Log.d(TAG, "\tcreateCaptureSession - unsubscribed")); //todo think of close session here

            cameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onConfigured");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CreateCaptureSessionEvents.ON_CONFIGURED, session));
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
                        observableEmitter.onNext(new Pair<>(CreateCaptureSessionEvents.ON_READY, session));
                    }
                }

                @Override
                public void onActive(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onActive");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CreateCaptureSessionEvents.ON_ACTIVE, session));
                    }
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "\tcreateCaptureSession - onClosed");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CreateCaptureSessionEvents.ON_CLOSED, session));
                        observableEmitter.onComplete();
                    }
                }

                @Override
                public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
                    Log.d(TAG, "\tcreateCaptureSession - onSurfacePrepared");
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onNext(new Pair<>(CreateCaptureSessionEvents.ON_SURFACE_PREPARED, session));
                    }
                }
            }, null);
        });
    }


    static Maybe<CaptureResultParams> fromCapture(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Maybe
            .create(source -> captureSession.capture(request, getSessionListener(source), null));
    }

    static Maybe<CaptureResultParams> fromSetRepeatingRequest(@NonNull CameraCaptureSession captureSession, @NonNull CaptureRequest request) {
        return Maybe
            .create(source -> captureSession.setRepeatingRequest(request, getSessionListener(source), null));
    }

    @NonNull
    private static CameraCaptureSession.CaptureCallback getSessionListener(final MaybeEmitter<CaptureResultParams> source) {
        return new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                if (!source.isDisposed()) {
                    source.onSuccess(new CaptureResultParams(session, result));
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
