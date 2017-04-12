package courier;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.util.LongSparseArray;

import java.util.HashMap;
import java.util.Map;

@MainThread
public abstract class Crew<InquiryT, ReplyT> {

    public static <InquiryT, ReplyT, InvokerT extends Activity & Hub>
    TaggedCouriers<InquiryT, ReplyT> tagged(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal,
            TaggedCouriers.Receiver<ReplyT> receiver) {
        TaggedCouriers<InquiryT, ReplyT> crew = new TaggedCouriers<>(invoker, terminal, receiver);
        invoker.getAdministrator().enroll(crew);
        return crew;
    }

    public static <InquiryT, ReplyT, InvokerT extends Activity & Hub>
    NumberedCouriers<InquiryT, ReplyT> numbered(
            InvokerT invoker,
            Terminal<InquiryT, ReplyT> terminal,
            NumberedCouriers.Receiver<ReplyT> receiver) {
        NumberedCouriers<InquiryT, ReplyT> crew = new NumberedCouriers<>(invoker, terminal, receiver);
        invoker.getAdministrator().enroll(crew);
        return crew;
    }


    private final Activity mActivity;
    private final Terminal<InquiryT, ReplyT> mTerminal;
    private int mAdministratorId;

    private Crew(Activity activity, Terminal<InquiryT, ReplyT> terminal) {
        mActivity = activity;
        mTerminal = terminal;
    }

    abstract void relay(Transporter transporter);

    abstract void deliver(Transporter transporter, ReplyT reply);

    final void setAdministratorId(int index) {
        mAdministratorId = index;
    }

    @SuppressWarnings("unchecked")
    private <InvokerT extends Activity & Hub> InvokerT getInvoker() {
        return (InvokerT) mActivity;
    }

    @MainThread
    public static final class TaggedCouriers<InquiryT, ReplyT> extends Crew<InquiryT, ReplyT> {
        public interface Receiver<ReplyT> {
            void onCourierDelivery(String tag, ReplyT reply);
        }

        private final Map<String, Courier<InquiryT, ReplyT>> mCouriers = new HashMap<>();
        private final Receiver<ReplyT> mReceiver;

        private TaggedCouriers(
                Activity activity,
                Terminal<InquiryT, ReplyT> terminal,
                Receiver<ReplyT> receiver) {
            super(activity, terminal);
            mReceiver = receiver;
        }

        public Courier<InquiryT, ReplyT> getCourier(String tag) {
            Courier<InquiryT, ReplyT> courier = mCouriers.get(tag);
            if (courier != null) {
                return courier;
            }

            courier = Courier.assign(super.getInvoker(), super.mTerminal, super.mAdministratorId, tag);
            mCouriers.put(tag, courier);
            return courier;
        }

        @Override
        void relay(Transporter transporter) {
            getCourier(transporter.getCrewTag()).relay(transporter);
        }

        @Override
        void deliver(Transporter transporter, ReplyT reply) {
            mReceiver.onCourierDelivery(transporter.getCrewTag(), reply);
            getCourier(transporter.getCrewTag()).quit();
        }
    }

    @MainThread
    public static final class NumberedCouriers<InquiryT, ReplyT> extends Crew<InquiryT, ReplyT> {
        public interface Receiver<ReplyT> {
            void onCourierDelivery(long number, ReplyT reply);
        }

        private final LongSparseArray<Courier<InquiryT, ReplyT>> mCouriers = new LongSparseArray<>();
        private final Receiver<ReplyT> mReceiver;

        private NumberedCouriers(
                Activity activity,
                Terminal<InquiryT, ReplyT> terminal,
                Receiver<ReplyT> receiver) {
            super(activity, terminal);
            mReceiver = receiver;
        }

        public Courier<InquiryT, ReplyT> getCourier(long number) {
            Courier<InquiryT, ReplyT> courier = mCouriers.get(number);
            if (courier != null) {
                return courier;
            }

            courier = Courier.assign(super.getInvoker(), super.mTerminal, super.mAdministratorId, number);
            mCouriers.put(number, courier);
            return courier;
        }

        @Override
        void relay(Transporter transporter) {
            getCourier(transporter.getCrewNumber()).relay(transporter);
        }

        @Override
        void deliver(Transporter transporter, ReplyT reply) {
            mReceiver.onCourierDelivery(transporter.getCrewNumber(), reply);
            getCourier(transporter.getCrewNumber()).quit();
        }
    }
}
