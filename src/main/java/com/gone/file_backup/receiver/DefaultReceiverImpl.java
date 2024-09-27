package com.gone.file_backup.receiver;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.ReconstructFile;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.*;
import com.gone.file_backup.utils.BeanUtils;
import com.gone.file_backup.utils.Crc32Utils;
import com.gone.file_backup.utils.FileUtils;
import com.gone.file_backup.utils.SignatureUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.RandomAccessFileMode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DefaultReceiverImpl implements Receiver {

    public Map<String, Set<String>> syncIdToFileIdMap = new ConcurrentHashMap<>();
    public Map<String, FileMetaInfo> fileMetaInfoMap = new ConcurrentHashMap<>();
    public Map<String, ReconstructFile> reconstructMap = new ConcurrentHashMap<>();

    @Override
    public void processMetaInfoOpt(MetaInfoFrame opt) {

        //TODO 如果文件重复多路径备份，应该允许远程快速检测文件是否存在，在决定是否通知进行文件传输
        log.info("[Receiver]receive file meta info :{}", opt);
        String fid = opt.getFid();
        String destPath = opt.getDestPath();

        FileMetaInfo fileMetaInfo = BeanUtils.copyProperties(opt, FileMetaInfo.class);
        fileMetaInfoMap.put(fid, fileMetaInfo);
        ensurePathDirectoryExists(destPath);

        Channel channel = opt.getChannel();
        ByteBuf metaInfoAckFrame = Unpooled.buffer();
        metaInfoAckFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        metaInfoAckFrame.writeInt(OptCodeEnums.FILE_META_INFO_ACK.getValue());
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
    public void processFileDataOpt(FileDataPartFrame opt) {

        log.info("[Receiver]receive file data:{}", opt);

        String fid = opt.getFid();
        FileMetaInfo metaInfo = fileMetaInfoMap.get(fid);
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
        fileDataAck.writeInt(OptCodeEnums.FILE_DATA_PART_TRANSPORT_ACK.getValue());
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
    public void processFileDataFinishOpt(FileDataFinishFrame opt) {
        log.info("[Receiver]receive file data finish:{}", opt);
        // 计算文件md5
        FileMetaInfo metaInfo = fileMetaInfoMap.get(opt.getFid());
        if (Objects.isNull(metaInfo)) {
            //
        }
        String md5 = SignatureUtils.computeMD5(metaInfo.getDestPath());
        String originMD5 = metaInfo.getMd5();
        boolean retry = !md5.equalsIgnoreCase(originMD5);

        if (!retry) {
            // 校验通过生成文件切片信息
            String destPath = metaInfo.getDestPath();
            SliceFile sliceFile = FileUtils.slice(opt.getFid(), destPath);
            metaInfo.setSliceFile(sliceFile);
        }

        ByteBuf fileDataFinishAck = Unpooled.buffer();
        fileDataFinishAck.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        fileDataFinishAck.writeInt(OptCodeEnums.FILE_DATA_TRANSPORT_FINISH_ACK.getValue());
        fileDataFinishAck.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        fileDataFinishAck.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);
        fileDataFinishAck.writeCharSequence(opt.getFid(), StandardCharsets.UTF_8);
        fileDataFinishAck.writeBoolean(retry);
        fileDataFinishAck.writeLong(Crc32Utils.computeCrc32(fileDataFinishAck.array()));
        fileDataFinishAck.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);

        log.info("[Receiver]send file data finish ack:{}", fileDataFinishAck);
        opt.getChannel().writeAndFlush(fileDataFinishAck);
    }

    @Override
    public void processFullSyncFinishOpt(FullSyncFinishFrame opt) {
        log.info("[Receiver]receive full sync finish, opt:{}", opt);
        String syncId = opt.getSyncId();
        Set<String> fileIdList = syncIdToFileIdMap.get(syncId);
        // 进行文件切分、快速签名和强签名计算
        fileIdList.forEach(fid -> {
            FileMetaInfo metaInfo = fileMetaInfoMap.remove(fid);
            String destPath = metaInfo.getDestPath();
            SliceFile sliceFile = FileUtils.slice(fid, destPath);
            metaInfo.setSliceFile(sliceFile);
        });
        // 发送全量同步确认
        Channel channel = opt.getChannel();
        ByteBuf fullSyncFinishAckFrame = Unpooled.buffer();
        fullSyncFinishAckFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        fullSyncFinishAckFrame.writeInt(OptCodeEnums.FULL_SYNC_FINISH_ACK.getValue());
        fullSyncFinishAckFrame.writeCharSequence(opt.getSyncId(), StandardCharsets.UTF_8);
        fullSyncFinishAckFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        fullSyncFinishAckFrame.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);
        fullSyncFinishAckFrame.writeLong(Crc32Utils.computeCrc32(fullSyncFinishAckFrame.array()));
        fullSyncFinishAckFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
        log.info("[Receiver]send full sync finish ack, frame:{}", opt);
        channel.writeAndFlush(fullSyncFinishAckFrame);
    }

    @Override
    public void executeOpt(Opt opt, OperationBaseFrame optBO) {


    }

    @SneakyThrows
    @Override
    public void processFileSliceInfo(FileSliceInfoFrame opt) {
        log.info("[Receiver]receive file slice info:{}", opt);
        FileMetaInfo fileMetaInfo = fileMetaInfoMap.get(opt.getFid());
        Channel channel = opt.getChannel();
        ByteBuf fileSliceInfoFrame = Unpooled.buffer();
        fileSliceInfoFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        fileSliceInfoFrame.writeInt(OptCodeEnums.FILE_SLICE_INFO_ACK.getValue());
        fileSliceInfoFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        fileSliceInfoFrame.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);

        if (Objects.nonNull(fileMetaInfo) && Objects.nonNull(fileMetaInfo.getSliceFile())) {
            String json = JSON.toJSONString(fileMetaInfo.getSliceFile());

            fileSliceInfoFrame.writeInt(json.getBytes(StandardCharsets.UTF_8).length);
            fileSliceInfoFrame.writeCharSequence(json, StandardCharsets.UTF_8);
        } else {
            fileSliceInfoFrame.writeInt(0);
        }

        fileSliceInfoFrame.writeLong(Crc32Utils.computeCrc32(fileSliceInfoFrame.array()));
        fileSliceInfoFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
        log.info("[Receiver]send file slice info ack, frame:{}", opt);
        channel.writeAndFlush(fileSliceInfoFrame);
    }


    @Override
    public void processReconstructList(FileReconstructListFrame opt) {

        log.info("[Receiver]receive reconstruct list transport, frame:{}", opt);
        ReconstructFile reconstructFile = reconstructMap.computeIfAbsent(opt.getFid(), key -> new ReconstructFile());
        if (Objects.isNull(reconstructFile.getFid())) {
            reconstructFile.setFid(opt.getFid());
        }
        if (Objects.isNull(reconstructFile.getMd5())) {
            reconstructFile.setMd5(opt.getMd5());
        }
        reconstructFile.getBlocks().addAll(opt.getBlocks());

        Channel channel = opt.getChannel();
        ByteBuf ack = opt.ack();
        log.info("[Receiver]send reconstruct list ack, frame:{}", ack);
        channel.writeAndFlush(ack);
    }

    @Override
    public void processFileReconstructListTransportFinish(FileReconstructFrame opt) {
        log.info("[Receiver]receive reconstruct file, frame:{}", opt);
        ReconstructFile reconstructFile = reconstructMap.get(opt.getFid());
        FileMetaInfo fileMetaInfo = fileMetaInfoMap.get(opt.getFid());
        if (Objects.isNull(fileMetaInfo)) {
            return;
        }
        Path tempFilePath = FileUtils.reconstructFile(fileMetaInfo.getDestPath(), fileMetaInfo.getSliceFile().getFileBlockList(), reconstructFile.getBlocks());
        String computeMD5 = SignatureUtils.computeMD5(tempFilePath.toString());
        boolean retry = !computeMD5.equalsIgnoreCase(reconstructFile.getMd5());
        if (!retry) {
            // 覆盖源文件
            try {
                Files.move(tempFilePath, Paths.get(fileMetaInfo.getDestPath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 重新分片
            SliceFile slice = FileUtils.slice(fileMetaInfo.getFid(), fileMetaInfo.getDestPath());
            fileMetaInfo.setSliceFile(slice);
            fileMetaInfo.setMd5(computeMD5);
        }

        ByteBuf fileReconstuctAckFrame = Unpooled.buffer();
        fileReconstuctAckFrame.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        fileReconstuctAckFrame.writeInt(OptCodeEnums.FILE_DATA_TRANSPORT_FINISH_ACK.getValue());
        fileReconstuctAckFrame.writeCharSequence(opt.getContextId(), StandardCharsets.UTF_8);
        fileReconstuctAckFrame.writeCharSequence(opt.getOptId(), StandardCharsets.UTF_8);
        fileReconstuctAckFrame.writeCharSequence(opt.getFid(), StandardCharsets.UTF_8);
        fileReconstuctAckFrame.writeBoolean(retry);
        fileReconstuctAckFrame.writeLong(Crc32Utils.computeCrc32(fileReconstuctAckFrame.array()));
        fileReconstuctAckFrame.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
        log.info("[Receiver]send file reconstruct ack, frame:{}", fileReconstuctAckFrame);
        opt.getChannel().writeAndFlush(fileReconstuctAckFrame);
    }
}























