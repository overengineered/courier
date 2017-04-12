package courier;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.util.concurrent.Executor;

import static java.lang.annotation.RetentionPolicy.SOURCE;

final class Transporter<InquiryT, ReplyT, InvokerT extends Activity & Hub>
        extends AsyncTask<Object, Object, Object>
        implements ActivityLifecycleCallbacks {

    @Retention(SOURCE)
    @IntDef({Phase.IDLE, Phase.RUNNING, Phase.REPLY_READY, Phase.REPLY_DISPATCHED})
    private @interface Phase {
        int IDLE = 0;
        int RUNNING = 1;
        int REPLY_READY = 2;
        int REPLY_DISPATCHED = 3;
    }

    private static int sNextId = 1;

    private static int provideId() {
        return sNextId++;
    }

    private final int mId;
    private final InquiryT mInquiry;
    private final Terminal<InquiryT, ReplyT> mTerminal;

    private final int mAdministratorId;
    private final String mCrewTag;
    private final long mCrewNumber;

    private @Phase int mPhase;
    private InvokerT mActivity;
    private boolean mActivityAvailable;
    private String mActivityStateKey;
    private ReplyT mReply;

    Transporter(InvokerT activity,
                Terminal<InquiryT, ReplyT> terminal,
                InquiryT inquiry,
                int administratorId,
                String crewTag,
                long crewNumber) {
        mId = provideId();
        mTerminal = terminal;
        mInquiry = inquiry;
        mAdministratorId = administratorId;
        mCrewTag = crewTag;
        mCrewNumber = crewNumber;
        mActivity = activity;
        mActivityAvailable = true;
        mPhase = Phase.IDLE;
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
        mActivity.getAdministrator().deliver(mAdministratorId, this, mReply);
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
        mReply = mTerminal.exchange(mInquiry);
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        mPhase = Phase.REPLY_READY;
        if (mActivityAvailable) {
            finish();
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.containsKey(getActivityStateKey())) {
            mActivity = asInvoker(activity);
            mActivity.getAdministrator().relay(mAdministratorId, this);
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
                activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            }
        }
    }
}
