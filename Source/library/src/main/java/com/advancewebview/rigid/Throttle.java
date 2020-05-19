package com.advancewebview.rigid;

import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class used to "throttle" a flow of events.
 * <p>
 * When {@link #onEvent()} is called, it calls the callback in a certain timeout later.
 * Initially {@link #mMinTimeout} is used as the timeout, but if it gets multiple {@link #onEvent}
 * calls in a certain amount of time, it extends the timeout, until it reaches {@link #mMaxTimeout}.
 * <p>
 * This class is primarily used to throttle content changed events.
 */
public class Throttle {
    public static final boolean DEBUG = false; // Don't submit with true
    public static final int DEFAULT_MIN_TIMEOUT = 150;
    public static final int DEFAULT_MAX_TIMEOUT = 2500;
    /* package */ static final int TIMEOUT_EXTEND_INTERVAL = 500;
    private static final Timer TIMER = new Timer();
    private final Clock mClock;
    private final Timer mTimer;
    /**
     * Name of the instance.  Only for logging.
     */
    private final String mName;
    /**
     * Handler for UI thread.
     */
    private final Handler mHandler;
    /**
     * Callback to be called
     */
    private final Runnable mCallback;
    /**
     * Minimum (default) timeout, in milliseconds.
     */
    private final int mMinTimeout;
    /**
     * Max timeout, in milliseconds.
     */
    private final int mMaxTimeout;
    /**
     * Current timeout, in milliseconds.
     */
    private int mTimeout;
    /**
     * When {@link #onEvent()} was last called.
     */
    private long mLastEventTime;
    private MyTimerTask mRunningTimerTask;

    /**
     * Constructor with default timeout
     */
    public Throttle(String name, Runnable callback, Handler handler) {
        this(name, callback, handler, DEFAULT_MIN_TIMEOUT, DEFAULT_MAX_TIMEOUT);
    }

    /**
     * Constructor that takes custom timeout
     */
    public Throttle(String name, Runnable callback, Handler handler, int minTimeout,
                    int maxTimeout) {
        this(name, callback, handler, minTimeout, maxTimeout, Clock.INSTANCE, TIMER);
    }

    /**
     * Constructor for tests
     */
    /* package */ Throttle(String name, Runnable callback, Handler handler, int minTimeout,
                           int maxTimeout, Clock clock, Timer timer) {
        if (maxTimeout < minTimeout) {
            throw new IllegalArgumentException();
        }
        mName = name;
        mCallback = callback;
        mClock = clock;
        mTimer = timer;
        mHandler = handler;
        mMinTimeout = minTimeout;
        mMaxTimeout = maxTimeout;
        mTimeout = mMinTimeout;
    }

    private void debugLog(String message) {

    }

    private boolean isCallbackScheduled() {
        return mRunningTimerTask != null;
    }

    public void cancelScheduledCallback() {
        if (mRunningTimerTask != null) {
            if (DEBUG) debugLog("Canceling scheduled callback");
            mRunningTimerTask.cancel();
            mRunningTimerTask = null;
        }
    }

    /* package */ void updateTimeout() {
        final long now = mClock.getTime();
        if ((now - mLastEventTime) <= TIMEOUT_EXTEND_INTERVAL) {
            mTimeout *= 2;
            if (mTimeout >= mMaxTimeout) {
                mTimeout = mMaxTimeout;
            }
            if (DEBUG) debugLog("Timeout extended " + mTimeout);
        } else {
            mTimeout = mMinTimeout;
            if (DEBUG) debugLog("Timeout reset to " + mTimeout);
        }
        mLastEventTime = now;
    }

    public void onEvent() {
        if (DEBUG) debugLog("onEvent");
        updateTimeout();
        if (isCallbackScheduled()) {
            if (DEBUG) debugLog("    callback already scheduled");
        } else {
            if (DEBUG) debugLog("    scheduling callback");
            mRunningTimerTask = new MyTimerTask();
            mTimer.schedule(mRunningTimerTask, mTimeout);
        }
    }

    /* package */ int getTimeoutForTest() {
        return mTimeout;
    }

    /* package */ long getLastEventTimeForTest() {
        return mLastEventTime;
    }

    /**
     * Timer task called on timeout,
     */
    private class MyTimerTask extends TimerTask {
        private boolean mCanceled;

        @Override
        public void run() {
            mHandler.post(new HandlerRunnable());
        }

        @Override
        public boolean cancel() {
            mCanceled = true;
            return super.cancel();
        }

        private class HandlerRunnable implements Runnable {
            @Override
            public void run() {
                mRunningTimerTask = null;
                if (!mCanceled) { // This check has to be done on the UI thread.
                    if (DEBUG) debugLog("Kicking callback");
                    mCallback.run();
                }
            }
        }
    }
}
