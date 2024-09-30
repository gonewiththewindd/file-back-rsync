package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.FileSliceInfoAckFrame;
import com.gone.file_backup.network.frame.FileSliceInfoFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import com.gone.file_backup.utils.BeanUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class FileSliceInfoOpt extends AbstractOpt<FileSliceInfoFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(FileSliceInfoFrame opt) {

        log.info("[Receiver]receive file slice info:{}", opt);

        FileSliceInfoAckFrame ack = BeanUtils.copyProperties(opt, FileSliceInfoAckFrame.class);
        ack.setOptCode(OptCodeEnums.FILE_SLICE_INFO_ACK.getValue());

        FileMetaInfo fileMetaInfo = receiverContext.fileMetaInfoMap.get(opt.getFid());
        if (Objects.nonNull(fileMetaInfo) && Objects.nonNull(fileMetaInfo.getSliceFile())) {
            ack.setSliceFile(fileMetaInfo.getSliceFile());
        }

        Channel channel = opt.getChannel();
        log.info("[Receiver]send file slice info ack, frame:{}", ack);
        channel.writeAndFlush(ack);
    }

}
