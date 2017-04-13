package courier;

import android.app.Activity;
import android.support.annotation.MainThread;
import android.support.v4.util.LongSparseArray;

import java.util.HashMap;
import java.util.Map;

@MainThread
public abstract class Crew<InquiryT, ReplyT> {

    private final Activity mActivity;
    private final Terminal<InquiryT, ReplyT> mTerminal;
    private final int mDispatcherId;

    private Crew(Activity activity, Terminal<InquiryT, ReplyT> terminal, int dispatcherId) {
        mActivity = activity;
        mTerminal = terminal;
        mDispatcherId = dispatcherId;
    }

    abstract void relay(Transporter transporter);

    abstract void deliver(Transporter transporter, ReplyT reply);

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

        TaggedCouriers(Activity activity, Terminal<InquiryT, ReplyT> terminal, int dispatcherId,
                       Receiver<ReplyT> receiver) {
            super(activity, terminal, dispatcherId);
            mReceiver = receiver;
        }

        public Courier<InquiryT, ReplyT> getCourier(String tag) {
            Courier<InquiryT, ReplyT> courier = mCouriers.get(tag);
            if (courier != null) {
                return courier;
            }

            courier = new Courier<>(super.getInvoker(), super.mTerminal, null, super.mDispatcherId, tag, 0);
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

        NumberedCouriers(Activity activity, Terminal<InquiryT, ReplyT> terminal, int dispatcherId,
                         Receiver<ReplyT> receiver) {
            super(activity, terminal, dispatcherId);
            mReceiver = receiver;
        }

        public Courier<InquiryT, ReplyT> getCourier(long number) {
            Courier<InquiryT, ReplyT> courier = mCouriers.get(number);
            if (courier != null) {
                return courier;
            }

            courier = new Courier<>(super.getInvoker(), super.mTerminal, null, super.mDispatcherId, null, number);
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
