package com.gone.file_backup.controller;

import com.gone.file_backup.generator.DefaultGeneratorImpl;
import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.sender.DefaultSenderImpl;
import com.gone.file_backup.sender.SenderContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/backup/task")
public class BackTaskController {

    @Autowired
    private ScheduledExecutorService scheduledExecutorService;
    @Autowired
    DefaultSenderImpl sender;
    @Autowired
    SenderContext senderContext;

    @Data
    public static class NewTaskBO {
        String localDirectory;
        String remoteIp;
        int remotePort;
        String remoteDirectory;
    }

    @PostMapping("/newAndRun")
    public void newAndRun(@RequestBody NewTaskBO taskBO) {

        DefaultGeneratorImpl generator = new DefaultGeneratorImpl(
                taskBO.getRemoteIp(),
                taskBO.getRemotePort(),
                taskBO.getLocalDirectory(),
                taskBO.getRemoteDirectory(),
                sender
        );
        List<FileMetaInfo> fileMetaInfos = generator.generateFileList();
        sender.syncFileList(taskBO.getRemoteIp(), taskBO.getRemotePort(), fileMetaInfos);

        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                generator.detectFileListChange();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }, 100, 10000, TimeUnit.MILLISECONDS);
    }

    @GetMapping("/runtimeInfo")
    public Object runtimeInfo() {

        Map<String, RetryMsg> retryMap = senderContext.retryMap;
        Map<String, AckResult> ackMap = senderContext.ackMap;
        Map<String, Object> continuteMap = senderContext.continuteMap;

        return List.of(retryMap, ackMap, continuteMap);
    }

}
