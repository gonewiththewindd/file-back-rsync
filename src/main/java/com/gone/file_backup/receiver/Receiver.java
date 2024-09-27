package com.gone.file_backup.receiver;

import com.gone.file_backup.network.frame.*;

public interface Receiver {

    void processMetaInfoOpt(MetaInfoFrame opt);

    void processFileDataOpt(FileDataPartFrame opt);

    void processFileDataFinishOpt(FileDataFinishFrame opt);

    void processFullSyncFinishOpt(FullSyncFinishFrame opt);

    void executeOpt(Opt opt, OperationBaseFrame optBO);

    void processFileSliceInfo(FileSliceInfoFrame opt);

    void processReconstructList(FileReconstructListFrame opt);

    void processFileReconstructListTransportFinish(FileReconstructFrame opt);
}
