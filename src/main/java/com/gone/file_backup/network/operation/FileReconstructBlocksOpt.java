package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.ReconstructFile;
import com.gone.file_backup.network.frame.FileReconstructBlocksAckFrame;
import com.gone.file_backup.network.frame.FileReconstructBlocksFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import com.gone.file_backup.utils.BeanUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class FileReconstructBlocksOpt extends AbstractOpt<FileReconstructBlocksFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(FileReconstructBlocksFrame opt) {

        log.info("[Receiver]receive reconstruct blocks, frame:{}", opt);
        ReconstructFile reconstructFile = receiverContext.reconstructMap.computeIfAbsent(opt.getFid(), key -> new ReconstructFile());
        if (Objects.isNull(reconstructFile.getFid())) {
            reconstructFile.setFid(opt.getFid());
        }
        if (Objects.isNull(reconstructFile.getMd5())) {
            reconstructFile.setMd5(opt.getMd5());
        }
        reconstructFile.getBlocks().addAll(opt.getBlocks());

        FileReconstructBlocksAckFrame ack = BeanUtils.copyProperties(opt, FileReconstructBlocksAckFrame.class);

        Channel channel = opt.getChannel();
        log.info("[Receiver]send reconstruct list ack, frame:{}", ack);
        channel.writeAndFlush(ack);
    }

}
