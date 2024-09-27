package com.gone.file_backup.rsync.sender;

import com.gone.file_backup.rsync.model.FileMetaInfo;
import com.gone.file_backup.rsync.model.RetryMsg;
import com.gone.file_backup.rsync.network.opt.FileDataAckOptBO;
import com.gone.file_backup.rsync.network.opt.MetaInfoAckOptBO;
import com.gone.file_backup.rsync.network.opt.OptBO;

import java.util.List;
import java.util.Map;

public interface Sender {

    void syncFileList(String ip, Integer port, List<FileMetaInfo> fileList);

    void receiveMetaInfoAck(MetaInfoAckOptBO opt);

    void fetchFileInfo(String fid);

    void sendReconstructFile();

    void processFileDataAck(FileDataAckOptBO opt);

    void processFileDataEndAck(OptBO opt);

    Map<String, RetryMsg> retryMap();
}
