package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.utils.Crc32Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class OperationBaseFrame {

    protected String head = NetworkConstants.FRAME_HEAD;
    protected OptCodeEnums optCode; // 操作码
    protected String contextId; // 上下文标识，用于维持上下文一致
    protected String optId; // 操作标识，用于确认
    protected long crc; // 循环校验和
    protected String tail = NetworkConstants.FRAME_TAIL;

    // TODO 执行结果

    protected transient Channel channel;

    public ByteBuf ack() {
        if (this.optCode.getValue() < 0) {
            throw new IllegalArgumentException("Invalid opt code: " + this.optCode);
        }
        ByteBuf ack = Unpooled.buffer();
        ack.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        ack.writeInt(OptCodeEnums.of(-this.optCode.getValue()).getValue());
        ack.writeCharSequence(this.getContextId(), StandardCharsets.UTF_8);
        ack.writeCharSequence(this.getOptId(), StandardCharsets.UTF_8);
        ack.writeLong(Crc32Utils.computeCrc32(ack.array()));
        ack.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
        return ack;
    }

    public static OperationBaseFrame parse(ByteBuf buf) {

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence oid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence sid = buf.readCharSequence(32, StandardCharsets.UTF_8);
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        OperationBaseFrame frame = new OperationBaseFrame();
        frame.setOptCode(OptCodeEnums.of(optCode));
        frame.setContextId(sid.toString());
        frame.setOptId(oid.toString());
        frame.setCrc(crc);

        return frame;
    }
}
