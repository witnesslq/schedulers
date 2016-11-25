最近准备做一个可满足易扩展,高可用,负载均衡的分布式任务调度器参考了网上的很多帖子和文章.遗憾的是没有找到一个可以跑起来实例,下面是我调整总结可以跑起来的实例.在这里记录下.
系统特点也是Quertz的特点,支持水平扩展,通俗的说就是可以部署多个服务节点/不存在任务重复执行的情况/一个任务节点宕机后其他节点的任务可以照常继续执行.不说废话了,直接上代码.
[Quartz集群架构可以参考这个帖子](http://www.cnblogs.com/davidwang456/p/4205237.html)

### Quartz分布式集群方案

  * Scheduler表<mysql-db>
  
`````
    DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
    DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
    DROP TABLE IF EXISTS QRTZ_LOCKS;
    DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_TRIGGERS;
    DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;
    DROP TABLE IF EXISTS QRTZ_CALENDARS;
    
    -- 存储每一个已配置的Job的详细信息
    CREATE TABLE QRTZ_JOB_DETAILS(
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME VARCHAR(250) NOT NULL,
    IS_DURABLE VARCHAR(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
    ENGINE=InnoDB;
    
    -- 存储已配置的Trigger的信息
    CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT(13) NULL,
    PREV_FIRE_TIME BIGINT(13) NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT(13) NOT NULL,
    END_TIME BIGINT(13) NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT(2) NULL,
    JOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
    REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
    ENGINE=InnoDB;
    
    -- 存储简单的Trigger，包括重复次数、间隔、以及已触的次数
    CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT(7) NOT NULL,
    REPEAT_INTERVAL BIGINT(12) NOT NULL,
    TIMES_TRIGGERED BIGINT(10) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;
    
    -- 存储CronTrigger，包括Cron表达式和时区信息
    CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

    CREATE TABLE QRTZ_SIMPROP_TRIGGERS
    (          
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR(1) NULL,
    BOOL_PROP_2 VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) 
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;

    CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;
    
    -- 存储Quartz的Calendar信息
    CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
    ENGINE=InnoDB;
    
    -- 存储已暂停的Trigger组的信息
    CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
    ENGINE=InnoDB;
    
    -- 存储与已触发的Trigger相关的状态信息，以及相联Job的执行信息
    CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT(13) NOT NULL,
    SCHED_TIME BIGINT(13) NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT VARCHAR(1) NULL,
    REQUESTS_RECOVERY VARCHAR(1) NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID))
    ENGINE=InnoDB;
    
    -- 存储少量的有关Scheduler的状态信息，和别的Scheduler实例
    CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
    CHECKIN_INTERVAL BIGINT(13) NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
    ENGINE=InnoDB;
    
    -- 存储程序的悲观锁的信息
    CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME))
    ENGINE=InnoDB;

CREATE INDEX IDX_QRTZ_J_REQ_RECOVERY ON QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_J_GRP ON QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_QRTZ_T_J ON QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_JG ON QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_T_C ON QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_QRTZ_T_G ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_T_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_N_G_STATE ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NEXT_FIRE_TIME ON QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST ON QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_QRTZ_T_NFT_ST_MISFIRE_GRP ON QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_QRTZ_FT_TRIG_INST_NAME ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_QRTZ_FT_INST_JOB_REQ_RCVRY ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_QRTZ_FT_J_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_JG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_QRTZ_FT_T_G ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_QRTZ_FT_TG ON QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

commit; 
    
`````


  * maven配置
   
`````
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.schedulers</groupId>
        <artifactId>schedulers</artifactId>
        <version>1.0-SNAPSHOT</version>
        <name>schedulers</name>
        <url>http://maven.apache.org</url>
    
        <properties>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            <java.version>1.8</java.version>
            <spring.version>4.3.3.RELEASE</spring.version>
        </properties>
    
        <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>1.4.2.RELEASE</version>
        </parent>
    
        <dependencies>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>3.8.1</version>
                <scope>test</scope>
            </dependency>
            <!-- quartz -->
            <dependency>
                <groupId>org.quartz-scheduler</groupId>
                <artifactId>quartz</artifactId>
                <version>2.2.1</version>
            </dependency>
            <dependency>
                <groupId>org.quartz-scheduler</groupId>
                <artifactId>quartz-jobs</artifactId>
                <version>2.2.1</version>
            </dependency>
    
            <!-- mysql -->
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>6.0.5</version>
            </dependency>
    
            <!-- spring boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
    
            <!-- spring -->
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-context-support</artifactId>
                <version>${spring.version}</version>
            </dependency>
    
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-orm</artifactId>
                <version>${spring.version}</version>
            </dependency>
    
        </dependencies>
        <build>
            <finalName>schedulers</finalName>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
                <plugin>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>2.3.2</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <encoding>${project.build.sourceEncoding}</encoding>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                        <execution>
                            <id>copy-dependencies</id>
                            <phase>prepare-package</phase>
                            <goals>
                                <goal>copy-dependencies</goal>
                            </goals>
                            <configuration>
                                <outputDirectory>${project.build.directory}/lib</outputDirectory>
                                <overWriteReleases>false</overWriteReleases>
                                <overWriteSnapshots>false</overWriteSnapshots>
                                <overWriteIfNewer>true</overWriteIfNewer>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-jar-plugin</artifactId>
                    <version>2.4</version>
                    <configuration>
                        <archive>
                            <addMavenDescriptor>false</addMavenDescriptor>
                            <manifest>
                                <addClasspath>true</addClasspath>
                                <classpathPrefix>lib/</classpathPrefix>
                                <mainClass>com.schedulers.controller.ApplicationStart</mainClass>
                            </manifest>
                        </archive>
                        <excludes>
                            <exclude>lib/</exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </project>    
`````

  * quartz.properties配置
   
`````
    #用在 JDBC JobStore 中来唯一标识实例，但是所有集群节点中必须相同
    org.quartz.scheduler.instanceName=schedulers
    #基于主机名和时间戳来产生实例 ID
    org.quartz.scheduler.instanceId=AUTO
    org.quartz.threadPool.class:org.quartz.simpl.SimpleThreadPool
    org.quartz.threadPool.threadCount=10
    org.quartz.threadPool.threadPriority=5
    org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread=true
    #============================================================================
    # Configure JobStore  
    #============================================================================
    org.quartz.jobStore.misfireThreshold=60000
    # 将任务持久化到数据中
    org.quartz.jobStore.class:org.quartz.impl.jdbcjobstore.JobStoreTX
    org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
    # Configuring JDBCJobStore with the Table Prefix
    org.quartz.jobStore.tablePrefix:QRTZ_
    org.quartz.jobStore.maxMisfiresToHandleAtATime=10
    #设置Scheduler实例参与到一个集群当中,这一属性会贯穿于调度框架的始终，用于修改集群环境中操作的默认行为。
    org.quartz.jobStore.isClustered=true
    #定义了Scheduler 实例检入到数据库中的频率(单位：毫秒)
    org.quartz.jobStore.clusterCheckinInterval=20000
    
`````

  * spring文件配置<application-scheduler.xml>
   
````
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:context="http://www.springframework.org/schema/context"
           xsi:schemaLocation="http://www.springframework.org/schema/beans
         http://www.springframework.org/schema/beans/spring-beans-3.0.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    
        <context:property-placeholder location="jdbc.properties, app.properties"/>
    
        <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
            <!-- 指定连接数据库的驱动-->
            <property name="driverClass" value="${jdbc.driverClassName}"/>
            <!-- 指定连接数据库的URL-->
            <property name="jdbcUrl" value="${jdbc.url}"/>
            <!-- 指定连接数据库的用户名-->
            <property name="user" value="${jdbc.username}"/>
            <!-- 指定连接数据库的密码-->
            <property name="password" value="${jdbc.password}"/>
            <!-- 指定连接池中保留的最大连接数. Default:15-->
            <property name="maxPoolSize" value="${jdbc.maxPoolSize}"/>
            <!-- 指定连接池中保留的最小连接数-->
            <property name="minPoolSize" value="${jdbc.minPoolSize}"/>
            <!-- 指定连接池的初始化连接数  取值应在minPoolSize 与 maxPoolSize 之间.Default:3-->
            <property name="initialPoolSize" value="${jdbc.initialPoolSize}"/>
            <!-- 最大空闲时间,60秒内未使用则连接被丢弃。若为0则永不丢弃。 Default:0-->
            <property name="maxIdleTime" value="${jdbc.maxIdleTime}"/>
            <!-- 当连接池中的连接耗尽的时候c3p0一次同时获取的连接数. Default:3-->
            <property name="acquireIncrement" value="${jdbc.acquireIncrement}"/>
            <!-- JDBC的标准,用以控制数据源内加载的PreparedStatements数量。
            但由于预缓存的statements属于单个connection而不是整个连接池所以设置这个参数需要考虑到多方面的因数.如果maxStatements与maxStatementsPerConnection均为0,则缓存被关闭。Default:0-->
            <property name="maxStatements" value="${jdbc.maxStatements}"/>
            <!-- 每60秒检查所有连接池中的空闲连接.Default:0 -->
            <property name="idleConnectionTestPeriod" value="${jdbc.idleConnectionTestPeriod}"/>
        </bean>
    
        <!-- 启动触发器的配置开始 -->
        <bean name="scheduler" lazy-init="false" autowire="no"
              class="org.springframework.scheduling.quartz.SchedulerFactoryBean">
            <property name="dataSource" ref="dataSource"/>
            <property name="applicationContextSchedulerContextKey" value="applicationContextKey"/>
            <property name="quartzProperties" value="classpath:quartz.properties"/>
            <property name="schedulerName" value="bigEngineScheduler"/>
            <!-- scheduler延时启动 -->
            <property name="startupDelay" value="${scheduler.startupDelay}"/>
            <!--可选，scheduler 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了 -->
            <property name="overwriteExistingJobs" value="true"/>
            <!-- 设置自动启动 -->
            <property name="autoStartup" value="true" />
            <property name="triggers">
                <list>
                    <ref bean="helloTrigger"/>
                    <!--<ref bean="wordTrigger"/>-->
                </list>
            </property>
        </bean>
        <!-- 启动触发器的配置结束 -->
    
        <bean id="helloJob" class="org.springframework.scheduling.quartz.JobDetailFactoryBean">
            <property name="jobClass">
                <value>com.schedulers.job.MyJobHello</value>
            </property>
            <property name="durability" value="true"/>
            <!-- 属性必须设置为 true,当Quartz服务被中止后，再次启动或集群中其他机器接手任务时会尝试恢复执行之前未完成的所有任务. -->
            <property name="requestsRecovery" value="true"/>
        </bean>
        <bean id="helloTrigger" class="org.springframework.scheduling.quartz.CronTriggerFactoryBean">
            <property name="jobDetail" ref="helloJob"/>
            <property name="cronExpression" value="${helloTrigger.cron}"/>
        </bean>
    
        <!--<bean id="wordJob" class="org.springframework.scheduling.quartz.JobDetailFactoryBean">
            <property name="jobClass">
                <value>com.schedulers.job.MyJobWord</value>
            </property>
            <property name="durability" value="true"/>
            &lt;!&ndash; 属性必须设置为 true,当Quartz服务被中止后，再次启动或集群中其他机器接手任务时会尝试恢复执行之前未完成的所有任务. &ndash;&gt;
            <property name="requestsRecovery" value="true"/>
        </bean>
        <bean id="wordTrigger" class="org.springframework.scheduling.quartz.CronTriggerFactoryBean">
            <property name="jobDetail" ref="wordJob"/>
            <property name="cronExpression" value="${wordTrigger.cron}"/>
        </bean>-->
    
    </beans>
````

  * app.properties配置   
  
`````
    #scheduler延时启动
    scheduler.startupDelay=5
    # cron
    helloTrigger.cron=0/1 * * ? * * *
    wordTrigger.cron=0/1 * * ? * * *
`````

  * jdbc.properties配置   
  
`````
    jdbc.username=
    jdbc.password=
    jdbc.driverClassName=com.mysql.cj.jdbc.Driver
    jdbc.url=
    jdbc.initialPoolSize=20
    jdbc.maxPoolSize=100
    jdbc.minPoolSize=10
    jdbc.maxIdleTime=600
    jdbc.acquireIncrement=5
    jdbc.maxStatements=5
    jdbc.idleConnectionTestPeriod=60
`````

  * ApplicationStart启动类
  
`````
    package com.schedulers.main;
    
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    import org.springframework.context.annotation.ImportResource;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    
    @RestController
    @SpringBootApplication
    @ImportResource({"classpath:spring/application-*.xml"})
    public class ApplicationStart {
    
        @RequestMapping("/")
        String index() {
            return "Hello Spring Boot";
        }
    
        public static void main(String[] args) {
            SpringApplication.run(ApplicationStart.class, args);
        }
    
    }
`````

  * HelloWordService服务类   
  
`````
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

`````

  * MyJobHello-job类  
   
`````
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

`````

这个程序我已经测试过了,部署了两个节点.两个节点同时只有一个节点在跑任务.因为为了更直观的看测试结果所以我就启动了一个job. 
注意job同步到数据库之后,再次运行程序的时候任务主要就依赖数据库中的记录了.