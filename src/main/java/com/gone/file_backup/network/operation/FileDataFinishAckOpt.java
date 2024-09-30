package com.gone.file_backup.network.operation;

import com.gone.file_backup.model.ContinueContext;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.FileMetaInfoHolder;
import com.gone.file_backup.network.frame.FileDataFinishAckFrame;
import com.gone.file_backup.sender.Sender;
import com.gone.file_backup.sender.SenderContext;
import com.gone.file_backup.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

@Slf4j
@Component
public class FileDataFinishAckOpt extends AbstractOpt<FileDataFinishAckFrame> {

    @Autowired
    private Sender sender;
    @Autowired
    private SenderContext senderContext;

    @Override
    public void process(FileDataFinishAckFrame opt) {

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
            sender.syncFile(ip, port, holder.getFileMetaInfo(), holder.getCountDownLatch());
        } else {
            // 清楚文件缓存信息
            log.info("[Sender]file data transport finish, clear file meta info cache...fid:{}, contextId:{}", fid, opt.getContextId());
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
}
