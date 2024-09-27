package com.gone.file_backup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.concurrent.CountDownLatch;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class AckResult {

    private Object result;
    private CountDownLatch latch;

    public static AckResult of(CountDownLatch latch) {
        return new AckResult().setLatch(latch);
    }

}
