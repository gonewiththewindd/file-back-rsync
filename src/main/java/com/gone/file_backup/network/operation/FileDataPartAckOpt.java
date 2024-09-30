package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.network.frame.FileDataPartAckFrame;
import com.gone.file_backup.sender.SenderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class FileDataPartAckOpt extends AbstractOpt<FileDataPartAckFrame> {

    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(FileDataPartAckFrame opt) {

        log.info("[Sender]receive file data ack :{}", opt);
        AckResult result = senderContext.ackMap.remove(opt.getOptId());
        if (Objects.nonNull(result)) {
            result.getLatch().countDown();
        }

    }
}
