package com.flowsphere.feature.removal;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Map;

import static com.sun.webkit.graphics.GraphicsDecoder.SCALE;

@Slf4j
public class RemovalThread implements Runnable {

    private final int expireTime;

    public RemovalThread(int expireTime) {
        this.expireTime = expireTime;
    }

    @Override
    public void run() {
        Map<String, Instance> instanceCallResult = InstanceCallResultCache.getInstanceCallResult();
        if (instanceCallResult.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<String, Instance>> iterator = instanceCallResult.entrySet().iterator();
             iterator.hasNext(); ) {
            Instance info = iterator.next().getValue();
            if (System.currentTimeMillis() - info.getLastInvokeTime() >= expireTime) {
                iterator.remove();
                if (info.getRemovalStatus().get()) {
                    //TODO 通知server状态更新了
                }
                continue;
            }
            info.setErrorRate(calErrorRate(info));
        }
    }


    private float calErrorRate(Instance info) {
        if (info.getRequestNum().get() == 0 || info.getRequestFailNum().get() == 0) {
            return 0;
        } else {
            BigDecimal count = new BigDecimal(info.getRequestNum().get());
            BigDecimal failNum = new BigDecimal(info.getRequestFailNum().get());
            return failNum.divide(count, SCALE, RoundingMode.HALF_UP).floatValue();
        }
    }

}
