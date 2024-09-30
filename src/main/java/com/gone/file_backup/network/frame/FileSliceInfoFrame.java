package com.gone.file_backup.network.frame;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileSliceInfoFrame extends OperationBaseFrame {

    private String fid;

    public FileSliceInfoFrame() {

    }

    public static FileSliceInfoFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileSliceInfoFrame sliceInfoFrame = new FileSliceInfoFrame()
                .setFid(fid.toString());
        sliceInfoFrame.setOptCode(optCode);
        sliceInfoFrame.setContextId(contextId.toString());
        sliceInfoFrame.setOptId(optId.toString());
        sliceInfoFrame.setCrc(crc);

        return sliceInfoFrame;
    }

}
