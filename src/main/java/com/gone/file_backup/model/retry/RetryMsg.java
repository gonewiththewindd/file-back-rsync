package com.gone.file_backup.model.retry;

import com.gone.file_backup.network.frame.OperationBaseFrame;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@Accessors(chain = true)
public abstract class RetryMsg {

    protected String ip;
    protected int port;

    protected long lastSendAt;
    protected AtomicInteger retryCount = new AtomicInteger(0);

    public void reset(ByteBuf buf, long systemTime) {
        this.lastSendAt = systemTime;
        retryCount.set(0);
        reset(buf);
    }

    public void reset(OperationBaseFrame frame, long systemTime) {
        this.lastSendAt = systemTime;
        retryCount.set(0);
        reset(frame);
    }

    protected abstract void reset(ByteBuf buf);
    protected abstract void reset(OperationBaseFrame frame);
}
