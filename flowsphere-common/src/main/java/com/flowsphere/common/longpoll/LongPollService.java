package com.flowsphere.common.longpoll;

import com.flowsphere.common.config.YamlAgentConfig;
import com.flowsphere.common.config.YamlAgentConfigCache;
import com.flowsphere.common.eventbus.EventBusManager;
import com.flowsphere.common.eventbus.enums.CmdEnum;
import com.flowsphere.common.eventbus.event.RefreshServerListEvent;
import com.flowsphere.common.longpoll.entity.NotificationRequest;
import com.flowsphere.common.longpoll.entity.ReleaseMessage;
import com.flowsphere.common.transport.SimpleHttpClient;
import com.flowsphere.common.transport.SimpleHttpRequest;
import com.flowsphere.common.utils.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LongPollService {

    private static final LongPollService INSTANT = new LongPollService();

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    private static final String NOTIFICATION_API_URL = "/notification/pollNotification";

    public static LongPollService getInstance() {
        return INSTANT;
    }

    public void startLongPolling(String serverAddr, String applicationName, String ip, int port) {
        YamlAgentConfig yamlAgentConfig = YamlAgentConfigCache.get();
        SCHEDULER.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Response response = SimpleHttpClient.getInstance().send(new SimpleHttpRequest()
                            .setUrl(serverAddr + NOTIFICATION_API_URL)
                            .setData(
                                    new NotificationRequest()
                                            .setApplicationName(applicationName)
                                            .setIp(ip)
                                            .setPort(port)
                            )
                    );
                    responseHandler(response);
                } catch (Exception e) {
                    log.error("[flowsphere] long polling notification fail", e);
                }
            }
        }, 0, yamlAgentConfig.getLongPollDelay(), TimeUnit.MILLISECONDS);
    }

    private void responseHandler(Response response) throws IOException {
        if (response.isSuccessful()) {
            String bodyStr = response.body().string();
            ReleaseMessage releaseMessage = JacksonUtils.toObj(bodyStr, ReleaseMessage.class);
            if (CmdEnum.PROVIDER_OFFLINE.getCmd().equals(releaseMessage.getCmd())) {
                String extendData = releaseMessage.getExtendData();
                ReleaseMessage releaseMessage1 = JacksonUtils.toObj(extendData, ReleaseMessage.class);
                releaseMessage.setApplicationName(releaseMessage1.getApplicationName());
                EventBusManager.post(new RefreshServerListEvent(releaseMessage.getApplicationName()));
            }
        }
    }


}
