package com.gone.file_backup.model.retry;

import com.gone.file_backup.network.frame.OperationBaseFrame;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FrameRetryMsg extends RetryMsg {

    private OperationBaseFrame frame;

    @Override
    protected void reset(ByteBuf buf) {

    }

    @Override
    protected void reset(OperationBaseFrame frame) {
        this.frame = frame;
    }
}
