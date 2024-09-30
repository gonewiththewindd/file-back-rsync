package com.gone.file_backup.sender;

import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.SliceFile;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public interface Sender {

    void syncFileList(String ip, Integer port, List<FileMetaInfo> fileList);

    void syncFile(String ip, Integer port, FileMetaInfo fileMetaInfo, CountDownLatch countDownLatch);

    SliceFile fetchFileSliceInfo(String ip, int port, String fid);

    void sendReconstructFileBlocks(String ip, int port, FileMetaInfo modifiedFile, List<ReconstructFileBlock> reconstructFileBlocks);
}
