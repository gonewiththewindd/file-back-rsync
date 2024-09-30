package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.model.ContinueContext;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.FileMetaInfoHolder;
import com.gone.file_backup.model.retry.FrameRetryMsg;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.FileDataFinishFrame;
import com.gone.file_backup.network.frame.FileDataPartFrame;
import com.gone.file_backup.network.frame.MetaInfoAckFrame;
import com.gone.file_backup.sender.SenderContext;
import com.gone.file_backup.utils.UUIDUtils;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MetaInfoAckOpt extends AbstractOpt<MetaInfoAckFrame> {

    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(MetaInfoAckFrame opt) {

        log.info("[Sender]receive meta info ack...opt:{}", opt);

        ContinueContext context = (ContinueContext) senderContext.continuteMap.get(opt.getContextId());
        if (Objects.isNull(context)) {
            return;
        }
        FileMetaInfoHolder holder = (FileMetaInfoHolder) context.getContext();
        FileMetaInfo f = holder.getFileMetaInfo();
        String ip = context.getIp();
        Integer port = context.getPort();
        Channel channel = opt.getChannel();
        log.info("[Sender]begin to transfer file data...fid:{}", f.getFid());
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(Paths.get(f.getAbsolutePath())))) {
            int length;
            byte[] buffer = new byte[SenderContext.BLOCK_SIZE];
            int seq = 0;
//            List<String> optIdList = new ArrayList<>();
            CountDownLatch countDownLatch = new CountDownLatch(bufferedInputStream.available() / SenderContext.BLOCK_SIZE + 1);
            while ((length = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
                String optId = UUIDUtils.randomUUID();
                // file frame
                FileDataPartFrame dataPartFrame = new FileDataPartFrame()
                        .setFid(f.getFid())
                        .setSeq(++seq)
                        .setLength(length)
                        .setContent(buffer)
                        .setBlockSize(SenderContext.BLOCK_SIZE);
                dataPartFrame.setOptCode(OptCodeEnums.FILE_DATA_PART.getValue());
                dataPartFrame.setContextId(opt.getContextId());
                dataPartFrame.setOptId(optId);

                FrameRetryMsg retryMsg = new FrameRetryMsg().setFrame(dataPartFrame);
                retryMsg.setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                senderContext.retryMap.put(optId, retryMsg); // TODO 过多的retry缓存，可以不缓存原始数据，原始数据在retry时再加载

                senderContext.ackMap.put(optId, AckResult.of(countDownLatch));
                log.info("[Sender]transfer file {}, seq:{}, length:{}", f.getFid(), seq, length);
                channel.writeAndFlush(dataPartFrame);
            }
            if (countDownLatch.await(60, TimeUnit.SECONDS)) {
                // 单个文件数据传输完成，通知远程进行文件一致性校验
                log.info("[Sender]notify remote file {} transfer finish", f.getFid(), seq, length);
                String optId = UUIDUtils.randomUUID();
                FileDataFinishFrame fileDataFinishFrame = new FileDataFinishFrame()
                        .setFid(f.getFid());
                fileDataFinishFrame.setOptCode(OptCodeEnums.FILE_DATA_FINISH.getValue());
                fileDataFinishFrame.setContextId(opt.getContextId());
                fileDataFinishFrame.setOptId(optId);

                FrameRetryMsg retryMsg = new FrameRetryMsg().setFrame(fileDataFinishFrame);
                retryMsg.setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                senderContext.retryMap.put(optId, retryMsg);
                log.info("[Sender]send file transport finish cmd, frame:{}", fileDataFinishFrame);
                channel.writeAndFlush(fileDataFinishFrame);
            } else {
                log.error("waiting for file data receive ack timeout, fid:{}", f.getFid());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
