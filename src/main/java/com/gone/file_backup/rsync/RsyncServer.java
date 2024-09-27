package com.gone.file_backup.rsync;

import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.handler.ChannelOperationHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class RsyncServer implements Runnable {

    private ChannelOperationHandler optChannelHandler;

    public RsyncServer(ChannelOperationHandler optChannelHandler) {
        this.optChannelHandler = optChannelHandler;
    }

    @Override
    public void run() {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(new NioEventLoopGroup());
        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(
                                128 * 1024,
                                false,
                                true,
//                        Unpooled.wrappedBuffer(NetworkConstants.FRAME_HEAD.getBytes(StandardCharsets.UTF_8)),
                                Unpooled.wrappedBuffer(NetworkConstants.FRAME_TAIL.getBytes(StandardCharsets.UTF_8)))
                );
//                ch.pipeline().addLast(new Base64Decoder());
//
//                ch.pipeline().addLast(new Base64Encoder());
                ch.pipeline().addLast(optChannelHandler);
            }
        }).childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channelFuture = serverBootstrap.bind(8888);
        log.info("netty rsync server start on port:{}", 8888);
        try {
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
