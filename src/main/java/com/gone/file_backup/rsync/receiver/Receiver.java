package com.gone.file_backup.rsync.receiver;

import com.gone.file_backup.rsync.network.opt.*;

public interface Receiver {

    void processMetaInfoOpt(MetaInfoOptBO opt);

    void processFileDataOpt(FileDataOptBO opt);

    void processFullSyncFinishOpt(FullSyncFinishOptBO opt);

    void executeOpt(Opt opt, OptBO optBO);
}
