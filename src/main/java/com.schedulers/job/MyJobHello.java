package com.schedulers.job;

import com.schedulers.service.HelloWordService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Created by xiehui1956(@)gmail.com on 16-11-24.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution //不允许并发执行
public class MyJobHello extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        HelloWordService helloWordService = new HelloWordService();
        helloWordService.hello();
    }

}
