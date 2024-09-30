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
public class FileDataPartAckFrame extends OperationBaseFrame {

    private String fid;
    private int seq;

    public FileDataPartAckFrame() {

    }

    public static FileDataPartAckFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int seq = buf.readInt();
        long crc = buf.readLong();
//        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataPartAckFrame dataPartAckFrame = new FileDataPartAckFrame()
                .setFid(fid.toString())
                .setSeq(seq);
        dataPartAckFrame.setOptCode(optCode);
        dataPartAckFrame.setContextId(contextId.toString());
        dataPartAckFrame.setOptId(optId.toString());
        dataPartAckFrame.setCrc(crc);

        return dataPartAckFrame;
    }

}
