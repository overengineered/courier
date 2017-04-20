package courier;

import android.content.Context;
import android.content.ContextWrapper;

public abstract class Exchange extends ContextWrapper {
    public Exchange(Context base) {
        super(base);
    }

    public abstract boolean isCancelled();

    public abstract void setCancellationListener(CancellationListener listener);
}
