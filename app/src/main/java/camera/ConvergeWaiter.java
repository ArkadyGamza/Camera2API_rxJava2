package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

@TargetApi(21)
class ConvergeWaiter {

    private static final int TIMEOUT_SECONDS = 3;

    private final CaptureRequest.Key<Integer> mRequestTriggerKey;
    private final int mRequestTriggerStartValue;
    private final CaptureResult.Key<Integer> mResultStateKey;
    private final List<Integer> mResultReadyStates;

    private ConvergeWaiter(
        @NonNull CaptureRequest.Key<Integer> requestTriggerKey,
        int requestTriggerStartValue,
        @NonNull CaptureResult.Key<Integer> resultStateKey,
        @NonNull List<Integer> resultReadyStates
    ) {
        mRequestTriggerKey = requestTriggerKey;
        mRequestTriggerStartValue = requestTriggerStartValue;
        mResultStateKey = resultStateKey;
        mResultReadyStates = resultReadyStates;
    }

    @NonNull
    Single<CaptureResultParams> waitForConverge(@NonNull CaptureResultParams captureResultParams, @NonNull CaptureRequest.Builder builder) {
        CaptureRequest previewRequest = builder.build();

        builder.set(mRequestTriggerKey, mRequestTriggerStartValue);
        CaptureRequest triggerRequest = builder.build();

        Observable<CaptureResult> triggerObservable = CameraRxWrapper.fromCapture(captureResultParams.cameraCaptureSession, triggerRequest).toObservable();
        Observable<CaptureResult> previewObservable = CameraRxWrapper.fromSetRepeatingRequest(captureResultParams.cameraCaptureSession, previewRequest).toObservable();
        RequestStateMachine requestStateMachine = new RequestStateMachine();
        Observable<CaptureResultParams> convergeObservable = Observable
            .merge(previewObservable, triggerObservable) //order matters
            .filter(result -> filterWithStateMachine(result, requestStateMachine))
            .firstElement().toObservable()
            .map(result -> captureResultParams);

        Observable<CaptureResultParams> timeOutObservable = Observable
            .just(captureResultParams)
            .delay(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread());

        return Observable
            .merge(convergeObservable, timeOutObservable)
            .firstElement()
            .toSingle();
    }

    private boolean filterWithStateMachine(@NonNull CaptureResult captureResult, @NonNull RequestStateMachine stateMachine) {
        Integer triggerState = captureResult.getRequest().get(mRequestTriggerKey);
        if (triggerState == null) {
            return false;
        }
        return stateMachine.updateAndCheckIfReady(
            triggerState == mRequestTriggerStartValue,
            captureResult.getFrameNumber(),
            isStateReady(captureResult)
        );
    }

    private boolean isStateReady(@NonNull CaptureResult result) {
        Integer aeState = result.get(mResultStateKey);
        return aeState == null || mResultReadyStates.contains(aeState);
    }

    static class Factory {
        private static final List<Integer> afReadyStates = Collections.unmodifiableList(
            Arrays.asList(
                CaptureResult.CONTROL_AF_STATE_INACTIVE,
                CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            )
        );

        private static final List<Integer> aeReadyStates = Collections.unmodifiableList(
            Arrays.asList(
                CaptureResult.CONTROL_AE_STATE_INACTIVE,
                CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
                CaptureResult.CONTROL_AE_STATE_CONVERGED,
                CaptureResult.CONTROL_AE_STATE_LOCKED
            )
        );

        static ConvergeWaiter createAutoFocusConvergeWaiter() {
            return new ConvergeWaiter(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START,
                CaptureResult.CONTROL_AF_STATE,
                afReadyStates
            );
        }

        static ConvergeWaiter createAutoExposureConvergeWaiter() {
            return new ConvergeWaiter(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START,
                CaptureResult.CONTROL_AE_STATE,
                aeReadyStates
            );
        }
    }
}
