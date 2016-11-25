package com.schedulers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xiehui1956(@)gmail.com on 16-11-24.
 */
public class HelloWordService {

    private static final Logger logger = LoggerFactory.getLogger(HelloWordService.class);

    public void hello() {
        //这里执行定时调度业务
        logger.info("hello.......1");
    }

    public void word() {
        logger.info("word.......2");
    }
}
