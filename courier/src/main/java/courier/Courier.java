package courier;

import android.app.Activity;
import android.support.annotation.MainThread;

import java.util.concurrent.Executor;

@MainThread
public final class Courier<InquiryT, ReplyT> {

    public static <InquiryT, ReplyT, InvokerT extends Activity & Hub & Receiver<ReplyT>>
    Courier<InquiryT, ReplyT> prepare(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal) {
        Verify.isNotNull(invoker, "invoker");
        Verify.isNotNull(terminal, "terminal");
        Courier<InquiryT, ReplyT> courier = new Courier<>(invoker, terminal, invoker, 0, null, 0);
        invoker.getAdministrator().enroll(courier);
        return courier;
    }

    public static <InquiryT, ReplyT, InvokerT extends Activity & Hub>
    Courier<InquiryT, ReplyT> prepare(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal,
            Receiver<ReplyT> receiver) {
        Verify.isNotNull(invoker, "invoker");
        Verify.isNotNull(terminal, "terminal");
        Verify.isNotNull(receiver, "receiver");
        Courier<InquiryT, ReplyT> courier = new Courier<>(invoker, terminal, receiver, 0, null, 0);
        invoker.getAdministrator().enroll(courier);
        return courier;
    }

    static <InquiryT, ReplyT, InvokerT extends Activity & Hub>
    Courier<InquiryT, ReplyT> assign(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal,
            int administratorId,
            String crewTag) {
        Verify.isNotNull(invoker, "invoker");
        Verify.isNotNull(terminal, "terminal");
        Verify.isNotNull(crewTag, "crewTag");
        return new Courier<>(invoker, terminal, null, administratorId, crewTag, 0);
    }

    static <InquiryT, ReplyT, InvokerT extends Activity & Hub>
    Courier<InquiryT, ReplyT> assign(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal,
            int administratorId,
            long crewNumber) {
        Verify.isNotNull(invoker, "invoker");
        Verify.isNotNull(terminal, "terminal");
        return new Courier<>(invoker, terminal, null, administratorId, null, crewNumber);
    }


    private final Activity mActivity;
    private final Terminal<InquiryT, ReplyT> mTerminal;
    private final Receiver<ReplyT> mReceiver;
    private final String mCrewTag;
    private final long mCrewNumber;
    private int mAdministratorId;
    private Transporter mTransporter;

    private Courier(
            Activity activity,
            Terminal<InquiryT, ReplyT> terminal,
            Receiver<ReplyT> receiver,
            int administratorId,
            String crewTag,
            long crewNumber) {
        mActivity = activity;
        mTerminal = terminal;
        mReceiver = receiver;
        mAdministratorId = administratorId;
        mCrewTag = crewTag;
        mCrewNumber = crewNumber;
    }

    public final boolean isRunning() {
        return mTransporter != null;
    }

    public final void dispatch(InquiryT inquiry) {
        dispatch(inquiry, getInvoker().getAdministrator().getExecutor());
    }

    public final void dispatch(InquiryT inquiry, Executor executor) {
        if (mTransporter != null) {
            throw new IllegalStateException("Cannot dispatch while courier is running");
        }

        mTransporter = new Transporter<>(getInvoker(), mTerminal, inquiry,
                mAdministratorId, mCrewTag, mCrewNumber);
        mTransporter.start(executor);
    }

    final void setAdministratorId(int administratorId) {
        mAdministratorId = administratorId;
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
