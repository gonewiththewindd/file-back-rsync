package com.gone.file_backup.sender;

import com.gone.file_backup.model.AckResult;
import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.network.channel.PooledChannelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SenderContext {

    public static final int BLOCK_SIZE = 1024 * 64;

    @Lazy
    @Autowired
    private PooledChannelManager pooledChannelManager;

    /**
     * 重试暂存
     */
    public Map<String, RetryMsg> retryMap = new ConcurrentHashMap<>();
    public Map<String, AckResult> ackMap = new ConcurrentHashMap<>();
    public Map<String, Object> continuteMap = new ConcurrentHashMap<>();

}
