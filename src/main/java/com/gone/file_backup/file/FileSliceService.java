package com.gone.file_backup.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FileSliceService {

    @Value("${com.bdc.file.backup.slice.block-size:4096}")
    private Integer BLOCK_SIZE;



}
