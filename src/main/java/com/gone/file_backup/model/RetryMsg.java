package com.gone.file_backup.model;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@Accessors(chain = true)
public class RetryMsg {

    private String ip;
    private int port;

    private long lastSendAt;
    private ByteBuf buf;
    private AtomicInteger retryCount = new AtomicInteger(0);

    public void reset(ByteBuf buf, long systemTime) {
        this.buf = buf;
        this.lastSendAt = systemTime;
        retryCount.set(0);
    }
}
