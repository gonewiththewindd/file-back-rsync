package com.gone.file_backup.network.frame;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class FullSyncFinishFrame extends OperationBaseFrame {

    private String syncId;
    private String remoteDirectory;

    public FullSyncFinishFrame() {

    }

    public static FullSyncFinishFrame of(String remoteDirectory) {
        FullSyncFinishFrame metaInfoOpt = new FullSyncFinishFrame()
                .setRemoteDirectory(remoteDirectory);
        metaInfoOpt.setOptId(UUID.randomUUID().toString());
        metaInfoOpt.setContextId(UUID.randomUUID().toString());
        return metaInfoOpt;
    }

    public static FullSyncFinishFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence syncId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
//        int directoryLength = buf.readInt();
//        CharSequence directory = buf.readCharSequence(directoryLength, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        FullSyncFinishFrame fullSyncFinishFrame = new FullSyncFinishFrame()
                .setSyncId(syncId.toString())
                /*.setRemoteDirectory(directory.toString())*/;
        fullSyncFinishFrame.setOptCode(optCode);
        fullSyncFinishFrame.setOptId(contextId.toString());
        fullSyncFinishFrame.setContextId(optId.toString());
        fullSyncFinishFrame.setCrc(crc);

        return fullSyncFinishFrame;
    }

}
