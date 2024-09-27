package com.gone.file_backup.rsync.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;

public class SimpleChannelPoolHandler extends AbstractChannelPoolHandler {

    private ChannelOperationHandler clientOptChannelHandler;

    public SimpleChannelPoolHandler(ChannelOperationHandler clientOptChannelHandler) {
        this.clientOptChannelHandler = clientOptChannelHandler;
    }

    @Override
    public void channelCreated(Channel ch) {
        ch.pipeline().addLast(clientOptChannelHandler);
    }
}
