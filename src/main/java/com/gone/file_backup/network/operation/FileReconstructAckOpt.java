package com.gone.file_backup.network.operation;

import com.gone.file_backup.network.frame.FileReconstructAckFrame;
import com.gone.file_backup.sender.SenderContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FileReconstructAckOpt extends AbstractOpt<FileReconstructAckFrame> {

    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(FileReconstructAckFrame opt) {
        log.info("[Sender]receive file reconstruct ack:{}", opt);

    }

}
