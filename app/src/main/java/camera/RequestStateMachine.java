package camera;

/**
 * Waits for the first response to trigger request, then waits for ready response (to any request).
 */
class RequestStateMachine {
    private boolean mIsWaitingForTrigger = true;
    private long mLastTriggeredFrameNumber;

    boolean updateAndCheckIfReady(boolean isResponseForTriggeredRequest, long frameNumber, boolean isResponseStateReady) {
        if (mIsWaitingForTrigger) {
            if (isResponseForTriggeredRequest && frameNumber >= mLastTriggeredFrameNumber) {
                mLastTriggeredFrameNumber = frameNumber;
                mIsWaitingForTrigger = false;
            }
        }

        if (!mIsWaitingForTrigger) {
            if (frameNumber >= mLastTriggeredFrameNumber && isResponseStateReady) {
                return true;
            }
        }

        return false;
    }
}
