package com.taoyuan.gms.job.proxymanage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.taoyuan.gms.core.proxymanage.service.IGoldenRechargeService;
import com.taoyuan.gms.job.JobManager;
import com.taoyuan.gms.model.entity.proxy.GoldenRechargeEntity;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@Configuration
public class GoldenRechargeJob extends QuartzJobBean
{
    private static GoldenRechargeJob goldenRechargeJob;
    @Autowired
    private IGoldenRechargeService goldenRechargeService;

    @PostConstruct
    public void init() {
        goldenRechargeJob = this;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext)
        throws JobExecutionException
    {
        log.info("JobDateMap:{}",
            jobExecutionContext.getJobDetail().getJobDataMap());

        // 获取业务主键
        Long id = jobExecutionContext.getJobDetail()
            .getJobDataMap()
            .getLong("GoldRecharge");
        
        GoldenRechargeEntity entity = getEntityById(id);
        // 更新状态为不可撤销
        entity.setStatus(2);
        UpdateWrapper<GoldenRechargeEntity> wrapper =
            new UpdateWrapper<GoldenRechargeEntity>();
        wrapper.lambda().eq(GoldenRechargeEntity::getId, id);
        goldenRechargeJob.goldenRechargeService.update(entity, wrapper);
        
        // 删除任务
        JobManager.removeJob("GoldenRecharge");
    }
    
    private GoldenRechargeEntity getEntityById(Long id)
    {
        log.info("id:{}", id);
        QueryWrapper<GoldenRechargeEntity> queryWrapper =
            new QueryWrapper<GoldenRechargeEntity>();
        queryWrapper.lambda().eq(GoldenRechargeEntity::getId, id);
        if(null==goldenRechargeJob.goldenRechargeService){
            log.error("goldenRechargeService is null");
        }
        return goldenRechargeJob.goldenRechargeService.getOne(queryWrapper);
    }
}
