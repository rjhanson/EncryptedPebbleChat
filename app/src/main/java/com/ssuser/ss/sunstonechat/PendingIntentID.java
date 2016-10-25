package com.ssuser.ss.sunstonechat;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Eric "I love Curry" Yu on 5/12/2015.
 */
public class PendingIntentID {
    private final static AtomicInteger id = new AtomicInteger(0);
    public static int getID() {
        return id.incrementAndGet();
    }
}
