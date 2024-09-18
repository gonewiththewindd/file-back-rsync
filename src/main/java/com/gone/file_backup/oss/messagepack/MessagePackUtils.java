package com.gone.file_backup.oss.messagepack;

import org.msgpack.core.ExtensionTypeHeader;
import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MessagePackUtils {


    public static final String messagePackageToJson(String messagePackageFilePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(messagePackageFilePath))) {
            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(inputStream);
            while (unpacker.hasNext()) {
                readRecursively(unpacker);
            }
            System.out.println();
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    private static Object readRecursively(MessageUnpacker unpacker) throws IOException {
        MessageFormat format = unpacker.getNextFormat();
        ValueType type = format.getValueType();
        int length;
        ExtensionTypeHeader extension;
        switch (type) {
            case NIL:
                unpacker.unpackNil();
                break;
            case BOOLEAN:
                boolean b = unpacker.unpackBoolean();
                printValue("boolean", b);
                break;
            case INTEGER:
                switch (format) {
                    case UINT64:
                        printValue("UINT64",unpacker.unpackBigInteger());
                        break;
                    case INT64:
                    case UINT32:
                        printValue("long", unpacker.unpackLong());
                        break;
                    default:
                        printValue("int", unpacker.unpackInt());
                        break;
                }
                break;
            case FLOAT:
                printValue("float", unpacker.unpackDouble());
                break;
            case STRING:
                printValue("string", unpacker.unpackString());
                break;
            case BINARY:
                length = unpacker.unpackBinaryHeader();
                byte[] dst = new byte[length];
                unpacker.readPayload(dst);

                MessageUnpacker messageUnpacker = MessagePack.newDefaultUnpacker(dst);
                while (messageUnpacker.hasNext()){
                    readRecursively(messageUnpacker);
                }
//                printValue("binary", new String(dst, StandardCharsets.UTF_8));
                break;
            case ARRAY:
                length = unpacker.unpackArrayHeader();
                for (int i = 0; i < length; i++) {
                    readRecursively(unpacker);
//                    printValue(o);
                }
                break;
            case MAP:
                length = unpacker.unpackMapHeader();
                for (int i = 0; i < length; i++) {
                    Object key = readRecursively(unpacker);// key
                    Object value = readRecursively(unpacker);// value
                    System.out.print(String.format("%s:%s", key, value));
                }
                break;
            case EXTENSION:
                extension = unpacker.unpackExtensionTypeHeader();
                unpacker.readPayload(new byte[extension.getLength()]);
                break;
        }

        return null;
    }

    public static int cnt = 0;

    private static void printValue(String type, Object val) {
        System.out.println(String.format("loop %s: type:%s, value:%s", ++cnt, type, val.toString()));
    }


    public static void main(String[] args) {
        messagePackageToJson("C:\\minio\\budongchan\\不动产数据安全管控及监测项目可研报告（细化功能点）0326-LWB.docx\\xl.meta");
        System.out.println();
        System.out.println(cnt);
    }


}
