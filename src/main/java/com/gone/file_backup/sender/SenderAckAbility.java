package com.gone.file_backup.sender;

import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.network.frame.*;

import java.util.Map;

/**
 * sender应答相关接口
 */
public interface SenderAckAbility {

    void receiveMetaInfoAck(MetaInfoAckFrame opt);

    void processFileDataPartAck(FileDataPartAckFrame opt);

    void processFileDataFinishAck(FileDataFinishAckFrame opt);

    Map<String, RetryMsg> retryMap();

    void processFileSliceInfoAck(FileSliceInfoAckFrame opt);

    void processFileReconstructListAck(OperationBaseFrame opt);
}
