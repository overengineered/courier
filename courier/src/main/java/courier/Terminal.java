package courier;

import android.support.annotation.WorkerThread;

public interface Terminal<InquiryT, ReplyT> {
    @WorkerThread
    ReplyT exchange(Exchange exchange, InquiryT inquiry);
}
