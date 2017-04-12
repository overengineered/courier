package courier;

import android.os.AsyncTask;
import android.support.annotation.MainThread;

import java.util.ArrayList;
import java.util.concurrent.Executor;

@MainThread
public final class Administrator {

    private final ArrayList<Courier> mCouriers = new ArrayList<>();
    private final ArrayList<Crew> mCrews = new ArrayList<>();
    private final Executor mExecutor;

    public Administrator() {
        mExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    public Administrator(Executor executor) {
        Verify.isNotNull(executor, "executor");
        mExecutor = executor;
    }

    final Executor getExecutor() {
        return mExecutor;
    }

    final void enroll(Courier courier) {
        courier.setAdministratorId(-mCouriers.size() - 1);
        mCouriers.add(courier);
    }

    final void enroll(Crew crew) {
        crew.setAdministratorId(mCrews.size());
        mCrews.add(crew);
    }

    final void relay(int administratorId, Transporter transporter) {
        if (administratorId < 0) {
            mCouriers.get(-administratorId - 1).relay(transporter);
        }
        else {
            mCrews.get(administratorId).relay(transporter);
        }
    }

    @SuppressWarnings("unchecked")
    final void deliver(int administratorId, Transporter transporter, Object reply) {
        if (administratorId < 0) {
            mCouriers.get(-administratorId - 1).deliver(transporter, reply);
        }
        else {
            mCrews.get(administratorId).deliver(transporter, reply);
        }
    }
}
