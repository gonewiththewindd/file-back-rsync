package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.MetaInfoAckFrame;
import com.gone.file_backup.network.frame.MetaInfoFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import com.gone.file_backup.utils.BeanUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class MetaInfoOpt extends AbstractOpt<MetaInfoFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(MetaInfoFrame opt) {

        log.info("[Receiver]receive file meta info :{}", opt);
        String fid = opt.getFid();
        String destPath = opt.getDestPath();

        FileMetaInfo fileMetaInfo = BeanUtils.copyProperties(opt, FileMetaInfo.class);
        receiverContext.fileMetaInfoMap.put(fid, fileMetaInfo);
        ensurePathDirectoryExists(destPath);

        MetaInfoAckFrame ack = BeanUtils.copyProperties(opt, MetaInfoAckFrame.class);
        ack.setOptCode(OptCodeEnums.FILE_META_INFO_ACK.getValue());

        Channel channel = opt.getChannel();
        log.info("[Receiver]send file meta info ack...ack:{}, channel:{}", ack, channel);
        channel.writeAndFlush(ack);
    }

    private void ensurePathDirectoryExists(String destPath) {
        Path path = Paths.get(destPath);
        File directory = path.getParent().toFile();
        if (!directory.exists()) {
            log.info("[Receiver]directory not exists :{}", destPath);
            if (!directory.mkdirs()) {
                //TODO 创建目录失败
                throw new RuntimeException("Failed to create directory " + directory);
            }
            log.info("[Receiver]directory created :{}", destPath);
        }
    }
}
