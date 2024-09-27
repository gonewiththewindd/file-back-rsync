package com.gone.file_backup.rsync.network.channel;

import com.gone.file_backup.rsync.network.handler.ChannelOperationHandler;
import com.gone.file_backup.rsync.network.handler.SimpleChannelPoolHandler;
import com.gone.file_backup.rsync.sender.Sender;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Component
public class PooledChannelManager {

    @Autowired
    private ChannelOperationHandler channelOperationHandler;

    private Map<String, ChannelPool> channelPoolMap = new ConcurrentHashMap<>();

    public Future<Channel> getChannel(String ip, int port, Sender sender) {
        String key = ip.concat(String.valueOf(port));
        ChannelPool channelPool = channelPoolMap.computeIfAbsent(key, k -> getChannelPoolHandler(ip, port));
        return channelPool.acquire();
    }

    private ChannelPool getChannelPoolHandler(String ip, int port) {
        Bootstrap bootstrap = getBootstrap(ip, port);
        return new SimpleChannelPool(bootstrap, new SimpleChannelPoolHandler(channelOperationHandler));
    }

    public void executeOnChannelAcquire(String ip, int port, Function<Channel, Object> callback) {
        String key = ip.concat(String.valueOf(port));
        ChannelPool channelPool = channelPoolMap.computeIfAbsent(key, k -> getChannelPoolHandler(ip, port));
        Future<Channel> channelFuture = channelPool.acquire();
        channelFuture.addListener((FutureListener<Channel>) future -> {
            if (future.isSuccess()) {
                try {
                    callback.apply(future.get());
                } finally {
                    channelPool.release(future.get());
                }
            } else {
                log.error("failed to acquire channel", future.cause());
                throw new RuntimeException(future.cause().getCause());
            }
        });
    }

    public void executeOnChannelAcquireSync(String ip, int port, Function<Channel, Object> callback) {
        String key = ip.concat(String.valueOf(port));
        ChannelPool channelPool = channelPoolMap.computeIfAbsent(key, k -> getChannelPoolHandler(ip, port));
        try {
            Future<Channel> future = channelPool.acquire().sync();
            if (future.isSuccess()) {
                try {
                    callback.apply(future.get());
                } finally {
                    channelPool.release(future.get());
                }
            } else {
                log.error("failed to acquire channel", future.cause());
                throw new RuntimeException(future.cause().getCause());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private Bootstrap getBootstrap(String ip, int port) {
        return new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(ip, port);
    }

}
