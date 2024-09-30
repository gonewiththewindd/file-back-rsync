package com.gone.file_backup.file.reconstruct;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ToString(callSuper = true)
public class ModifyByteSerial extends ReconstructFileBlock {
    private byte[] content; // TODO 考虑使用索引下标范围+文件路径的方式 代替直接缓存修改内容
}
