package com.gone.file_backup.network.frame;

public interface Opt<T extends OperationBaseFrame> {

    void process(T optBO);

}
