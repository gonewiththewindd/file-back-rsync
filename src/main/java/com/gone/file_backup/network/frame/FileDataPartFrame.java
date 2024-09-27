package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileDataPartFrame extends OperationBaseFrame {

    private String fid;
    private long length;
    private int seq;
    private int blockSize;
    private ByteBuf content;

    private FileDataPartFrame() {
        super.optCode = OptCodeEnums.FILE_DATA_PART_TRANSPORT;
    }

    public static FileDataPartFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int seq = buf.readInt();
        int blockSize = buf.readInt();
        int length = buf.readInt();
        ByteBuf buffer = buf.readBytes(length);
        long crc = buf.readLong();
//        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataPartFrame metaInfoOpt = new FileDataPartFrame()
                .setFid(fid.toString())
                .setLength(length)
                .setSeq(seq)
                .setBlockSize(blockSize)
                .setContent(buffer);
        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
