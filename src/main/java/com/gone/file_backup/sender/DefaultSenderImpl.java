package com.gone.file_backup.sender;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.constants.NetworkConstants;
import com.gone.file_backup.file.reconstruct.FileBlockTypeEnums;
import com.gone.file_backup.file.reconstruct.MatchedFileBlock;
import com.gone.file_backup.file.reconstruct.ModifyByteSerial;
import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.*;
import com.gone.file_backup.model.retry.BufRetryMsg;
import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.channel.PooledChannelManager;
import com.gone.file_backup.network.frame.*;
import com.gone.file_backup.utils.BeanUtils;
import com.gone.file_backup.utils.Crc32Utils;
import com.gone.file_backup.utils.SignatureUtils;
import com.gone.file_backup.utils.UUIDUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultSenderImpl implements Sender, SenderAckAbility {

    public static final int BLOCK_SIZE = 1024 * 64;

    @Lazy
    @Autowired
    private PooledChannelManager pooledChannelManager;

    @Autowired
    private SenderContext senderContext;

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
//                String optId = UUIDUtils.randomUUID();
//                ByteBuf fullSyncFinishFrame = Unpooled.buffer();
//                fullSyncFinishFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
//                fullSyncFinishFrame.writeInt(OptCodeEnums.FULL_SYNC_FINISH.getValue());
//                fullSyncFinishFrame.writeCharSequence(syncId, StandardCharsets.UTF_8);
//                fullSyncFinishFrame.writeCharSequence(UUIDUtils.randomUUID(), StandardCharsets.UTF_8);
//                fullSyncFinishFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
//                fullSyncFinishFrame.writeLong(Crc32Utils.computeCrc32(fullSyncFinishFrame.array()));
//                fullSyncFinishFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
//
//                RetryMsg retryMsg = new RetryMsg().setIp(ip).setPort(port).setBuf(fullSyncFinishFrame.copy()).setLastSendAt(System.currentTimeMillis());
//                senderContext.retryMap.put(optId, retryMsg);
//
//                pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {
//                    log.info("[Sender]send full sync finish opt, frame:{}", fullSyncFinishFrame);
//                    channel.writeAndFlush(fullSyncFinishFrame);
//                    return null;
//                });
            } else {
                throw new RuntimeException("sync file list timeout...but will try to achieve final consistence");
            }
        } catch (InterruptedException e) {
            log.error("[Sender]sync file list interrupted...", e);
            throw new RuntimeException("sync file list interrupted");
        }
    }

    @Override
    public void receiveMetaInfoAck(MetaInfoAckFrame opt) {

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
                fileFrame.writeInt(OptCodeEnums.FILE_DATA_PART.getValue());
                fileFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
                fileFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                fileFrame.writeCharSequence(f.getFid(), StandardCharsets.UTF_8);
                fileFrame.writeInt(++seq);// file offset = seq * BLOCK_SIZE
                fileFrame.writeInt(BLOCK_SIZE);
                fileFrame.writeInt(length);
                fileFrame.writeBytes(buffer, 0, length);
                fileFrame.writeLong(Crc32Utils.computeCrc32(fileFrame.array()));
                fileFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                RetryMsg retryMsg = new BufRetryMsg().setBuf(fileFrame.copy()).setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                senderContext.retryMap.put(optId, retryMsg); // TODO 过多的retry缓存，可以不缓存原始数据，原始数据在retry时再加载
                senderContext.ackMap.put(optId, AckResult.of(countDownLatch));
                log.info("[Sender]transfer file {}, seq:{}, length:{}", f.getFid(), seq, length);
                channel.writeAndFlush(fileFrame);
            }
            if (countDownLatch.await(60, TimeUnit.SECONDS)) {
                // 单个文件数据传输完成，通知远程进行文件一致性校验
                log.info("[Sender]notify remote file {} transfer finish", f.getFid(), seq, length);
                String optId = UUIDUtils.randomUUID();
                ByteBuf fileDataFinishFrame = Unpooled.buffer();
                fileDataFinishFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
                fileDataFinishFrame.writeInt(OptCodeEnums.FILE_DATA_FINISH.getValue());
                fileDataFinishFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
                fileDataFinishFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                fileDataFinishFrame.writeCharSequence(f.getFid(), StandardCharsets.UTF_8);
                fileDataFinishFrame.writeLong(Crc32Utils.computeCrc32(fileDataFinishFrame.array()));
                fileDataFinishFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                BufRetryMsg retryMsg = new BufRetryMsg().setBuf(fileDataFinishFrame.copy());
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

    public void syncFile(String ip, Integer port, FileMetaInfo f, CountDownLatch countDownLatch) {

        pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {

            log.info("[Sender]sync file to remote '{}', info:{}", channel, f);
            String contextId = UUIDUtils.randomUUID();
            String optId = UUIDUtils.randomUUID();
            ByteBuf metaInfoFrame = Unpooled.buffer();
            metaInfoFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(OptCodeEnums.FILE_META_INFO.getValue());
//            metaInfoFrame.writeCharSequence(syncId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(f.getFid(), StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(f.getName().getBytes(StandardCharsets.UTF_8).length);
            metaInfoFrame.writeCharSequence(f.getName(), StandardCharsets.UTF_8);
            metaInfoFrame.writeInt(f.getDestPath().getBytes(StandardCharsets.UTF_8).length);
            metaInfoFrame.writeCharSequence(f.getDestPath(), StandardCharsets.UTF_8);
            metaInfoFrame.writeCharSequence(f.getMd5(), StandardCharsets.UTF_8);
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

            senderContext.continuteMap.put(contextId, continueContext);
            RetryMsg retryMsg = new BufRetryMsg()
                    .setBuf(metaInfoFrame.copy())
                    .setIp(ip)
                    .setPort(port)
                    .setLastSendAt(System.currentTimeMillis());
            senderContext.retryMap.put(optId, retryMsg);

            return null;
        });
    }

    @Override
    public void processFileDataPartAck(FileDataPartAckFrame opt) {
        log.info("[Sender]receive file data ack :{}", opt);
        AckResult result = senderContext.ackMap.remove(opt.getOptId());
        if (Objects.nonNull(result)) {
            result.getLatch().countDown();
        }
    }

    @Override
    public void processFileDataFinishAck(FileDataFinishAckFrame opt) {

        log.info("[Sender]receive file data finish ack :{}", opt);
        String fid = opt.getFid();
        boolean retry = opt.isRetry();
        ContinueContext context = (ContinueContext) senderContext.continuteMap.remove(opt.getContextId());
        if (Objects.isNull(context)) {
            return;
        }
        FileMetaInfoHolder holder = (FileMetaInfoHolder) context.getContext();
        if (retry) {
            // 文件重传，读取最新文件，并重新生成文件元数据信息
            log.info("[Sender]file data transport retry...fid:{}, contextId:{}", fid, opt.getContextId());
            refreshFileMetaInfo(holder);
            String ip = context.getIp();
            int port = context.getPort();
//            senderContext.continuteMap.remove(opt.getContextId());
            syncFile(ip, port, holder.getFileMetaInfo(), holder.getCountDownLatch());
        } else {
            // 清楚文件缓存信息
            log.info("[Sender]file data transport finish, clear file meta info cache...fid:{}, contextId:{}", fid, opt.getContextId());
//            senderContext.continuteMap.remove(opt.getContextId());
            holder.getCountDownLatch().countDown();
        }
    }

    private void refreshFileMetaInfo(FileMetaInfoHolder holder) {
        String absolutePath = holder.getFileMetaInfo().getAbsolutePath();
        File file = Paths.get(absolutePath).toFile();
        String name = file.getName();
        long length = file.length();
        long l = file.lastModified();
        FileMetaInfo fileMetaInfo = new FileMetaInfo()
                .setFid(holder.getFileMetaInfo().getFid())
                .setName(name)
                .setLength(length)
                .setLastModified(l)
                .setDestPath(holder.getFileMetaInfo().getDestPath())
                .setAbsolutePath(absolutePath);
        fileMetaInfo.setMd5(SignatureUtils.computeMD5(absolutePath));
        holder.setFileMetaInfo(fileMetaInfo);
    }

    @Override
    public Map<String, RetryMsg> retryMap() {
        return senderContext.retryMap;
    }

    @Override
    public SliceFile fetchFileSliceInfo(String ip, int port, String fid) {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        String optId = UUIDUtils.randomUUID();
        pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {

            log.info("[Sender]fetch file slice info, fid:{}", fid);
            String contextId = UUIDUtils.randomUUID();
            ByteBuf fileSliceInfoFrame = Unpooled.buffer();
            fileSliceInfoFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
            fileSliceInfoFrame.writeInt(OptCodeEnums.FILE_SLICE_INFO.getValue());
            fileSliceInfoFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
            fileSliceInfoFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
            fileSliceInfoFrame.writeCharSequence(fid, StandardCharsets.UTF_8);
            fileSliceInfoFrame.writeLong(Crc32Utils.computeCrc32(fileSliceInfoFrame.array()));
            fileSliceInfoFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

            RetryMsg retryMsg = new BufRetryMsg().setBuf(fileSliceInfoFrame.copy()).setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
            senderContext.ackMap.put(optId, AckResult.of(countDownLatch));
            senderContext.retryMap.put(optId, retryMsg);

            log.info("[Sender]send file slice info cmd, frame:{}", fileSliceInfoFrame);
            channel.writeAndFlush(fileSliceInfoFrame);

            return null;
        });

        try {
            if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                AckResult ackResult = senderContext.ackMap.remove(optId);
                return (SliceFile) ackResult.getResult();
            } else {
                log.error("[Sender]fetch file slice info cmd, timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public void processFileSliceInfoAck(FileSliceInfoAckFrame opt) {
        log.info("[Sender]receive file slice info ack, frame:{}", opt);
        SliceFile sliceFile = opt.getSliceFile();
        AckResult ackResult = senderContext.ackMap.get(opt.getOptId());
        if (Objects.nonNull(ackResult)) {
            ackResult.setResult(sliceFile);
            ackResult.getLatch().countDown();
        }
    }

    @Override
    public void sendReconstructFileBlocks(String ip, int port, FileMetaInfo modifiedFile, List<ReconstructFileBlock> reconstructFileBlocks) {

        long start = System.currentTimeMillis();
        AtomicInteger ackCnt = new AtomicInteger();
        // 同步文件重构块列表
        List<ReconstructFileBlock> matchedFileBlockList = reconstructFileBlocks.stream()
                .filter(block -> block instanceof MatchedFileBlock)
                .collect(Collectors.toList());
        int times = 0;
        if (CollectionUtils.isNotEmpty(matchedFileBlockList)) {
            reconstructFileBlocks.removeAll(matchedFileBlockList);
            int mod = matchedFileBlockList.size() % 1000;
            times = mod == 0 ? matchedFileBlockList.size() / 1000 : matchedFileBlockList.size() / 1000 + 1;
            ackCnt.addAndGet(times);
        }
        ackCnt.addAndGet(reconstructFileBlocks.size());

        String contextId = UUIDUtils.randomUUID();
        int finalTimes = times;
        CountDownLatch ackLatch = new CountDownLatch(ackCnt.get());
        pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {
            // 匹配文件块
            if (CollectionUtils.isNotEmpty(matchedFileBlockList)) {
                for (int i = 0; i < finalTimes; i++) {
                    int end = (i + 1000) > matchedFileBlockList.size() ? matchedFileBlockList.size() : (i + 1000);
                    List<CompressFileReconstructBlock.CompactMatchFileBlock> blocks = BeanUtils.copyList(
                            matchedFileBlockList.subList(i, end),
                            CompressFileReconstructBlock.CompactMatchFileBlock.class
                    );
                    CompressFileReconstructBlock compressFileReconstructBlock = new CompressFileReconstructBlock()
                            .setFid(modifiedFile.getFid())
                            .setMd5(modifiedFile.getMd5())
                            .setBlocks(blocks);
                    String json = JSON.toJSONString(compressFileReconstructBlock);
                    int length = json.getBytes(StandardCharsets.UTF_8).length;

                    String optId = UUIDUtils.randomUUID();
                    ByteBuf fileReconstructListFrame = Unpooled.buffer();
                    fileReconstructListFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
                    fileReconstructListFrame.writeInt(OptCodeEnums.FILE_RECONSTRUCT_BLOCKS.getValue());
                    fileReconstructListFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
                    fileReconstructListFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                    fileReconstructListFrame.writeInt(FileBlockTypeEnums.MATCHED.getValue());
                    fileReconstructListFrame.writeInt(length);
                    fileReconstructListFrame.writeCharSequence(json, StandardCharsets.UTF_8);
                    fileReconstructListFrame.writeLong(Crc32Utils.computeCrc32(fileReconstructListFrame.array()));
                    fileReconstructListFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                    RetryMsg retryMsg = new BufRetryMsg().setBuf(fileReconstructListFrame.copy()).setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                    senderContext.retryMap.put(optId, retryMsg);
                    senderContext.ackMap.put(optId, AckResult.of(ackLatch));

                    log.info("[Sender]send file reconstruct list. fid:{}, list size:{}", modifiedFile.getFid(), blocks.size());
                    channel.writeAndFlush(fileReconstructListFrame);
                }
            }

            // 连续修改字节块
            ackCnt.getAndAdd(reconstructFileBlocks.size());
            reconstructFileBlocks.forEach(modifyByteBlock -> {

                ModifyByteSerial serial = (ModifyByteSerial) modifyByteBlock;
                String optId = UUIDUtils.randomUUID();
                ByteBuf fileReconstructListFrame = Unpooled.buffer();
                fileReconstructListFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
                fileReconstructListFrame.writeInt(OptCodeEnums.FILE_RECONSTRUCT_BLOCKS.getValue());
                fileReconstructListFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
                fileReconstructListFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                fileReconstructListFrame.writeInt(FileBlockTypeEnums.SERIAL.getValue());
                fileReconstructListFrame.writeCharSequence(modifiedFile.getFid(), StandardCharsets.UTF_8);
                fileReconstructListFrame.writeCharSequence(modifiedFile.getMd5(), StandardCharsets.UTF_8);
                fileReconstructListFrame.writeInt(serial.getFrom());
                fileReconstructListFrame.writeInt(serial.getTo());
                fileReconstructListFrame.writeInt(serial.getContent().length);
                fileReconstructListFrame.writeBytes(serial.getContent());
                fileReconstructListFrame.writeLong(Crc32Utils.computeCrc32(fileReconstructListFrame.array()));
                fileReconstructListFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                RetryMsg retryMsg = new BufRetryMsg().setBuf(fileReconstructListFrame.copy()).setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                senderContext.retryMap.put(optId, retryMsg);
                senderContext.ackMap.put(optId, AckResult.of(ackLatch));

                log.info("[Sender]send file reconstruct byte serial. fid:{}, serial size:{}", modifiedFile.getFid(), serial.getContent().length);
                channel.writeAndFlush(fileReconstructListFrame);
            });

            return null;
        });

        try {
            if (ackLatch.await(60, TimeUnit.SECONDS)) {
                // 通知远程进行文件重构
                pooledChannelManager.executeOnChannelAcquire(ip, port, channel -> {
                    String optId = UUIDUtils.randomUUID();
                    ByteBuf fileReconstructFrame = Unpooled.buffer();
                    fileReconstructFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
                    fileReconstructFrame.writeInt(OptCodeEnums.FILE_RECONSTRUCT.getValue());
                    fileReconstructFrame.writeCharSequence(contextId, StandardCharsets.UTF_8);
                    fileReconstructFrame.writeCharSequence(optId, StandardCharsets.UTF_8);
                    fileReconstructFrame.writeCharSequence(modifiedFile.getFid(), StandardCharsets.UTF_8);
                    fileReconstructFrame.writeLong(Crc32Utils.computeCrc32(fileReconstructFrame.array()));
                    fileReconstructFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

                    RetryMsg retryMsg = new BufRetryMsg().setBuf(fileReconstructFrame.copy()).setIp(ip).setPort(port).setLastSendAt(System.currentTimeMillis());
                    senderContext.retryMap.put(optId, retryMsg);

                    log.info("[Sender]send file reconstruct cmd.fid:{}", modifiedFile.getFid());
                    channel.writeAndFlush(fileReconstructFrame);

                    return null;
                });
            } else {
                log.error("[Sender]receive file reconstruct transport ack timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processFileReconstructListAck(OperationBaseFrame opt) {
        log.info("[Sender]receive file reconstruct ack:{}", opt);
        AckResult ackResult = senderContext.ackMap.remove(opt.getOptId());
        if (Objects.nonNull(ackResult)) {
            ackResult.getLatch().countDown();
        }
    }
}
