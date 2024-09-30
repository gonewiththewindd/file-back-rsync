package com.gone.file_backup.network.frame;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
@ToString(callSuper = true)
public class FileDataFinishAckFrame extends OperationBaseFrame {

    private String fid;
    private String syncId;
    private boolean retry;

    public FileDataFinishAckFrame() {

    }

    public static FileDataFinishAckFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
//        CharSequence syncId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        boolean retry = buf.readBoolean();
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataFinishAckFrame finishAckFrame = new FileDataFinishAckFrame()
//                .setSyncId(syncId.toString())
                .setFid(fid.toString())
                .setRetry(retry);

        finishAckFrame.setOptCode(optCode);
        finishAckFrame.setContextId(contextId.toString());
        finishAckFrame.setOptId(optId.toString());
        finishAckFrame.setCrc(crc);

        return finishAckFrame;
    }

}
