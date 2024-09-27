package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileReconstructFrame extends OperationBaseFrame {

    private String fid;
    
    public FileReconstructFrame() {
        super.optCode = OptCodeEnums.FILE_RECONSTRUCT_LIST_TRANSPORT;
    }

    public static FileReconstructFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FileReconstructFrame reconstructFrame = new FileReconstructFrame();
        reconstructFrame.setContextId(sid.toString());
        reconstructFrame.setOptId(oid.toString());
        reconstructFrame.setFid(fid.toString());
        reconstructFrame.setCrc(crc);
        return reconstructFrame;
    }

}
