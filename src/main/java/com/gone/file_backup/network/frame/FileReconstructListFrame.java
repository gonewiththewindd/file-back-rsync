package com.gone.file_backup.network.frame;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.file.reconstruct.FileBlockTypeEnums;
import com.gone.file_backup.file.reconstruct.MatchedFileBlock;
import com.gone.file_backup.file.reconstruct.ModifyByteSerial;
import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.utils.BeanUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
@Accessors(chain = true)
public class FileReconstructListFrame extends OperationBaseFrame {

    private String fid;
    private String md5;
    private List<? extends ReconstructFileBlock> blocks;


    public FileReconstructListFrame() {
        super.optCode = OptCodeEnums.FILE_RECONSTRUCT_LIST_TRANSPORT;
    }

    public static FileReconstructListFrame parse(ByteBuf buf) {

        FileReconstructListFrame reconstructListFrame = new FileReconstructListFrame();

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int blockType = buf.readInt();
        if (blockType == FileBlockTypeEnums.MATCHED.getValue()) {
            int length = buf.readInt();
            CharSequence json = buf.readCharSequence(length, StandardCharsets.UTF_8);
            CompressFileReconstructBlock compressFileReconstructBlock = JSON.parseObject(json.toString(), CompressFileReconstructBlock.class);
            reconstructListFrame.setFid(compressFileReconstructBlock.getFid());
            reconstructListFrame.setMd5(compressFileReconstructBlock.getMd5());
            List<MatchedFileBlock> blocks = BeanUtils.copyList(compressFileReconstructBlock.getBlocks(), MatchedFileBlock.class);
            reconstructListFrame.setBlocks(blocks);
        } else if (blockType == FileBlockTypeEnums.SERIAL.getValue()) {
            CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
            CharSequence md5 = buf.readCharSequence(32, StandardCharsets.UTF_8);
            int from = buf.readInt();
            int to = buf.readInt();
            int length = buf.readInt();
            ByteBuf byteBuf = buf.readBytes(length);
            ReconstructFileBlock reconstructFileBlock = new ModifyByteSerial()
                    .setContent(byteBuf.array())
                    .setFrom(from)
                    .setTo(to);
            reconstructListFrame.setFid(fid.toString());
            reconstructListFrame.setMd5(md5.toString());
            reconstructListFrame.setBlocks(List.of(reconstructFileBlock));
        }
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        reconstructListFrame.setContextId(sid.toString());
        reconstructListFrame.setOptId(oid.toString());
        reconstructListFrame.setCrc(crc);

        return reconstructListFrame;
    }

}
