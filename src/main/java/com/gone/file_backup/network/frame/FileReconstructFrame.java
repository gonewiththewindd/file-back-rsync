package com.gone.file_backup.network.frame;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileReconstructFrame extends OperationBaseFrame {

    private String fid;
    
    public FileReconstructFrame() {

    }

    public static FileReconstructFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileReconstructFrame reconstructFrame = new FileReconstructFrame();
        reconstructFrame.setOptCode(optCode);
        reconstructFrame.setContextId(contextId.toString());
        reconstructFrame.setOptId(optId.toString());
        reconstructFrame.setFid(fid.toString());
        reconstructFrame.setCrc(crc);
        return reconstructFrame;
    }

}
