package com.gone.file_backup.network.handler;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.constants.DataTypeConstants;
import com.gone.file_backup.constants.NetworkConstants;
import com.gone.file_backup.network.OptCodeEnums;
import com.gone.file_backup.network.frame.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class OperationDecoder extends ByteToMessageDecoder {

    private static Map<String, Class> optTypeBusinessClassMap = new HashMap<>() {{
        put(String.valueOf(OptCodeEnums.FILE_META_INFO.getValue()), MetaInfoFrame.class);
        put(String.valueOf(OptCodeEnums.FILE_META_INFO_ACK.getValue()), MetaInfoAckFrame.class);

        put(String.valueOf(OptCodeEnums.FILE_DATA_PART.getValue()), FileDataPartFrame.class);
        put(String.valueOf(OptCodeEnums.FILE_DATA_PART_ACK.getValue()), FileDataPartAckFrame.class);

        put(String.valueOf(OptCodeEnums.FILE_DATA_FINISH.getValue()), FileDataFinishFrame.class);
        put(String.valueOf(OptCodeEnums.FILE_DATA_FINISH_ACK.getValue()), FileDataFinishAckFrame.class);

        put(String.valueOf(OptCodeEnums.FILE_RECONSTRUCT_BLOCKS.getValue()), FileReconstructBlocksFrame.class);
        put(String.valueOf(OptCodeEnums.FILE_RECONSTRUCT_BLOCKS_ACK.getValue()), FileReconstructBlocksAckFrame.class);

        put(String.valueOf(OptCodeEnums.FILE_RECONSTRUCT.getValue()), FileReconstructFrame.class);
        put(String.valueOf(OptCodeEnums.FILE_RECONSTRUCT_ACK.getValue()), FileReconstructBlocksAckFrame.class);
    }};

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object object = decodeBuf(ctx, in);
        if (Objects.nonNull(object)) {
            out.add(object);
        }
    }

    public static Object decodeBuf(ChannelHandlerContext ctx, ByteBuf in) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        CharSequence head = in.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        if (!NetworkConstants.FRAME_HEAD.equalsIgnoreCase(head.toString())) {
            log.error("[Decoder]throw away illegal frame that start with:{}", head);
            return null;
        }
//        in.readCharSequence(NetworkConstants.FRAME_HEAD.length(), StandardCharsets.UTF_8);
        int optCode = in.readInt();

        int propertyLength = in.readInt();
        Map<String, Object> properties = new HashMap<>();
        if (propertyLength > 0) {
            for (int i = 0; i < propertyLength; i++) {
                int properNameLength = in.readInt();
                CharSequence propertyName = in.readCharSequence(properNameLength, StandardCharsets.UTF_8);
                int dataType = in.readInt();
                Object value = extractValue(in, dataType);
                properties.put(propertyName.toString(), value);
            }
        }

        Class businessClass = optTypeBusinessClassMap.get(String.valueOf(optCode));
        Object instance = businessClass.getConstructor().newInstance();
        Field[] declaredFields = businessClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Object value = properties.get(declaredField.getName());
            if (Objects.nonNull(value)) {
                declaredField.setAccessible(true);
                declaredField.set(instance, value);
            }
        }

        for (Field declaredField : businessClass.getSuperclass().getDeclaredFields()) {
            if (declaredField.getName().equalsIgnoreCase("channel")) {
                declaredField.setAccessible(true);
                declaredField.set(instance, ctx.channel());
                continue;
            }
            Object value = properties.get(declaredField.getName());
            if (Objects.nonNull(value)) {
                declaredField.setAccessible(true);
                declaredField.set(instance, value);
            }
        }

        return instance;
    }

    private static Object extractValue(ByteBuf buf, int dataType) {

        Object value;
        switch (dataType) {
            case DataTypeConstants.BOOLEAN:
                value = buf.readBoolean();
                break;
            case DataTypeConstants.BYTE:
                value = buf.readByte();
                break;
            case DataTypeConstants.CHAR:
                value = buf.readChar();
                break;
            case DataTypeConstants.SHORT:
                value = buf.readShort();
                break;
            case DataTypeConstants.INT:
                value = buf.readInt();
                break;
            case DataTypeConstants.LONG:
                value = buf.readLong();
                break;
            case DataTypeConstants.FLOAT:
                value = buf.readFloat();
                break;
            case DataTypeConstants.DOUBLE:
                value = buf.readDouble();
                break;
            case DataTypeConstants.STRING:
                int length = buf.readInt();
                value = buf.readCharSequence(length, StandardCharsets.UTF_8).toString();
                break;
            case DataTypeConstants.BYTE_ARRAY:
                int byteLength = buf.readInt();
                byte[] bytes = new byte[byteLength];
                buf.readBytes(bytes);
                value = bytes;
                break;
            case DataTypeConstants.OBJECT_JSON:
                try {
                    int classTypeLength = buf.readInt();
                    CharSequence classType = buf.readCharSequence(classTypeLength, StandardCharsets.UTF_8);
                    int jsonLength = buf.readInt();
                    String json = buf.readCharSequence(jsonLength, StandardCharsets.UTF_8).toString();
                    if (json.startsWith("[")) {
                        value = JSON.parseArray(json, Class.forName(classType.toString()));
                    } else if (json.startsWith("{")) {
                        value = JSON.parseObject(json, Class.forName(classType.toString()));
                    } else {
                        throw new IllegalArgumentException("invalid JSON object");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new IllegalArgumentException("invalid JSON object");
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }

        return value;
    }

}
