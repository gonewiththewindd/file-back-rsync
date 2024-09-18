package com.gone.file_backup.file;

import com.gone.file_backup.file.reconstruct.MatchedFileBlock;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

@Data
@Accessors(chain = true)
public class SearchFileBlock {

    private RollingChecksum lastChecksum;
    private MatchedFileBlock matchedFileBlock;

    public boolean isMatched() {
        return Objects.nonNull(matchedFileBlock);
    }
}
