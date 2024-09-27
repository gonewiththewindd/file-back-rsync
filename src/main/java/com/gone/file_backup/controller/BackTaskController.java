package com.gone.file_backup.controller;

import com.gone.file_backup.rsync.generator.DefaultGeneratorImpl;
import com.gone.file_backup.rsync.model.FileMetaInfo;
import com.gone.file_backup.rsync.sender.Sender;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/backup/task")
public class BackTaskController {

    @Autowired
    Sender sender;

    @Data
    public static class NewTaskBO {
        String localDirectory;
        String remoteIp;
        int remotePort;
        String remoteDirectory;
    }

    @PostMapping("/newAndRun")
    public void newAndRun(@RequestBody NewTaskBO taskBO) {

        DefaultGeneratorImpl generator = new DefaultGeneratorImpl(taskBO.getLocalDirectory(), taskBO.getRemoteDirectory());
        List<FileMetaInfo> fileMetaInfos = generator.generateFileList();

        sender.syncFileList(taskBO.getRemoteIp(), taskBO.getRemotePort(), fileMetaInfos);

        generator.detectFileListChange();
    }

}
