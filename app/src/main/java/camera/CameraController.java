package camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import java.io.File;
import java.util.Arrays;

import camera.CameraRxWrapper.CaptureSessionData;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;


@TargetApi(21)
public class CameraController {

    static final String TAG = CameraController.class.getName();

    @NonNull
    private final Context mContext;
    @NonNull
    private final Callback mCallback;
    private final int mLayoutOrientation;
    @NonNull
    private final File mFile;
    @NonNull
    private final AutoFitTextureView mTextureView;
    @NonNull
    private final WindowManager mWindowManager;
    @NonNull
    private final CameraManager mCameraManager;
    private SurfaceParams mSurfaceParams;
    private ImageReader mImageReader;

    private class CameraParams {
        @NonNull
        private final String cameraId;
        @NonNull
        private final CameraCharacteristics cameraCharacteristics;
        @NonNull
        private final Size previewSize;

        private CameraParams(@NonNull String cameraId, @NonNull CameraCharacteristics cameraCharacteristics, @NonNull Size previewSize) {
            this.cameraId = cameraId;
            this.cameraCharacteristics = cameraCharacteristics;
            this.previewSize = previewSize;
        }
    }

    private class SurfaceParams {
        @NonNull
        private final Surface previewSurface;

        private SurfaceParams(@NonNull Surface previewSurface) {
            this.previewSurface = previewSurface;
        }
    }

    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private final PublishSubject<Object> mOnPauseSubject = PublishSubject.create();
    private final PublishSubject<Object> mOnShutterClick = PublishSubject.create();
    private final PublishSubject<Object> mOnSwitchCameraClick = PublishSubject.create();
    private final PublishSubject<SurfaceTexture> mOnSurfaceTextureAvailable = PublishSubject.create();
    private final ConvergeWaiter mAutoFocusConvergeWaiter = ConvergeWaiter.Factory.createAutoFocusConvergeWaiter();
    private final ConvergeWaiter mAutoExposureConvergeWaiter = ConvergeWaiter.Factory.createAutoExposureConvergeWaiter();


    public CameraController(@NonNull Context context, @NonNull Callback callback, @NonNull String photoFileUrl,
                            @NonNull AutoFitTextureView textureView, int layoutOrientation) {
        mContext = context;
        mCallback = callback;
        mFile = new File(photoFileUrl);
        mTextureView = textureView;
        mLayoutOrientation = layoutOrientation;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

    }

    public void takePhoto() {
        mOnShutterClick.onNext(this);
    }

    public void switchCamera() {
        mOnSwitchCameraClick.onNext(this);
    }

    public AndroidLifecycle getLifecycle() {
        return mLifecycleImpl;
    }

