package courier;

import android.support.annotation.WorkerThread;

public interface Medium<InquiryT, ReplyT> {
    @WorkerThread
    ReplyT resolve(Exchange exchange, InquiryT inquiry);
}
