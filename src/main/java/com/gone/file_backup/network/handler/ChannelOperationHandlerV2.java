package com.gone.file_backup.network.handler;

import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.OperationBaseFrame;
import com.gone.file_backup.network.operation.Opt;
import com.gone.file_backup.sender.SenderContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@ChannelHandler.Sharable
public class ChannelOperationHandlerV2 extends SimpleChannelInboundHandler<OperationBaseFrame> {

    @Autowired
    private Map<String, Opt> operationMap;
    @Autowired
    private SenderContext senderContext;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OperationBaseFrame frame) {

        // TODO crc校验失败，丢弃包，依赖远程重试机制
        if (frame.getOptCode() < 0) {
            senderContext.retryMap.remove(frame.getOptId());
        }
        OptCodeEnums optCodeEnums = OptCodeEnums.of(frame.getOptCode());
        Opt opt = operationMap.get(optCodeEnums.getBeanName());
        if (Objects.nonNull(opt)) {
            opt.process(frame);
        } else {
            log.error("[]operation impl not exist. opt:{}", frame);
        }
    }

}
