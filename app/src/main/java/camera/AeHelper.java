package camera;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.support.annotation.NonNull;
import android.util.Log;

import rx.Observable;

@TargetApi(21)
class AeHelper {

    private static boolean filterAeWithStateMachine(@NonNull CaptureResult captureResult, @NonNull TriggerStateMachine stateMachine) {
        return stateMachine.updateAndCheckIfReady(
            captureResult.getRequest().get(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER) == CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START,
            captureResult.getFrameNumber(),
            checkResultAeState(captureResult)
        );
    }

    @NonNull
    static Observable<CameraController.State> waitForAe(CameraController.State state, CaptureRequest.Builder builder) {
        Log.d(CameraController.TAG, "\twait for Exposure");

        CaptureRequest previewRequest = builder.build();

        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        CaptureRequest triggerRequest = builder.build();

        Observable<CaptureResult> aeTriggerObservable = CameraRxWrapper.fromCapture(state.captureSession, triggerRequest);
        Observable<CaptureResult> previewObservable = CameraRxWrapper.fromSetRepeatingRequest(state.captureSession, previewRequest);
        TriggerStateMachine triggerStateMachine = new TriggerStateMachine();
        return Observable
            .merge(previewObservable, aeTriggerObservable)
            .filter(result -> filterAeWithStateMachine(result, triggerStateMachine))
            .first()
            .map(result -> state);
    }

    private static boolean checkResultAeState(CaptureResult result) {
        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
        if (aeState == null) {
            return false;
        }

        return aeState == CaptureResult.CONTROL_AE_STATE_INACTIVE
            || aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
            || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
            || aeState == CaptureResult.CONTROL_AE_STATE_LOCKED;
    }

}
