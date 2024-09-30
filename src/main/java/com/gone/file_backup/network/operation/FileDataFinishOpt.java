package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.FileDataFinishAckFrame;
import com.gone.file_backup.network.frame.FileDataFinishFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import com.gone.file_backup.utils.FileUtils;
import com.gone.file_backup.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
@Component
public class FileDataFinishOpt extends AbstractOpt<FileDataFinishFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(FileDataFinishFrame opt) {

        log.info("[Receiver]receive file data finish:{}", opt);
        // 计算文件md5
        FileMetaInfo metaInfo = receiverContext.fileMetaInfoMap.get(opt.getFid());
        if (Objects.isNull(metaInfo)) {
            //
        }
        String md5 = SignatureUtils.computeMD5(metaInfo.getDestPath());
        String originMD5 = metaInfo.getMd5();
        boolean retry = !md5.equalsIgnoreCase(originMD5);

        if (!retry) {
            // 校验通过生成文件切片信息
            String destPath = metaInfo.getDestPath();
            SliceFile sliceFile = FileUtils.slice(destPath);
            metaInfo.setSliceFile(sliceFile);
        } else {
            try {
                Files.deleteIfExists(Paths.get(metaInfo.getDestPath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        FileDataFinishAckFrame ack = new FileDataFinishAckFrame()
                .setFid(opt.getFid())
                .setRetry(retry);
        ack.setOptCode(OptCodeEnums.FILE_DATA_FINISH_ACK.getValue())
                .setContextId(opt.getContextId())
                .setOptId(opt.getOptId());

        log.info("[Receiver]send file data finish ack:{}", ack);
        opt.getChannel().writeAndFlush(ack);
    }
}
