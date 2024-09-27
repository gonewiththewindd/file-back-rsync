package com.gone.file_backup.rsync.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ContinueContext {

    private String ip;
    private int port;
    private Object context;

}
