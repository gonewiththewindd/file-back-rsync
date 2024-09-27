package com.gone.file_backup;

import com.gone.file_backup.rsync.RsyncServer;
import com.gone.file_backup.rsync.network.channel.PooledChannelManager;
import com.gone.file_backup.rsync.network.handler.ChannelOperationHandler;
import com.gone.file_backup.rsync.schedule.RetryTask;
import com.gone.file_backup.rsync.sender.Sender;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class OssFileMonitorApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext ctx = SpringApplication.run(OssFileMonitorApplication.class, args);
        ChannelOperationHandler optChannelHandler = ctx.getBean(ChannelOperationHandler.class);
        new Thread(new RsyncServer(optChannelHandler)).start();

        Sender sender = ctx.getBean(Sender.class);
        PooledChannelManager pooledChannelManager = ctx.getBean(PooledChannelManager.class);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new RetryTask(sender, pooledChannelManager), 100, 3000, TimeUnit.MILLISECONDS);
    }

}
