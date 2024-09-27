package com.gone.file_backup.sender;

import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.RetryMsg;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.network.frame.*;

import java.util.List;
import java.util.Map;

public interface Sender {

    void syncFileList(String ip, Integer port, List<FileMetaInfo> fileList);

    void receiveMetaInfoAck(MetaInfoAckFrame opt);

    void processFileDataPartAck(FileDataPartAckFrame opt);

    void processFileDataFinishAck(FileDataFinishAckFrame opt);

    void processFullSyncFinishAck(OperationBaseFrame opt);

    Map<String, RetryMsg> retryMap();

    SliceFile fetchFileSliceInfo(String ip, int port, String fid);

    void processFileSliceInfoAck(FileSliceInfoAckFrame opt);

    void sendReconstructFileBlocks(String ip, int port, FileMetaInfo modifiedFile, List<ReconstructFileBlock> reconstructFileBlocks);

    void processFileReconstructListAck(OperationBaseFrame opt);
}
