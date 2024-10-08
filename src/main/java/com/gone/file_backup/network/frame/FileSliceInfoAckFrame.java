package com.gone.file_backup.network.frame;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.constants.NetworkConstants;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FileSliceInfoAckFrame extends OperationBaseFrame {

    private SliceFile sliceFile;

    public FileSliceInfoAckFrame() {

    }

    public static FileSliceInfoAckFrame parse(ByteBuf buf) {

        FileSliceInfoAckFrame fileSliceInfoAckFrame = new FileSliceInfoAckFrame();

        CharSequence head = buf.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = buf.readInt();
        CharSequence contextId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        CharSequence otpId = buf.readCharSequence(32, StandardCharsets.UTF_8);
        int length = buf.readInt();
        if (length > 0) {
            CharSequence json = buf.readCharSequence(length, StandardCharsets.UTF_8);
            fileSliceInfoAckFrame.setSliceFile(JSON.parseObject(json.toString(), SliceFile.class));
        }
        long crc = buf.readLong();
        CharSequence tail = buf.readCharSequence(NetworkConstants.FRAME_TAIL.length(), StandardCharsets.UTF_8);

        fileSliceInfoAckFrame.setOptCode(optCode);
        fileSliceInfoAckFrame.setContextId(contextId.toString());
        fileSliceInfoAckFrame.setOptId(otpId.toString());
        fileSliceInfoAckFrame.setCrc(crc);

        return fileSliceInfoAckFrame;
    }

}
