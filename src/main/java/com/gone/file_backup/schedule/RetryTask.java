package com.gone.file_backup.schedule;

import com.gone.file_backup.model.retry.BufRetryMsg;
import com.gone.file_backup.model.retry.FrameRetryMsg;
import com.gone.file_backup.model.retry.RetryMsg;
import com.gone.file_backup.network.channel.PooledChannelManager;
import com.gone.file_backup.sender.Sender;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RetryTask implements Runnable {

    public static final int ACK_TIMEOUT_IN_MILLISECONDS = 5000;

    private Sender sender;
    private PooledChannelManager pooledChannelManager;

    public RetryTask(Sender sender, PooledChannelManager pooledChannelManager) {
        this.sender = sender;
        this.pooledChannelManager = pooledChannelManager;
    }

    @Override
    public void run() {
        try {
            Map<String, RetryMsg> retryMap = sender.retryMap();
            long current = System.currentTimeMillis();
            List<Map.Entry<String, RetryMsg>> timeoutList = retryMap.entrySet()
                    .stream()
                    .filter(entry -> current - entry.getValue().getLastSendAt() > ACK_TIMEOUT_IN_MILLISECONDS)
                    .collect(Collectors.toList());
            timeoutList.forEach(entry -> {
                RetryMsg msg = entry.getValue();
                log.info("retry opt in retryTask, retryMsg:{}", msg);
                pooledChannelManager.executeOnChannelAcquire(msg.getIp(), msg.getPort(), channel -> {
                    if (!retryMap.containsKey(entry.getKey())) {
                        return null;
                    }
                    Object retryMsg = msg instanceof BufRetryMsg ? ((BufRetryMsg) msg).getBuf().copy() : ((FrameRetryMsg) msg).getFrame();
                    ChannelFuture channelFuture = channel.writeAndFlush(retryMsg);
                    channelFuture.addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            msg.getRetryCount().incrementAndGet();
                            msg.setLastSendAt(System.currentTimeMillis());
                        } else {
                            log.error("retry failed! retry msg:{}, cause:{}", msg, future.cause());
                        }
                    });
                    return null;
                });
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
