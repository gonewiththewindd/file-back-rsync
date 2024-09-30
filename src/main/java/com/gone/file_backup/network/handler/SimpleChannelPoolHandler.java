package com.gone.file_backup.network.handler;

import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.nio.charset.StandardCharsets;

public class SimpleChannelPoolHandler extends AbstractChannelPoolHandler {

    private ChannelOperationHandlerV2 clientOptChannelHandler;

    public SimpleChannelPoolHandler(ChannelOperationHandlerV2 clientOptChannelHandler) {
        this.clientOptChannelHandler = clientOptChannelHandler;
    }

    @Override
    public void channelCreated(Channel ch) {
        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(
                        128 * 1024,
                        true,
                        true,
                        Unpooled.wrappedBuffer(NetworkConstants.FRAME_TAIL.getBytes(StandardCharsets.UTF_8)))
        );
        ch.pipeline().addLast(new OperationDecoder());
        ch.pipeline().addLast(new OperationEncoder());
        ch.pipeline().addLast(clientOptChannelHandler);
    }
}
