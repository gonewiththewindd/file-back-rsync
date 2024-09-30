package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.FileDataPartAckFrame;
import com.gone.file_backup.network.frame.FileDataPartFrame;
import com.gone.file_backup.network.frame.OperationBaseFrame;
import com.gone.file_backup.receiver.ReceiverContext;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.RandomAccessFileMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Paths;

@Slf4j
@Component
public class FileDataPartOpt extends AbstractOpt<FileDataPartFrame> {

    @Autowired
    private ReceiverContext receiverContext;

    @Override
    public void process(FileDataPartFrame opt) {

        log.info("[Receiver]receive file data:{}", opt);

        String fid = opt.getFid();
        FileMetaInfo metaInfo = receiverContext.fileMetaInfoMap.get(fid);
        String path = metaInfo.getDestPath();
        int offset = (opt.getSeq() - 1) * opt.getBlockSize();
        File file = Paths.get(path).toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, RandomAccessFileMode.READ_WRITE.toString())) {
            raf.seek(offset);
            raf.write(opt.getContent());
        } catch (Exception e) {
            // TODO 写文件失败
            throw new RuntimeException(e);
        }

        OperationBaseFrame ack = new FileDataPartAckFrame()
                .setFid(fid)
                .setSeq(opt.getSeq())
                .setOptCode(OptCodeEnums.FILE_DATA_PART_ACK.getValue())
                .setContextId(opt.getContextId())
                .setOptId(opt.getOptId());

        Channel channel = opt.getChannel();
        log.info("[Receiver]send file data ack...fid:{}, seq:{}, acl:{}", fid, opt.getSeq(), ack);
        channel.writeAndFlush(ack);
    }
}
