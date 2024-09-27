package com.gone.file_backup.rsync.receiver;

import com.gone.file_backup.file.FileItem;
import com.gone.file_backup.rsync.file.FileSliceService;
import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import com.gone.file_backup.rsync.network.opt.*;
import com.gone.file_backup.rsync.utils.Crc32Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.RandomAccessFileMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class DefaultReceiverImpl implements Receiver {

    @Autowired
    private FileSliceService fileSliceService;

    public Map<String, Set<String>> syncIdToFileIdMap = new ConcurrentHashMap<>();
    public Map<String, MetaInfoOptBO> fileMetaInfoMap = new ConcurrentHashMap<>();

    @Override
    public void processMetaInfoOpt(MetaInfoOptBO opt) {

        log.info("[Receiver]receive file meta info :{}", opt);
        String fid = opt.getFid();
        String destPath = opt.getDestPath();
        fileMetaInfoMap.put(fid, opt);
        ensurePathDirectoryExists(destPath);

        String syncId = opt.getSyncId();
        Set<String> fileIdSet = syncIdToFileIdMap.computeIfAbsent(syncId, k -> new CopyOnWriteArraySet<>());
        fileIdSet.add(fid);

        Channel channel = opt.getChannel();
        ByteBuf metaInfoAckFrame = Unpooled.buffer();
        metaInfoAckFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        metaInfoAckFrame.writeInt(OptCodeEnums.SEND_FILE_META_INFO_ACK.getValue());
        metaInfoAckFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        metaInfoAckFrame.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);
        metaInfoAckFrame.writeLong(Crc32Utils.computeCrc32(metaInfoAckFrame.array()));
        metaInfoAckFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

        log.info("[Receiver]send file meta info ack...ack:{}, channel:{}", metaInfoAckFrame, channel);
        channel.writeAndFlush(metaInfoAckFrame);
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

    @Override
    public void processFileDataOpt(FileDataOptBO opt) {

        log.info("[Receiver]receive file data:{}", opt);

        String fid = opt.getFid();
        MetaInfoOptBO metaInfo = fileMetaInfoMap.get(fid);
        String path = metaInfo.getDestPath();
        int offset = (opt.getSeq() - 1) * opt.getBlockSize();
        File file = Paths.get(path).toFile();
        try (RandomAccessFile raf = new RandomAccessFile(file, RandomAccessFileMode.READ_WRITE.toString())) {
            raf.seek(offset);
            byte[] bytes = new byte[(int) opt.getLength()];
            opt.getContent().readBytes(bytes);
            raf.write(bytes);
        } catch (Exception e) {
            // TODO 写文件失败
            throw new RuntimeException(e);
        }

        Channel channel = opt.getChannel();
        ByteBuf fileDataAck = Unpooled.buffer();
        fileDataAck.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        fileDataAck.writeInt(OptCodeEnums.SEND_FILE_DATA_ACK.getValue());
        fileDataAck.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        fileDataAck.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);
        fileDataAck.writeCharSequence(opt.getFid(), StandardCharsets.UTF_8);
        fileDataAck.writeInt(opt.getSeq());
        fileDataAck.writeLong(Crc32Utils.computeCrc32(fileDataAck.array()));
        fileDataAck.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

        log.info("[Receiver]send file data ack...fid:{}, seq:{}", fid, opt.getSeq());
        channel.writeAndFlush(fileDataAck);
    }

    @Override
    public void processFullSyncFinishOpt(FullSyncFinishOptBO opt) {

        String syncId = opt.getSyncId();
        String remoteDirectory = opt.getRemoteDirectory();
        Set<String> fileIdList = syncIdToFileIdMap.get(syncId);
        // 进行文件切分、快速签名和强签名计算
        fileIdList.forEach(fid -> {
            MetaInfoOptBO metaInfo = fileMetaInfoMap.remove(fid);
            String destPath = metaInfo.getDestPath();
            FileItem file = fileSliceService.slice(fid, destPath);
        });
        //
        

    }

    @Override
    public void executeOpt(Opt opt, OptBO optBO) {


    }
}
