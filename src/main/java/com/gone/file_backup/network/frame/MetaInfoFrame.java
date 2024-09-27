package com.gone.file_backup.network.frame;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class MetaInfoFrame extends OperationBaseFrame {

//    private String syncId;
    private String fid;
    private String name;
    private long length;
    private String destPath;
    private String md5;

    private MetaInfoFrame() {
        super.optCode = OptCodeEnums.FILE_META_INFO;
    }

    public static MetaInfoFrame of(FileMetaInfo fileMetaInfo) {
        MetaInfoFrame metaInfoOpt = new MetaInfoFrame()
                .setFid(fileMetaInfo.getFid())
                .setName(fileMetaInfo.getName())
                .setLength(fileMetaInfo.getLength());
        metaInfoOpt.setOptId(UUID.randomUUID().toString());
        metaInfoOpt.setContextId(UUID.randomUUID().toString());
        return metaInfoOpt;
    }

    public static MetaInfoFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
//        CharSequence syncId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int nameLength = buf.readInt();
        CharSequence name = buf.readCharSequence(nameLength, StandardCharsets.UTF_8);
        int pathLength = buf.readInt();
        CharSequence destPath = buf.readCharSequence(pathLength, StandardCharsets.UTF_8);
        CharSequence md5 = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long fileLength = buf.readLong();
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        MetaInfoFrame metaInfoOpt = new MetaInfoFrame()
//                .setSyncId(syncId.toString())
                .setFid(fid.toString())
                .setName(name.toString())
                .setLength(fileLength)
                .setDestPath(destPath.toString())
                .setMd5(md5.toString());
        metaInfoOpt.setOptId(contextId.toString());
        metaInfoOpt.setContextId(optId.toString());
        metaInfoOpt.setCrc(crc);

        return metaInfoOpt;
    }

}
