package com.gone.file_backup;

import com.gone.file_backup.network.channel.PooledChannelManager;
import com.gone.file_backup.network.handler.ChannelOperationHandlerV2;
import com.gone.file_backup.schedule.RetryTask;
import com.gone.file_backup.sender.Sender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class OssFileMonitorApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext ctx = SpringApplication.run(OssFileMonitorApplication.class, args);
        ChannelOperationHandlerV2 optChannelHandler = ctx.getBean(ChannelOperationHandlerV2.class);
        new Thread(new RsyncServer(optChannelHandler)).start();

        Sender sender = ctx.getBean(Sender.class);
        PooledChannelManager pooledChannelManager = ctx.getBean(PooledChannelManager.class);

        ScheduledExecutorService scheduledExecutorService = ctx.getBean(ScheduledExecutorService.class);
        scheduledExecutorService.scheduleWithFixedDelay(new RetryTask(sender, pooledChannelManager), 100, 3000, TimeUnit.MILLISECONDS);
    }

}
