package com.gone.file_backup.sender;

import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.ContinueContext;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.FileMetaInfoHolder;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.model.retry.FrameRetryMsg;
import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.channel.PooledChannelManager;
import com.gone.file_backup.network.frame.MetaInfoFrame;
import com.gone.file_backup.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CoreSenderImpl implements Sender {

    @Autowired
    private SenderContext senderContext;
    @Autowired
    private PooledChannelManager pooledChannelManager;

    @Override
    public void syncFileList(String ip, Integer port, List<FileMetaInfo> fileList) {
        log.info("[Sender]sync file list...");
        int size = fileList.size();
        CountDownLatch countDownLatch = new CountDownLatch(size);
        for (FileMetaInfo f : fileList) {
            syncFile(ip, port, f, countDownLatch);
        }
        try {
            if (countDownLatch.await(60, TimeUnit.SECONDS)) {
                // 所有文件同步完成
                log.info("[Sender]notify sync file list finished. ip:{}, port:{}, file size:{}", ip, port, size);
            } else {
                log.warn("sync file list timeout...but will try to achieve final consistence");
            }
        } catch (InterruptedException e) {
            log.error("[Sender]sync file list interrupted...", e);
            throw new RuntimeException("sync file list interrupted");
        }
    }

    @Override
    public void syncFile(String ip, Integer port, FileMetaInfo f, CountDownLatch countDownLatch) {
        pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {

            log.info("[Sender]sync file to remote '{}', info:{}", channel, f);
            String contextId = UUIDUtils.randomUUID();
            String optId = UUIDUtils.randomUUID();

            MetaInfoFrame metaInfoFrame = new MetaInfoFrame()
                    .setFid(f.getFid())
                    .setName(f.getName())
                    .setLength(f.getLength())
                    .setDestPath(f.getDestPath())
                    .setMd5(f.getMd5());
            metaInfoFrame.setOptCode(OptCodeEnums.FILE_META_INFO.getValue());
            metaInfoFrame.setContextId(contextId);
            metaInfoFrame.setOptId(optId);

            log.info("[Sender]send sync file meta info command :{}", metaInfoFrame);
            channel.writeAndFlush(metaInfoFrame);

            FileMetaInfoHolder fileMetaInfoHolder = new FileMetaInfoHolder()
                    .setFileMetaInfo(f)
                    .setCountDownLatch(countDownLatch);
            ContinueContext continueContext = new ContinueContext()
                    .setIp(ip)
                    .setPort(port)
                    .setContext(fileMetaInfoHolder);

            senderContext.continuteMap.put(contextId, continueContext);
            RetryMsg retryMsg = new FrameRetryMsg()
                    .setFrame(metaInfoFrame)
                    .setIp(ip)
                    .setPort(port)
                    .setLastSendAt(System.currentTimeMillis());
            senderContext.retryMap.put(optId, retryMsg);

            return null;
        });
    }

    @Override
    public SliceFile fetchFileSliceInfo(String ip, int port, String fid) {
        return null;
    }

    @Override
    public void sendReconstructFileBlocks(String ip, int port, FileMetaInfo modifiedFile, List<ReconstructFileBlock> reconstructFileBlocks) {

    }
}
