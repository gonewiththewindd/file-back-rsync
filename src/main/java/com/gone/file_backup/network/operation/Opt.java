package com.gone.file_backup.network.operation;

import com.gone.file_backup.network.frame.OperationBaseFrame;

public interface Opt<T extends OperationBaseFrame> {

    void process(T opt);

}
