package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileSliceInfoFrame extends OperationBaseFrame {

    private String fid;

    private FileSliceInfoFrame() {
        super.optCode = OptCodeEnums.FILE_SLICE_INFO;
    }

    public static FileSliceInfoFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileSliceInfoFrame metaInfoOpt = new FileSliceInfoFrame()
                .setFid(fid.toString());
        metaInfoOpt.setContextId(sid.toString());
        metaInfoOpt.setOptId(oid.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
