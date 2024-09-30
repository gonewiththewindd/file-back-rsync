package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.network.frame.FileSliceInfoAckFrame;
import com.gone.file_backup.sender.SenderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class FileSliceInfoAckOpt extends AbstractOpt<FileSliceInfoAckFrame> {

    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(FileSliceInfoAckFrame opt) {

        log.info("[Sender]receive file slice info ack, frame:{}", opt);
        SliceFile sliceFile = opt.getSliceFile();
        AckResult ackResult = senderContext.ackMap.get(opt.getOptId());
        if (Objects.nonNull(ackResult)) {
            ackResult.setResult(sliceFile);
            ackResult.getLatch().countDown();
        }

    }
}
