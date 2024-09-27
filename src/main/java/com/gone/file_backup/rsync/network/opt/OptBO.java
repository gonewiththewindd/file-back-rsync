package com.gone.file_backup.rsync.network.opt;

import com.gone.file_backup.rsync.network.NetworkConstants;
import com.gone.file_backup.rsync.network.OptCodeEnums;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OptBO {

    protected String head = NetworkConstants.FRAME_HEAD;
    protected OptCodeEnums optCode; // 操作码
    protected String contextId; // 上下文标识，用于维持上下文一致
    protected String optId; // 操作标识，用于确认
    protected long crc; // 循环校验和
    protected String tail = NetworkConstants.FRAME_TAIL;

    // TODO 执行结果

    protected transient Channel channel;
}
