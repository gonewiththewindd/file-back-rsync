package com.gone.file_backup.network.handler;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.*;
import com.gone.file_backup.receiver.Receiver;
import com.gone.file_backup.sender.Sender;
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
        OperationBaseFrame opt = parseOpt(msg, ctx);
        if (Objects.isNull(opt)) {
            return;
        }
        // TODO crc校验失败，丢弃包，依赖远程重试机制

        switch (opt.getOptCode()) {
            case FILE_META_INFO:
                receiver.processMetaInfoOpt((MetaInfoFrame) opt);
                break;
            case FILE_META_INFO_ACK:
                sender.receiveMetaInfoAck((MetaInfoAckFrame) opt);
                break;
            case FILE_DATA_PART_TRANSPORT:
                receiver.processFileDataOpt((FileDataPartFrame) opt);
                break;
            case FILE_DATA_PART_TRANSPORT_ACK:
                sender.processFileDataPartAck((FileDataPartAckFrame) opt);
                break;
            case FILE_DATA_TRANSPORT_FINISH:
                receiver.processFileDataFinishOpt((FileDataFinishFrame) opt);
                break;
            case FILE_DATA_TRANSPORT_FINISH_ACK:
                sender.processFileDataFinishAck((FileDataFinishAckFrame) opt);
                break;
            case FULL_SYNC_FINISH:
                receiver.processFullSyncFinishOpt((FullSyncFinishFrame) opt);
                break;
            case FULL_SYNC_FINISH_ACK:
                sender.processFullSyncFinishAck(opt);
                break;
            case FILE_SLICE_INFO:
                receiver.processFileSliceInfo((FileSliceInfoFrame) opt);
                break;
            case FILE_SLICE_INFO_ACK:
                sender.processFileSliceInfoAck((FileSliceInfoAckFrame) opt);
                break;
            case FILE_RECONSTRUCT_LIST_TRANSPORT:
                receiver.processReconstructList((FileReconstructListFrame) opt);
                break;
            case FILE_RECONSTRUCT_LIST_TRANSPORT_ACK:
                sender.processFileReconstructListAck(opt);
                break;
            case FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH:
                receiver.processFileReconstructListTransportFinish((FileReconstructFrame) opt);
                break;
            case FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH_ACK:
                // ignore
                break;
        }
    }

    private OperationBaseFrame parseOpt(ByteBuf buf, ChannelHandlerContext ctx) {

        int optCodeIndex = NetworkConstants.FRAME_HEAD.length();
        OptCodeEnums optCode = OptCodeEnums.of(buf.getInt(optCodeIndex));
        OperationBaseFrame opt = null;
        switch (optCode) {
            case FILE_META_INFO -> {
                opt = MetaInfoFrame.parse(buf);
            }
            case FILE_META_INFO_ACK -> {
                opt = MetaInfoAckFrame.parse(buf);
            }
            case FILE_DATA_PART_TRANSPORT -> {
                opt = FileDataPartFrame.parse(buf);
            }
            case FILE_DATA_PART_TRANSPORT_ACK -> {
                opt = FileDataPartAckFrame.parse(buf);
            }
            case FILE_DATA_TRANSPORT_FINISH -> {
                opt = FileDataFinishFrame.parse(buf);
            }
            case FILE_DATA_TRANSPORT_FINISH_ACK -> {
                opt = FileDataFinishAckFrame.parse(buf);
            }
/*            case FULL_SYNC_FINISH -> {
                opt = FullSyncFinishFrame.parse(buf);
            }
            case FULL_SYNC_FINISH_ACK -> {

            }*/
            case FILE_SLICE_INFO -> {
                opt = FileSliceInfoFrame.parse(buf);
            }
            case FILE_SLICE_INFO_ACK -> {
                opt = FileSliceInfoAckFrame.parse(buf);
            }
            case FILE_RECONSTRUCT_LIST_TRANSPORT -> {
                opt = FileReconstructListFrame.parse(buf);
            }
            case FILE_RECONSTRUCT_LIST_TRANSPORT_ACK -> {
                opt = OperationBaseFrame.parse(buf);
            }
            case FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH -> {
                opt = FileReconstructFrame.parse(buf);
            }
            case FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH_ACK -> {
                opt = OperationBaseFrame.parse(buf);
            }
        }

        opt.setChannel(ctx.channel());

        return opt;
    }
}
