package com.gone.file_backup.rsync.network.handler;

import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import com.gone.file_backup.rsync.network.opt.*;
import com.gone.file_backup.rsync.receiver.Receiver;
import com.gone.file_backup.rsync.sender.Sender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@ChannelHandler.Sharable
public class ChannelOperationHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Autowired
    private Receiver receiver;
    @Autowired
    private Sender sender;

    public ChannelOperationHandler(Receiver receiver, Sender sender) {
        this.receiver = receiver;
        this.sender = sender;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        CharSequence head = msg.getCharSequence(0, NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        if (!StringUtils.equalsIgnoreCase(NetworkConstants.FRAME_HEAD, head.toString())) {
            return;
        }
        OptBO opt = parseOpt(msg, ctx);
        if (Objects.isNull(opt)) {
            return;
        }
        switch (opt.getOptCode()) {
            case SEND_FILE_META_INFO:
                receiver.processMetaInfoOpt((MetaInfoOptBO) opt);
                break;
            case SEND_FILE_META_INFO_ACK:
                sender.receiveMetaInfoAck((MetaInfoAckOptBO) opt);
                break;
            case SEND_FILE_DATA:
                receiver.processFileDataOpt((FileDataOptBO) opt);
                break;
            case SEND_FILE_DATA_ACK:
                sender.processFileDataAck((FileDataAckOptBO) opt);
                break;
            case FULL_SYNC_FINISH:
                receiver.processFullSyncFinishOpt((FullSyncFinishOptBO) opt);
                break;
            case FULL_SYNC_FINISH_ACK:
                sender.processFileDataEndAck(opt);
                break;
        }
    }

    private OptBO parseOpt(ByteBuf buf, ChannelHandlerContext ctx) {

        int optCodeIndex = NetworkConstants.FRAME_HEAD.length();
        OptCodeEnums optCode = OptCodeEnums.of(buf.getInt(optCodeIndex));
        OptBO opt = null;
        switch (optCode) {
            case SEND_FILE_META_INFO -> {
                opt = MetaInfoOptBO.parse(buf);
            }
            case SEND_FILE_META_INFO_ACK -> {
                opt = MetaInfoAckOptBO.parse(buf);
            }
            case SEND_FILE_DATA -> {
                opt = FileDataOptBO.parse(buf);
            }
            case SEND_FILE_DATA_ACK -> {
                opt = FileDataAckOptBO.parse(buf);
            }
            case FULL_SYNC_FINISH -> {
                opt = FullSyncFinishOptBO.parse(buf);
            }
            case FULL_SYNC_FINISH_ACK -> {

            }
        }

        opt.setChannel(ctx.channel());

        return opt;
    }
}
