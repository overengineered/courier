package courier;

import static android.R.attr.label;

final class Verify {
    static void isNotNull(Object object, String name) {
        if (object == null)
            throw new IllegalStateException(name + " is null");
    }

    static void isHub(Object object) {
        if (!(object instanceof Hub))
            throw new IllegalStateException(missingImplementation(object, Hub.class));
    }

    private static String missingImplementation(Object object, Class<?> requirement) {
        return object.getClass().getName() + " does not implement " + requirement.getName();
    }
}
