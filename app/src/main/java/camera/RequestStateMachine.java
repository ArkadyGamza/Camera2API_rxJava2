package camera;

/**
 * Waits for the first response to trigger request, then waits for ready response (to any request).
 */
class RequestStateMachine {
    private boolean mIsWaitingForTrigger = true;
    private long mLastTriggeredFrameNumber;

    public boolean updateAndCheckIfReady(boolean isResponseForTriggeredRequest, long frameNumber, boolean isResonseStateReady) {
        if (mIsWaitingForTrigger) {
            if (isResponseForTriggeredRequest && frameNumber >= mLastTriggeredFrameNumber) {
                mLastTriggeredFrameNumber = frameNumber;
                mIsWaitingForTrigger = false;
            }
        }

        if (!mIsWaitingForTrigger) {
            if (frameNumber >= mLastTriggeredFrameNumber && isResonseStateReady) {
                return true;
            }
        }

        return false;
    }
}
