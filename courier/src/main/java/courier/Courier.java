package courier;

import android.app.Activity;
import android.support.annotation.MainThread;

import java.util.concurrent.Executor;

@MainThread
public final class Courier<InquiryT, ReplyT> {

    private final Activity mActivity;
    private final Medium<InquiryT, ReplyT> mMedium;
    private final Receiver<ReplyT> mReceiver;
    private final String mCrewTag;
    private final long mCrewNumber;
    private final int mDispatcherId;
    private Transporter mTransporter;

    Courier(Activity activity, Medium<InquiryT, ReplyT> medium, Receiver<ReplyT> receiver,
            int dispatcherId, String crewTag, long crewNumber) {
        mActivity = activity;
        mMedium = medium;
        mReceiver = receiver;
        mDispatcherId = dispatcherId;
        mCrewTag = crewTag;
        mCrewNumber = crewNumber;
    }

    public final boolean isRunning() {
        return mTransporter != null;
    }

    public final void dispatch(InquiryT inquiry) {
        dispatch(inquiry, getInvoker().getDispatcher().getExecutor());
    }

    public final void dispatch(InquiryT inquiry, Executor executor) {
        if (mTransporter != null) {
            throw new IllegalStateException("Cannot dispatch while courier is running");
        }

        mTransporter = new Transporter<>(getInvoker(), mMedium, inquiry,
                mDispatcherId, mCrewTag, mCrewNumber);
        mTransporter.start(executor);
    }

    final void relay(Transporter transporter) {
        if (mTransporter != null && mTransporter != transporter) {
            throw new IllegalStateException("New dispatch has been issued before finishing previous");
        }

        mTransporter = transporter;
    }

    final void deliver(Transporter transporter, ReplyT reply) {
        if (mTransporter != transporter) {
            throw new IllegalStateException("Cannot deliver result, courier has been issued a new dispatch");
        }

        mReceiver.onCourierDelivery(reply);
        mTransporter = null;
    }

    final void quit() {
        mTransporter = null;
    }

    private <InvokerT extends Activity & Hub> InvokerT getInvoker() {
        Verify.isHub(mActivity);
        //noinspection unchecked: checked by Verify.isHub
        return (InvokerT) mActivity;
    }
}
