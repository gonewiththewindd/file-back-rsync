package com.gone.file_backup.rsync.sender;

import com.gone.file_backup.rsync.model.ContinueContext;
import com.gone.file_backup.rsync.model.FileMetaInfo;
import com.gone.file_backup.rsync.model.FileMetaInfoHolder;
import com.gone.file_backup.rsync.model.RetryMsg;
import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import com.gone.file_backup.rsync.network.channel.PooledChannelManager;
import com.gone.file_backup.rsync.network.opt.FileDataAckOptBO;
import com.gone.file_backup.rsync.network.opt.MetaInfoAckOptBO;
import com.gone.file_backup.rsync.network.opt.OptBO;
import com.gone.file_backup.rsync.utils.Crc32Utils;
import com.gone.file_backup.rsync.utils.UUIDUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DefaultSenderImpl implements Sender {

    public static final int BLOCK_SIZE = 1024 * 64;

    @Lazy
    @Autowired
    private PooledChannelManager pooledChannelManager;

    /**
     * 重试暂存
     */
    private Map<String, RetryMsg> retryMap = new ConcurrentHashMap<>();
    private Map<String, CountDownLatch> ackMap = new ConcurrentHashMap<>();
    private Map<String, Object> continuteMap = new ConcurrentHashMap<>();

    @Override
    public void syncFileList(String ip, Integer port, List<FileMetaInfo> fileList) {
        log.info("[Sender]sync file list...");
        int size = fileList.size();
        CountDownLatch countDownLatch = new CountDownLatch(size);
        String syncId = UUIDUtils.randomUUID();
        for (FileMetaInfo f : fileList) {
            syncFile(ip, port, syncId, f, countDownLatch);
        }
        try {
            if (countDownLatch.await(60, TimeUnit.SECONDS)) {
                // TODO 所有文件同步完成, 通知远程进行文件分片
                log.info("[Sender]notify sync file list finished. ip:{}, port:{}, file size:{}", ip, port, size);




            } else {
                throw new RuntimeException("fail to sync file list");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void receiveMetaInfoAck(MetaInfoAckOptBO opt) {

        log.info("[Sender]receive meta info ack...opt:{}", opt);

        ContinueContext context = (ContinueContext) continuteMap.get(opt.getContextId());
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
            byte[] buffer = new byte[BLOCK_SIZE];
            int seq = 0;
            List<String> optIdList = new ArrayList<>(bufferedInputStream.available() / BLOCK_SIZE + 1);
            CountDownLatch countDownLatch = new CountDownLatch(optIdList.size());
            while ((length = bufferedInputStream.read(buffer, 0, buffer.length)) != -1) {
                String optId = UUIDUtils.randomUUID();
                optIdList.add(optId);
                // file frame
                ByteBuf fileFrame = Unpooled.buffer();
                fileFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
                fileFrame.writeInt(OptCodeEnums.SEND_FILE_DATA.getValue());
                fileFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
                fileFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                fileFrame.writeCharSequence(f.getFid(), StandardCharsets.UTF_8);
                fileFrame.writeInt(++seq);// file offset = seq * BLOCK_SIZE
                fileFrame.writeInt(BLOCK_SIZE);
                fileFrame.writeInt(length);
                fileFrame.writeBytes(buffer, 0, length);
                fileFrame.writeLong(Crc32Utils.computeCrc32(fileFrame.array()));
                fileFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                RetryMsg retryMsg = new RetryMsg().setIp(ip).setPort(port).setBuf(fileFrame.copy()).setLastSendAt(System.currentTimeMillis());
                retryMap.put(optId, retryMsg); // TODO 过多的retry缓存，可以不缓存原始数据，原始数据在retry时再加载
                ackMap.put(optId, countDownLatch);
                log.info("[Sender]transfer file {}, seq:{}, length:{}", f.getFid(), seq, length);
                channel.writeAndFlush(fileFrame);
            }
            if (countDownLatch.await(60, TimeUnit.SECONDS)) {
                // 单个文件完成
                continuteMap.remove(opt.getContextId());
                holder.getCountDownLatch().countDown();
            } else {
                log.error("waiting for file data receive ack timeout, fid:{}", f.getFid());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void syncFile(String ip, Integer port, String syncId, FileMetaInfo f, CountDownLatch countDownLatch) {

        pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {

            log.info("[Sender]sync file to remote '{}', info:{}", channel, f);
            String contextId = UUIDUtils.randomUUID();
            String optId = UUIDUtils.randomUUID();
            ByteBuf metaInfoFrame = Unpooled.buffer();
            metaInfoFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(OptCodeEnums.SEND_FILE_META_INFO.getValue());
            metaInfoFrame.writeCharSequence(syncId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(f.getFid(), StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(f.getName().getBytes(StandardCharsets.UTF_8).length);
            metaInfoFrame.writeCharSequence(f.getName(), StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(f.getDestPath().getBytes(StandardCharsets.UTF_8).length);
            metaInfoFrame.writeCharSequence(f.getDestPath(), StandardCharsets.UTF_8);
            metaInfoFrame.writeLong(f.getLength());
            metaInfoFrame.writeLong(Crc32Utils.computeCrc32(metaInfoFrame.array()));
            metaInfoFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
            log.info("[Sender]send sync file meta info command :{}", metaInfoFrame.toString(StandardCharsets.UTF_8));
            channel.writeAndFlush(metaInfoFrame);

            FileMetaInfoHolder fileMetaInfoHolder = new FileMetaInfoHolder()
                    .setFileMetaInfo(f)
                    .setCountDownLatch(countDownLatch);
            ContinueContext continueContext = new ContinueContext()
                    .setIp(ip)
                    .setPort(port)
                    .setContext(fileMetaInfoHolder);

            continuteMap.put(contextId, continueContext);
            RetryMsg retryMsg = new RetryMsg()
                    .setIp(ip)
                    .setPort(port)
                    .setBuf(metaInfoFrame.copy())
                    .setLastSendAt(System.currentTimeMillis());
            retryMap.put(optId, retryMsg);

            return null;
        });
    }

    @Override
    public void fetchFileInfo(String fid) {

    }

    @Override
    public void sendReconstructFile() {

    }

    @Override
    public void processFileDataAck(FileDataAckOptBO opt) {
        log.info("[Sender]receive file data ack :{}", opt);
        CountDownLatch latch = ackMap.remove(opt.getOptId());
        if (Objects.nonNull(latch)) {
            latch.countDown();
        }
    }

    @Override
    public void processFileDataEndAck(OptBO opt) {

    }

    @Override
    public Map<String, RetryMsg> retryMap() {
        return retryMap;
    }
}
