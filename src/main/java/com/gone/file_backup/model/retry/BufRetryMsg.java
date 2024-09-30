package com.gone.file_backup.model.retry;

import com.gone.file_backup.network.frame.OperationBaseFrame;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BufRetryMsg extends RetryMsg{

    private ByteBuf buf;

    public void reset(ByteBuf buf, long systemTime) {
        this.buf = buf;
        this.lastSendAt = systemTime;
        retryCount.set(0);
    }

    @Override
    protected void reset(ByteBuf buf) {
        this.buf = buf;
    }

    @Override
    protected void reset(OperationBaseFrame frame) {

    }
}
