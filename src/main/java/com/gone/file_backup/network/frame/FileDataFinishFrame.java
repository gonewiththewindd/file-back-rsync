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
public class FileDataFinishFrame extends OperationBaseFrame {

    private String fid;

    public FileDataFinishFrame() {

    }

    public static FileDataFinishFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileDataFinishFrame metaInfoOpt = new FileDataFinishFrame()
                .setFid(fid.toString());

        metaInfoOpt.setOptCode(optCode);
        metaInfoOpt.setContextId(contextId.toString());
        metaInfoOpt.setOptId(optId.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
