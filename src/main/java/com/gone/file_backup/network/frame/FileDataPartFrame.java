package com.gone.file_backup.network.frame;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
@ToString(exclude = {"content"})
public class FileDataPartFrame extends OperationBaseFrame {

    private String fid;
    private long length;
    private int seq;
    private int blockSize;
    private byte[] content;

    public FileDataPartFrame() {
    }

    public static FileDataPartFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int seq = buf.readInt();
        int blockSize = buf.readInt();
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        long crc = buf.readLong();
//        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataPartFrame dataPartFrame = new FileDataPartFrame()
                .setFid(fid.toString())
                .setLength(length)
                .setSeq(seq)
                .setBlockSize(blockSize)
                .setContent(bytes);
        dataPartFrame.setOptCode(optCode);
        dataPartFrame.setContextId(contextId.toString());
        dataPartFrame.setOptId(optId.toString());
        dataPartFrame.setCrc(crc);

        return dataPartFrame;
    }

}
