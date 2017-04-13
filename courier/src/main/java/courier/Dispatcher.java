package courier;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.util.ArrayList;
import java.util.concurrent.Executor;

@MainThread
public final class Dispatcher {

    private final Activity mActivity;
    private final Executor mExecutor;
    private final ArrayList<Courier> mCouriers = new ArrayList<>();
    private final ArrayList<Crew> mCrews = new ArrayList<>();

    public <InvokerT extends Activity & Hub> Dispatcher(InvokerT invoker) {
        mActivity = invoker;
        mExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public <InvokerT extends Activity & Hub> Dispatcher(InvokerT invoker, Executor executor) {
        Verify.isNotNull(executor, "executor");
        mActivity = invoker;
        mExecutor = executor;
    }

    public <InquiryT, ReplyT> Courier<InquiryT, ReplyT> assignCourier(
            Terminal<InquiryT, ReplyT> terminal, Receiver<ReplyT> receiver) {
        Verify.isNotNull(terminal, "terminal");
        Verify.isNotNull(receiver, "receiver");
        int dispatcherId = -mCouriers.size() - 1;
        final Courier<InquiryT, ReplyT> courier;
        courier = new Courier<>(mActivity, terminal, receiver, dispatcherId, null, 0);
        mCouriers.add(courier);
        return courier;
    }

    public <InquiryT, ReplyT> Crew.TaggedCouriers<InquiryT, ReplyT> assignTaggedCrew(
            Terminal<InquiryT, ReplyT> terminal, Crew.TaggedCouriers.Receiver<ReplyT> receiver) {
        Verify.isNotNull(terminal, "terminal");
        Verify.isNotNull(receiver, "receiver");
        final Crew.TaggedCouriers<InquiryT, ReplyT> crew;
        crew = new Crew.TaggedCouriers<>(mActivity, terminal, mCrews.size(), receiver);
        mCrews.add(crew);
        return crew;
    }

    public <InquiryT, ReplyT> Crew.NumberedCouriers<InquiryT, ReplyT> assignNumberedCrew(
            Terminal<InquiryT, ReplyT> terminal, Crew.NumberedCouriers.Receiver<ReplyT> receiver) {
        Verify.isNotNull(terminal, "terminal");
        Verify.isNotNull(receiver, "receiver");
        final Crew.NumberedCouriers<InquiryT, ReplyT> crew;
        crew = new Crew.NumberedCouriers<>(mActivity, terminal, mCrews.size(), receiver);
        mCrews.add(crew);
        return crew;
    }

    final Executor getExecutor() {
        return mExecutor;
    }

    final void relay(int dispatcherId, Transporter transporter) {
        if (dispatcherId < 0) {
            mCouriers.get(-dispatcherId - 1).relay(transporter);
        }
        else {
            mCrews.get(dispatcherId).relay(transporter);
        }
    }

    @SuppressWarnings("unchecked")
    final void deliver(int dispatcherId, Transporter transporter, Object reply) {
        if (dispatcherId < 0) {
            mCouriers.get(-dispatcherId - 1).deliver(transporter, reply);
        }
        else {
            mCrews.get(dispatcherId).deliver(transporter, reply);
        }
    }
}
