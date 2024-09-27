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
public class MetaInfoAckOptBO extends OptBO {

    private MetaInfoAckOptBO() {
        super.optCode = OptCodeEnums.SEND_FILE_META_INFO_ACK;
    }

    public static MetaInfoAckOptBO parse(ByteBuf buf) {

        int index = 0;
        CharSequence head = buf.getCharSequence(index, NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        index += head.length();
        int optCode = buf.getInt(index);
        index += 4;
        CharSequence oid = buf.getCharSequence(index, 32, StandardCharsets.UTF_8);
        index += 32;
        CharSequence sid = buf.getCharSequence(index, 32, StandardCharsets.UTF_8);
        index += 32;
        long crc = buf.getLong(index);
        index += 8;
//        CharSequence tail = buf.getCharSequence(index, NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        MetaInfoAckOptBO metaInfoOpt = new MetaInfoAckOptBO();
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
