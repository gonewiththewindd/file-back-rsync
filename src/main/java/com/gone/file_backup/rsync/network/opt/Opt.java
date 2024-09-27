package com.gone.file_backup.rsync.network.opt;

public interface Opt<T extends OptBO> {

    void process(T optBO);

}
