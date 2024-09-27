package com.gone.file_backup.rsync.network.opt;

import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
@ToString(callSuper = true)
public class FileDataAckOptBO extends OptBO {

    private String fid;
    private int seq;

    private FileDataAckOptBO() {
        super.optCode = OptCodeEnums.SEND_FILE_DATA_ACK;
    }

    public static FileDataAckOptBO parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int seq = buf.readInt();
        long crc = buf.readLong();
//        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataAckOptBO metaInfoOpt = new FileDataAckOptBO()
                .setFid(fid.toString())
                .setSeq(seq);
        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
