package com.gone.file_backup.network.frame;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.file.reconstruct.FileBlockTypeEnums;
import com.gone.file_backup.file.reconstruct.MatchedFileBlock;
import com.gone.file_backup.file.reconstruct.ModifyByteSerial;
import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.constants.NetworkConstants;
import com.gone.file_backup.utils.BeanUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Data
@Accessors(chain = true)
public class FileReconstructBlocksFrame extends OperationBaseFrame {

    private String fid;
    private String md5;
    private List<? extends ReconstructFileBlock> blocks;


    public FileReconstructBlocksFrame() {
    }

    public static FileReconstructBlocksFrame parse(ByteBuf buf) {

        FileReconstructBlocksFrame reconstructBlocksFrame = new FileReconstructBlocksFrame();

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence optId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int blockType = buf.readInt();
        if (blockType == FileBlockTypeEnums.MATCHED.getValue()) {
            int length = buf.readInt();
            CharSequence json = buf.readCharSequence(length, StandardCharsets.UTF_8);
            CompressFileReconstructBlock compressFileReconstructBlock = JSON.parseObject(json.toString(), CompressFileReconstructBlock.class);
            reconstructBlocksFrame.setFid(compressFileReconstructBlock.getFid());
            reconstructBlocksFrame.setMd5(compressFileReconstructBlock.getMd5());
            List<MatchedFileBlock> blocks = BeanUtils.copyList(compressFileReconstructBlock.getBlocks(), MatchedFileBlock.class);
            reconstructBlocksFrame.setBlocks(blocks);
        } else if (blockType == FileBlockTypeEnums.SERIAL.getValue()) {
            CharSequence fid = buf.readCharSequence(32, StandardCharsets.UTF_8);
            CharSequence md5 = buf.readCharSequence(32, StandardCharsets.UTF_8);
            int from = buf.readInt();
            int to = buf.readInt();
            int length = buf.readInt();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            ReconstructFileBlock reconstructFileBlock = new ModifyByteSerial()
                    .setContent(bytes)
                    .setFrom(from)
                    .setTo(to);
            reconstructBlocksFrame.setFid(fid.toString());
            reconstructBlocksFrame.setMd5(md5.toString());
            reconstructBlocksFrame.setBlocks(List.of(reconstructFileBlock));
        }
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        reconstructBlocksFrame.setOptCode(optCode);
        reconstructBlocksFrame.setContextId(contextId.toString());
        reconstructBlocksFrame.setOptId(optId.toString());
        reconstructBlocksFrame.setCrc(crc);

        return reconstructBlocksFrame;
    }

}
