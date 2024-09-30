package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.ReconstructFile;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.FileReconstructAckFrame;
import com.gone.file_backup.network.frame.FileReconstructFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import com.gone.file_backup.utils.BeanUtils;
import com.gone.file_backup.utils.FileUtils;
import com.gone.file_backup.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@Component
public class FileReconstructOpt extends AbstractOpt<FileReconstructFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(FileReconstructFrame opt) {

        log.info("[Receiver]receive reconstruct file, frame:{}", opt);
        ReconstructFile reconstructFile = receiverContext.reconstructMap.get(opt.getFid());
        FileMetaInfo fileMetaInfo = receiverContext.fileMetaInfoMap.get(opt.getFid());
        if (Objects.isNull(fileMetaInfo)) {
            return;
        }

        FileReconstructAckFrame ack = BeanUtils.copyProperties(opt, FileReconstructAckFrame.class);
        ack.setOptCode(OptCodeEnums.FILE_RECONSTRUCT_ACK.getValue());
        ack.setFid(opt.getFid());

        Path tempFilePath = FileUtils.reconstructFile(fileMetaInfo.getDestPath(), fileMetaInfo.getSliceFile().getFileBlockList(), reconstructFile.getBlocks());
        if (Objects.isNull(tempFilePath)) {
            ack.setRetry(false);
            log.info("[Receiver]send file reconstruct ack(fast return due to block id not match), frame:{}", ack);
            opt.getChannel().writeAndFlush(ack);
            return;
        }

        String computeMD5 = SignatureUtils.computeMD5(tempFilePath.toString());
        boolean retry = !computeMD5.equalsIgnoreCase(reconstructFile.getMd5());
        if (!retry) {
            // 覆盖源文件
            try {
                Files.move(tempFilePath, Paths.get(fileMetaInfo.getDestPath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 重新分片
            SliceFile slice = FileUtils.slice(fileMetaInfo.getDestPath());
            fileMetaInfo.setSliceFile(slice);
            fileMetaInfo.setMd5(computeMD5);
        }

        ack.setRetry(retry);
        log.info("[Receiver]send file reconstruct ack, frame:{}", ack);
        opt.getChannel().writeAndFlush(ack);
    }

}
