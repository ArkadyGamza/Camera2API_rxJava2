package camera;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;

import rx.Observable;

@TargetApi(21)
class AfHelper {

    private static boolean filterAeWithStateMachine(@NonNull CaptureResult captureResult, @NonNull TriggerStateMachine stateMachine) {
        return stateMachine.updateAndCheckIfReady(
            captureResult.getRequest().get(CaptureRequest.CONTROL_AF_TRIGGER) == CameraMetadata.CONTROL_AF_TRIGGER_START,
            captureResult.getFrameNumber(),
            checkResultAfState(captureResult)
        );
    }

    @NonNull
    static Observable<CameraController.State> waitForAf(CameraController.State state, CaptureRequest.Builder builder) {
        Log.d(CameraController.TAG, "\twait for Focus");

        CaptureRequest previewRequest = builder.build();

        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        CaptureRequest triggerRequest = builder.build();

        Observable<CaptureResult> afTriggerObservable = CameraRxWrapper.fromCapture(state.captureSession, triggerRequest);
        Observable<CaptureResult> previewObservable = CameraRxWrapper.fromSetRepeatingRequest(state.captureSession, previewRequest);
        TriggerStateMachine afTriggerStateMachine = new TriggerStateMachine();
        return Observable
            .merge(previewObservable, afTriggerObservable)
            .filter(result -> filterAeWithStateMachine(result, afTriggerStateMachine))
            .first()
            .map(result -> state);
    }

    private static boolean checkResultAfState(CaptureResult result) {
        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
        if (afState == null) {
            return true;
        }
        return afState == CaptureResult.CONTROL_AF_STATE_INACTIVE
            || afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
            || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED;
    }
}
