package com.gone.file_backup.network.operation;

import com.gone.file_backup.network.frame.OperationBaseFrame;
import com.gone.file_backup.receiver.Receiver;
import com.gone.file_backup.sender.Sender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractOpt<T extends OperationBaseFrame> implements Opt<T> {

    @Autowired
    protected Sender sender;
    @Autowired
    protected Receiver receiver;


}
