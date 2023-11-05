package top.sharehome.springbootinittemplate.config.job;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * XXL-JOB配置文件
 * 需要用到XXL-JOB时，先将visual/xxl-job-admin部署上
 *
 * @author AntonyCheng
 */
@Configuration
@EnableConfigurationProperties(XxlJobProperties.class)
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "xxl.job", name = "enable", havingValue = "true")
public class XxlJobConfiguration {

    private final XxlJobProperties xxlJobProperties;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdminAddresses());
        xxlJobSpringExecutor.setAccessToken(xxlJobProperties.getAccessToken());
        XxlJobProperties.Executor executor = xxlJobProperties.getExecutor();
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setLogPath(executor.getLogpath());
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());
        log.info(">>>>>>>>>>> xxl-job config init.");
        return xxlJobSpringExecutor;
    }

    /**
     * 依赖注入日志输出
     */
    @PostConstruct
    public void initDi() {
        log.info("############ xxl-job config DI.");
    }

}