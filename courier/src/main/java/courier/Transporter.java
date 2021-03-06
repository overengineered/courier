package courier;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.util.concurrent.Executor;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@SuppressLint("StaticFieldLeak") // this class listens to activity lifecycle to prevent leaks
final class Transporter<InquiryT, ReplyT, InvokerT extends Activity & Hub>
        extends AsyncTask<Object, Object, Object>
        implements ActivityLifecycleCallbacks {

    @Retention(SOURCE)
    @IntDef({Phase.IDLE, Phase.RUNNING, Phase.REPLY_READY, Phase.REPLY_DISPATCHED, Phase.CANCELLED})
    private @interface Phase {
        int IDLE = 0;
        int RUNNING = 1;
        int REPLY_READY = 2;
        int REPLY_DISPATCHED = 3;
        int CANCELLED = 4;
    }

    private static int sNextId = 1;

    private static int provideId() {
        return sNextId++;
    }

    private final int mId;
    private final InquiryT mInquiry;
    private final Medium<InquiryT, ReplyT> mMedium;

    private final int mDispatcherId;
    private final String mCrewTag;
    private final long mCrewNumber;

    private @Phase int mPhase;
    private InvokerT mActivity;
    private Exchange mExchange;
    private boolean mActivityAvailable;
    private String mActivityStateKey;
    private CancellationListener mCancellationListener;
    private ReplyT mReply;

    Transporter(InvokerT activity,
                Medium<InquiryT, ReplyT> medium,
                InquiryT inquiry,
                int dispatcherId,
                String crewTag,
                long crewNumber) {
        mId = provideId();
        mMedium = medium;
        mInquiry = inquiry;
        mDispatcherId = dispatcherId;
        mCrewTag = crewTag;
        mCrewNumber = crewNumber;
        mActivity = activity;
        mActivityAvailable = true;
        mPhase = Phase.IDLE;

        mExchange = new Exchange(activity.getApplicationContext()) {
            @Override
            public boolean isCancelled() {
                return Transporter.this.isCancelled();
            }

            @Override
            public void setCancellationListener(CancellationListener listener) {
                mCancellationListener = listener;
                if (isCancelled()) {
                    listener.onCancel();
                }
            }
        };
    }

    final String getCrewTag() {
        return mCrewTag;
    }

    final long getCrewNumber() {
        return mCrewNumber;
    }

    final void start(Executor executor) {
        if (mPhase != Phase.IDLE) {
            throw new IllegalStateException("Cannot execute task again");
        }

        mPhase = Phase.RUNNING;
        mActivity.getApplication().registerActivityLifecycleCallbacks(this);
        executeOnExecutor(executor, (Object[]) null);
    }

    private void finish() {
        mActivity.getDispatcher().deliver(mDispatcherId, this, mReply);
        mPhase = Phase.REPLY_DISPATCHED;
        mActivity.getApplication().unregisterActivityLifecycleCallbacks(this);
        mActivityAvailable = false;
        mActivity = null;
    }

    private InvokerT asInvoker(Activity activity) {
        Verify.isHub(activity);
        //noinspection unchecked: checked by Verify.isHub
        return (InvokerT) activity;
    }

    private String getActivityStateKey() {
        if (mActivityStateKey == null) {
            mActivityStateKey = "courier.transporter#" + mId;
        }
        return mActivityStateKey;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        mReply = mMedium.resolve(mExchange, mInquiry);
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        if (mPhase == Phase.CANCELLED) {
            return;
        }

        mPhase = Phase.REPLY_READY;
        if (mActivityAvailable) {
            finish();
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mPhase = Phase.CANCELLED;
        mActivity = null;
        mActivityAvailable = false;
        if (mCancellationListener != null)
            mCancellationListener.onCancel();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.containsKey(getActivityStateKey())) {
            mActivity = asInvoker(activity);
            mActivity.getDispatcher().relay(mDispatcherId, this);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivity == activity) {
            if (mPhase == Phase.REPLY_READY) {
                finish();
            }
            else {
                mActivityAvailable = true;
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (mActivity == activity) {
            outState.putString(getActivityStateKey(), "");
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (mActivity == activity) {
            mActivityAvailable = false;
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mActivity == activity) {
            mActivity = null;
            if (activity.isFinishing()) {
                mPhase = Phase.CANCELLED;
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                cancel(true);
            }
        }
    }
}
