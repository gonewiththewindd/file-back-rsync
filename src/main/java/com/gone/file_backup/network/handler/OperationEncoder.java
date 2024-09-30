package com.gone.file_backup.network.handler;

import com.alibaba.fastjson2.JSON;
import com.gone.file_backup.constants.DataTypeConstants;
import com.gone.file_backup.constants.NetworkConstants;
import com.gone.file_backup.network.frame.OperationBaseFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class OperationEncoder extends MessageToByteEncoder<OperationBaseFrame> {

    public static final Map<String, Byte> classTypeMap = new HashMap() {{
        put(boolean.class.getName(), DataTypeConstants.BOOLEAN);
        put(Boolean.class.getName(), DataTypeConstants.BOOLEAN);

        put(byte.class.getName(), DataTypeConstants.BYTE);
        put(Byte.class.getName(), DataTypeConstants.BYTE);

        put(char.class.getName(), DataTypeConstants.CHAR);
        put(Character.class.getName(), DataTypeConstants.CHAR);

        put(short.class.getName(), DataTypeConstants.SHORT);
        put(Short.class.getName(), DataTypeConstants.SHORT);

        put(int.class.getName(), DataTypeConstants.INT);
        put(Integer.class.getName(), DataTypeConstants.INT);

        put(long.class.getName(), DataTypeConstants.LONG);
        put(Long.class.getName(), DataTypeConstants.LONG);

        put(float.class.getName(), DataTypeConstants.FLOAT);
        put(Float.class.getName(), DataTypeConstants.FLOAT);

        put(double.class.getName(), DataTypeConstants.DOUBLE);
        put(Double.class.getName(), DataTypeConstants.DOUBLE);

        put(String.class.getName(), DataTypeConstants.STRING);

        put(byte[].class.getName(), DataTypeConstants.BYTE_ARRAY);
    }};

    @Override
    protected void encode(ChannelHandlerContext ctx, OperationBaseFrame msg, ByteBuf out) {

        out.writeCharSequence(NetworkConstants.FRAME_HEAD, StandardCharsets.UTF_8);
        out.writeInt(msg.getOptCode());

        Field[] declaredFields = msg.getClass().getDeclaredFields();
        Field[] superDeclaredFields = msg.getClass().getSuperclass().getDeclaredFields();
        int propertyLength = declaredFields.length + superDeclaredFields.length - 1;
        out.writeInt(propertyLength);
        for (Field declaredField : declaredFields) {
            writeField(msg, declaredField, out);
        }
        for (Field declaredField : superDeclaredFields) {
            if (declaredField.getName().equalsIgnoreCase("channel")) {
                continue;
            }
            writeField(msg, declaredField, out);
        }

        out.writeCharSequence(NetworkConstants.FRAME_TAIL, StandardCharsets.UTF_8);
        log.info("[Encoder]encode msg:{} to frame:{}", msg, out);
    }

    private void writeField(OperationBaseFrame instance, Field declaredField, ByteBuf out) {

        try {
            declaredField.setAccessible(true);
            Object value = declaredField.get(instance);

            if (Objects.isNull(value)) {
                return;
            }

            String name = declaredField.getName();
            int length = name.getBytes(StandardCharsets.UTF_8).length;
            out.writeInt(length);
            out.writeCharSequence(name, StandardCharsets.UTF_8);

            Byte b = convertClassType(declaredField.getType());
            out.writeInt(b);

            switch (b) {
                case DataTypeConstants.BOOLEAN:
                    out.writeBoolean((boolean) value);
                    break;
                case DataTypeConstants.BYTE:
                    out.writeByte((byte) value);
                    break;
                case DataTypeConstants.CHAR:
                    out.writeChar((char) value);
                    break;
                case DataTypeConstants.SHORT:
                    out.writeShort((short) value);
                    break;
                case DataTypeConstants.INT:
                    out.writeInt((int) value);
                    break;
                case DataTypeConstants.LONG:
                    out.writeLong((long) value);
                    break;
                case DataTypeConstants.FLOAT:
                    out.writeFloat((float) value);
                    break;
                case DataTypeConstants.DOUBLE:
                    out.writeDouble((double) value);
                    break;
                case DataTypeConstants.STRING:
                    out.writeInt(((String) value).getBytes(StandardCharsets.UTF_8).length);
                    out.writeCharSequence((String) value, StandardCharsets.UTF_8);
                    break;
                case DataTypeConstants.BYTE_ARRAY:
                    byte[] bytes = (byte[]) value;
                    out.writeInt(bytes.length);
                    out.writeBytes(bytes);
                    break;
                case DataTypeConstants.OBJECT_JSON:
                    String objectType = declaredField.getGenericType() instanceof ParameterizedType ?
                            ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments()[0].getTypeName() :
                            declaredField.getType().getName();
                    int typeLength = objectType.getBytes(StandardCharsets.UTF_8).length;
                    out.writeInt(typeLength);
                    out.writeCharSequence(objectType, StandardCharsets.UTF_8);
                    String json = JSON.toJSONString(value);
                    int jsonLength = json.getBytes(StandardCharsets.UTF_8).length;
                    out.writeInt(jsonLength);
                    out.writeCharSequence(json, StandardCharsets.UTF_8);
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Byte convertClassType(Class<?> type) {
        Byte dataType = classTypeMap.get(type.getName());
        if (Objects.isNull(dataType)) {
            dataType = DataTypeConstants.OBJECT_JSON;
        }
        return dataType;
    }
}
