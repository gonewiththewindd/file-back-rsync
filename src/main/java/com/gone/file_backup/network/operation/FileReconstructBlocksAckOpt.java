package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.network.frame.FileReconstructBlocksAckFrame;
import com.gone.file_backup.sender.SenderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class FileReconstructBlocksAckOpt extends AbstractOpt<FileReconstructBlocksAckFrame> {

    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(FileReconstructBlocksAckFrame opt) {

        log.info("[Sender]receive file reconstruct blocks ack:{}", opt);
        AckResult ackResult = senderContext.ackMap.remove(opt.getOptId());
        if (Objects.nonNull(ackResult)) {
            ackResult.getLatch().countDown();
        }

    }

}
