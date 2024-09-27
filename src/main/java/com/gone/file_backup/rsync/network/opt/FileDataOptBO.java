package com.gone.file_backup.rsync.network.opt;

import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileDataOptBO extends OptBO {

    private String fid;
    private long length;
    private int seq;
    private int blockSize;
    private ByteBuf content;

    private FileDataOptBO() {
        super.optCode = OptCodeEnums.SEND_FILE_DATA;
    }

    public static FileDataOptBO parse(ByteBuf buf) {

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

        FileDataOptBO metaInfoOpt = new FileDataOptBO()
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
