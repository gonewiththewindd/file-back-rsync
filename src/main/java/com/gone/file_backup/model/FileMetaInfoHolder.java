package com.gone.file_backup.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.concurrent.CountDownLatch;

@Data
@Accessors(chain = true)
public class FileMetaInfoHolder {

    private FileMetaInfo fileMetaInfo;
    private CountDownLatch countDownLatch;

}
