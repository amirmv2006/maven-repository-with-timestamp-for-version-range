package ir.amv.os.tools.maveninterceptor.interceptor.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author Amir
 */
public class WaitASecondMap<K, V> extends HashMap<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitASecondMap.class);

    private final transient Object lock = new Object();

    public V iWantThisKeyAndIllWait(K key) throws InterruptedException {
        V v = super.get(key);
        while (v == null) {
            synchronized (lock) {
                LOGGER.info("Waiting for key '{}'", key);
                lock.wait();
            }
            v = super.get(key);
        }
        LOGGER.info("Got the value for key '{}'", key);
        return v;
    }

    @Override
    public V put(final K key, final V value) {
        V put = super.put(key, value);
        LOGGER.info("Put the value for key '{}'", key);
        synchronized (lock) {
            lock.notifyAll();
        }
        return put;
    }
}
