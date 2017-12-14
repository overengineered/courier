# Courier
Android lifecycle aware background tasks.

Using Courier simplifies getting result of an asynchronous operation even if activity gets recreated during
execution of that operation (e.g. because of orientation change).


## How to use it

Activity should implement Hub, add a field to hold Dispatcher. Then use Dispatcher to create couriers.

    public class LoginActivity extends AppCompatActivity implements Hub, Receiver<LoginResult> {
        private final Dispatcher dispatcher = new Dispatcher(this);
        private final Courier<LoginParams, LoginResult> login =
                dispatcher.assignCourier(NetworkOperations.LOGIN, this);

        public void onClick() {
            login.dispatch(getLoginParams());
        }

        @Override
        public void onCourierDelivery(LoginResult loginResult) {
            // handle result
        }

        public Dispatcher getDispatcher() {
            return dispatcher;
        }
    }
 
 Couriers are assigned to a Medium, which is the performs the asynchronous operation. It's best to define them outside
 activity class or to make them static to avoid leaking activity reference.
 
     public class NetworkOperations {
        public static final Medium<LoginParams, LoginResult> LOGIN =
                new Medium<LoginParams, LoginResult>() {
            @WorkerThread
            @Override
            public LoginResult resolve(Exchange exchange, LoginParams loginParams) {
                // perform network request
            }
        };
    }

After this setup you don't have to worry about orientation changes - Courier will find the new activity and deliver
the result there. Courier will also only deliver result if activity is in started state. This allows using
FragmentManager transactions to show/hide fragments in onCourierDelivery method.

Additionally, Medium gets passed Exchange parameter which provides access to application context and can be used
to watch for cancellation. Watching for cancellation allows to terminate early if user leaves the activity before
the operation is finished.
