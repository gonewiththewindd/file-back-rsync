package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
@ToString(callSuper = true)
public class MetaInfoAckFrame extends OperationBaseFrame {

    private MetaInfoAckFrame() {
        super.optCode = OptCodeEnums.FILE_META_INFO_ACK;
    }

    public static MetaInfoAckFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        MetaInfoAckFrame metaInfoOpt = new MetaInfoAckFrame();
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
