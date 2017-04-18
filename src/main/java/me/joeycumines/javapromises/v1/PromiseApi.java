package me.joeycumines.javapromises.v1;

/**
 * Helpers for the creation and use promises.
 * <p>
 * Note: implementation specific.
 */
public class PromiseApi {
    private static PromiseApi singletonInstance;

    private PromiseApi() {
    }

    /**
     * @return A singleton PromiseApi (thread safe).
     */
    public static PromiseApi getInstance() {
        // double checked locking
        if (null == singletonInstance) {
            synchronized (PromiseApi.class) {
                if (null == singletonInstance) {
                    singletonInstance = new PromiseApi();
                }
            }

        }

        return singletonInstance;
    }
}
