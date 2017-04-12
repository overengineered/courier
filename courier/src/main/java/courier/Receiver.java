package courier;

public interface Receiver<ReplyT> {
    void onCourierDelivery(ReplyT reply);
}