    private CameraParams mCameraParams;
    private final AndroidLifecycle mLifecycleImpl = new AndroidLifecycle() {
        private static final String SIS_CAMERA_ID = "SIS_CAMERA_ID";

        @Override
        public void onCreate(@Nullable Bundle saveState) {
            Log.d(TAG, "\tonCreate");
            String cameraId = null;
            if (saveState != null) {
                cameraId = saveState.getString(SIS_CAMERA_ID);
            }

            try {
                if (cameraId == null) {
                    Log.d(TAG, "\tchoosing default camera");
                    cameraId = CameraStrategy.chooseDefaultCamera(mCameraManager);
                }

                if (cameraId == null) {
                    mCallback.onException(new IllegalStateException("Can't find any camera"));
                    return;
                }

                mCameraParams = getCameraParams(cameraId);
                setTextureAspectRatio(mCameraParams);
            }
            catch (CameraAccessException e) {
                mCallback.onException(e);
                return;
            }

            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "\tonSurfaceTextureAvailable");
                    mOnSurfaceTextureAvailable.onNext(surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "\tonSurfaceTextureSizeChanged");
                    mOnSurfaceTextureAvailable.onNext(surface);
                    //NO-OP
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    Log.d(TAG, "\tonSurfaceTextureDestroyed");
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                }
            });

            // For some reasons onSurfaceSizeChanged is not always called, this is a workaround
            mTextureView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                Log.d(TAG, "\tonLayoutChange");
                if (mTextureView.isAvailable()) {
                    Log.d(TAG, "\tmTextureView.isAvailable()");
                    mOnSurfaceTextureAvailable.onNext(mTextureView.getSurfaceTexture());
                }
            });
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "\tonDestroy");
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putString(SIS_CAMERA_ID, mCameraParams.cameraId);
        }

        @Override
        public void onStart() {
            Log.d(TAG, "\tonStart");
        }

        @Override
        public void onResume() {
            Log.d(TAG, "\tonResume");

            subscribe();

            // When the screen is turned off and turned back on, the SurfaceTexture is already
            // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
            // a camera and start preview from here (otherwise, we wait until the surface is ready in
            // the SurfaceTextureListener).
            if (mTextureView.isAvailable()) {
                Log.d(TAG, "\tmTextureView.isAvailable()");
                mOnSurfaceTextureAvailable.onNext(mTextureView.getSurfaceTexture());
            }
        }


        @Override
        public void onPause() {
            Log.d(TAG, "\tonPause");
            mOnPauseSubject.onNext(this);
        }

        @Override
        public void onStop() {
            Log.d(TAG, "\tonStop");
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

        }
    };

    private CameraParams getCameraParams(@NonNull String cameraId) throws CameraAccessException {
        Log.d(TAG, "\tsetupPreviewSize");
        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
        Size previewSize = CameraStrategy.getPreviewSize(cameraCharacteristics);
        return new CameraParams(cameraId, cameraCharacteristics, previewSize);
    }

    private void setTextureAspectRatio(@NonNull CameraParams cameraParams) {
        // We fit the aspect ratio of TextureView to the size of preview we picked.
        // looks like the dimensions we get from camera characteristics are for Landscape layout, so we swap it for portrait
        if (mLayoutOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(cameraParams.previewSize.getWidth(), cameraParams.previewSize.getHeight());
        }
        else {
            mTextureView.setAspectRatio(cameraParams.previewSize.getHeight(), cameraParams.previewSize.getWidth());
        }
    }

    /**
     * Flow is configured in this method
     */
    private void subscribe() {
        mCompositeDisposable.clear();

        // open camera

        Observable<Pair<CameraRxWrapper.DeviceStateEvents, CameraDevice>> cameraDeviceObservable = mOnSurfaceTextureAvailable
            .firstElement()
            .doAfterSuccess(this::setupSurface)
            .doAfterSuccess(s -> initImageReader())
            .toObservable()
            .flatMap(s -> CameraRxWrapper.openCamera(mCameraParams.cameraId, mCameraManager))
            .share();

        Observable<CameraDevice> openCameraObservable = cameraDeviceObservable
            .filter(pair -> pair.first == CameraRxWrapper.DeviceStateEvents.ON_OPENED)
            .map(pair -> pair.second)
            .share();

        Observable<CameraDevice> closeCameraObservable = cameraDeviceObservable
            .filter(pair -> pair.first == CameraRxWrapper.DeviceStateEvents.ON_CLOSED)
            .map(pair -> pair.second)
            .share();

        // create capture session

        Observable<Pair<CameraRxWrapper.CaptureSessionStateEvents, CameraCaptureSession>> createCaptureSessionObservable = openCameraObservable
            .flatMap(cameraDevice -> CameraRxWrapper
                .createCaptureSession(cameraDevice, Arrays.asList(mSurfaceParams.previewSurface, mImageReader.getSurface()))
            )
            .share();

        Observable<CameraCaptureSession> captureSessionConfiguredObservable = createCaptureSessionObservable
            .filter(pair -> pair.first == CameraRxWrapper.CaptureSessionStateEvents.ON_CONFIGURED)
            .map(pair -> pair.second)
            .share();

        Observable<CameraCaptureSession> captureSessionClosedObservable = createCaptureSessionObservable
            .filter(pair -> pair.first == CameraRxWrapper.CaptureSessionStateEvents.ON_CLOSED)
            .map(pair -> pair.second)
            .share();

        // start preview

        Observable<CaptureSessionData> previewObservable = captureSessionConfiguredObservable
            .flatMap(cameraCaptureSession -> {
                Log.d(TAG, "\tstartPreview");
                CaptureRequest.Builder previewBuilder = createPreviewBuilder(cameraCaptureSession, mSurfaceParams.previewSurface);
                return CameraRxWrapper.fromSetRepeatingRequest(cameraCaptureSession, previewBuilder.build());
            })

            .share();

        // react to shutter button

        mCompositeDisposable.add(
            Observable.combineLatest(previewObservable, mOnShutterClick, (captureSessionData, o) -> captureSessionData)
                .firstElement().toObservable()
                .doOnNext(o -> Log.d(TAG, "\ton shutter click"))
                .doOnNext(state -> mCallback.onFocusStarted())
                .flatMap(this::waitForAf)
                .flatMap(this::waitForAe)
                .doOnNext(state -> mCallback.onFocusFinished())
                .flatMap(captureSessionData1 -> captureStillPicture(captureSessionData1.session))
                .ignoreElements()
                .subscribe(() -> {
                }, this::onError)
        );

        // react to switch camera button

        mCompositeDisposable.add(
            Observable.combineLatest(previewObservable, mOnSwitchCameraClick, (captureSessionData, o) -> captureSessionData)
                .firstElement().toObservable()
                .doOnNext(o -> Log.d(TAG, "\ton switch camera click"))
                .doOnNext(captureSessionData -> captureSessionData.session.close())
                .flatMap(captureSessionData -> captureSessionClosedObservable)
                .doOnNext(cameraCaptureSession -> cameraCaptureSession.getDevice().close())
                .flatMap(cameraCaptureSession -> closeCameraObservable)
                .doOnNext(cameraDevice -> closeImageReader())
                .subscribe(cameraDevice -> switchCameraInternal(), this::onError)
        );

        // react to onPause event

        mCompositeDisposable.add(Observable.combineLatest(previewObservable, mOnPauseSubject, (state, o) -> state)
            .firstElement().toObservable()
            .doOnNext(o -> Log.d(TAG, "\ton pause"))
            .doOnNext(captureSessionData -> captureSessionData.session.close())
            .flatMap(captureSessionData -> captureSessionClosedObservable)
            .doOnNext(cameraCaptureSession -> cameraCaptureSession.getDevice().close())
            .flatMap(cameraCaptureSession -> closeCameraObservable)
            .doOnNext(cameraDevice -> closeImageReader())
            .subscribe(state -> unsubscribe(), this::onError)
        );
    }

    private void onError(Throwable throwable) {
        unsubscribe();
        if (throwable instanceof CameraAccessException) {
            mCallback.onCameraAccessException();
        }
        else if (throwable instanceof OpenCameraException) {
            mCallback.onCameraOpenException(((OpenCameraException) throwable).getReason());
        }
        else {
            mCallback.onException(throwable);
        }
    }

    private void unsubscribe() {
        mCompositeDisposable.clear();
    }

    private void setupSurface(@NonNull SurfaceTexture surfaceTexture) {
        surfaceTexture.setDefaultBufferSize(mCameraParams.previewSize.getWidth(), mCameraParams.previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        mSurfaceParams = new SurfaceParams(previewSurface);
    }

    private void switchCameraInternal() {
        Log.d(TAG, "\tswitchCameraInternal");
        try {
            unsubscribe();
            String cameraId = CameraStrategy.switchCamera(mCameraManager, mCameraParams.cameraId);
            mCameraParams = getCameraParams(cameraId);
            setTextureAspectRatio(mCameraParams);
            subscribe();
            // waiting for textureView to be measured
        }
        catch (CameraAccessException e) {
            onError(e);
        }
    }

    private void initImageReader() {
        Log.d(TAG, "\tinitImageReader");
        Size sizeForImageReader = CameraStrategy.getStillImageSize(mCameraParams.cameraCharacteristics, mCameraParams.previewSize);
        mImageReader = ImageReader.newInstance(sizeForImageReader.getWidth(), sizeForImageReader.getHeight(), ImageFormat.JPEG, 1);
        mCompositeDisposable.add(
            ImageSaverRxWrapper.createOnImageAvailableObservable(mImageReader)
                .observeOn(Schedulers.io())
                .flatMap(imageReader1 -> ImageSaverRxWrapper.save(imageReader1.acquireLatestImage(), mFile).toObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> mCallback.onPhotoTaken(file.getAbsolutePath(), getLensFacingPhotoType()))
        );
    }

    @Nullable
    private Integer getLensFacingPhotoType() {
        return mCameraParams.cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
    }

    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    private Observable<CaptureSessionData> waitForAf(@NonNull CaptureSessionData captureResultParams) {
        return Observable
            .fromCallable(() -> createPreviewBuilder(captureResultParams.session, mSurfaceParams.previewSurface))
            .flatMap(
                previewBuilder -> mAutoFocusConvergeWaiter
                    .waitForConverge(captureResultParams, previewBuilder)
                    .toObservable()
            );
    }

    @NonNull
    private Observable<CaptureSessionData> waitForAe(@NonNull CaptureSessionData captureResultParams) {
        return Observable
            .fromCallable(() -> createPreviewBuilder(captureResultParams.session, mSurfaceParams.previewSurface))
            .flatMap(
                previewBuilder -> mAutoExposureConvergeWaiter
                    .waitForConverge(captureResultParams, previewBuilder)
                    .toObservable()
            );
    }

    @NonNull
    private Observable<CaptureSessionData> captureStillPicture(@NonNull CameraCaptureSession cameraCaptureSession) {
        Log.d(TAG, "\tcaptureStillPicture");
        return Observable
            .fromCallable(() -> createStillPictureBuilder(cameraCaptureSession.getDevice()))
            .flatMap(builder -> CameraRxWrapper.fromCapture(cameraCaptureSession, builder.build()));
    }

    @NonNull
    private CaptureRequest.Builder createStillPictureBuilder(@NonNull CameraDevice cameraDevice) throws CameraAccessException {
        final CaptureRequest.Builder builder;
        builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        builder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
        builder.addTarget(mImageReader.getSurface());
        setup3Auto(builder);

        int rotation = mWindowManager.getDefaultDisplay().getRotation();
        builder.set(CaptureRequest.JPEG_ORIENTATION, CameraOrientationHelper.getJpegOrientation(mCameraParams.cameraCharacteristics, rotation));
        return builder;
    }

    @NonNull
    CaptureRequest.Builder createPreviewBuilder(CameraCaptureSession captureSession, Surface previewSurface) throws CameraAccessException {
        CaptureRequest.Builder builder = captureSession.getDevice().createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        builder.addTarget(previewSurface);
        setup3Auto(builder);
        return builder;
    }

    private void setup3Auto(CaptureRequest.Builder builder) {
        // Enable auto-magical 3A run by camera device
        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);

        Float minFocusDist = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

        // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
        boolean noAFRun = (minFocusDist == null || minFocusDist == 0);

        if (!noAFRun) {
            // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
            int[] afModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (contains(afModes, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
            else {
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            }
        }

        // If there is an auto-magical flash control mode available, use it, otherwise default to
        // the "on" mode, which is guaranteed to always be available.
        int[] aeModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        if (contains(aeModes, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
        else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        }

        // If there is an auto-magical white balance control mode available, use it.
        int[] awbModes = mCameraParams.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (contains(awbModes, CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
            // Allow AWB to run auto-magically if this device supports this
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        }
    }

    private Observable<CameraCaptureSession> closeSessionBlocking(@NonNull CameraCaptureSession cameraCaptureSession, @NonNull Observable<CameraCaptureSession> cameraCaptureSessionObservable) {
        Log.d(TAG, "\tcloseSessionBlocking..." + Thread.currentThread().getName());
        //wait until the capture session observable completes
        cameraCaptureSession.close();
        return cameraCaptureSessionObservable
            .ignoreElements()
            .<CameraCaptureSession>toObservable()
            .switchIfEmpty(Observable.just(cameraCaptureSession));
    }

    private void closeImageReader() {
        Log.d(TAG, "\tcloseImageReader");
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public interface Callback {
        void onFocusStarted();

        void onFocusFinished();

        void onPhotoTaken(@NonNull String photoUrl, @Nullable Integer photoSourceType);

        void onCameraAccessException();

        void onCameraOpenException(@Nullable OpenCameraException.Reason reason);

        void onException(Throwable throwable);
    }

}
