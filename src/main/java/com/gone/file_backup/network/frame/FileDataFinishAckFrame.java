package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileDataFinishAckFrame extends OperationBaseFrame {

    private String fid;
    private String syncId;
    private boolean retry;

    private FileDataFinishAckFrame() {
        super.optCode = OptCodeEnums.FILE_DATA_PART_TRANSPORT;
    }

    public static FileDataFinishAckFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence syncId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        boolean retry = buf.readBoolean();
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataFinishAckFrame metaInfoOpt = new FileDataFinishAckFrame()
                .setSyncId(syncId.toString())
                .setFid(fid.toString())
                .setRetry(retry);

        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
