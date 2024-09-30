package com.gone.file_backup.receiver;

import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.ReconstructFile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReceiverContext {

    public Map<String, Set<String>> syncIdToFileIdMap = new ConcurrentHashMap<>();
    public Map<String, FileMetaInfo> fileMetaInfoMap = new ConcurrentHashMap<>();
    public Map<String, ReconstructFile> reconstructMap = new ConcurrentHashMap<>();

}
