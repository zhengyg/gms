package com.taoyuan.gms.model.entity.admin.web;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.taoyuan.framework.common.entity.TyOperRecordEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.sql.rowset.serial.SerialBlob;
import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/**
 * 网站配置
 */
@Data
@ToString
@EqualsAndHashCode
@TableName(value = "admin_websetting")
public class WebSettingEntity implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id = 1l;

    //名称
    private String name;

    //关键字
    private String keyWord;

    //描述
    private String description;

    //金币和人民币兑换比例
    private Long exchangePropor;

    //兑换开关
    private boolean enableExchange;

    //代理充值折扣
    private int proxyRechargeDiscount;

    //是否能24小时兑换
    private boolean enable24Recharge;

    //禁止兑换起始时间
//    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private String forbidRechargeBeginTime;

    //禁止兑换结束时间
//    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private String forbidRechargeEndTime;

    //注册赠送金币
    private int registOfferGold;

    //流水计算天数
    private int serialCalDays;

    //充值经验兑换比例
    private Double rechargeExperiencePropor;

    //免费兑换倍数
    private int freeExchangeTimes;

    //获得成长值比例
    private Double gainGrowPropor;

    //超出手续费
    private Double overrangingFee;

    //下线投注工资比例
    private Double subordinateCathecticWagesPropor;

    //投注工资比例
    private Double cathecticWagesPropor;

    //亏损返利比例
    private Double lossRebatePropor;

    //首充返利比例
    private Double firstFillRebatePropor;

    //报名费
    private Double entryFee;

    //可得虚拟币
    private int virtualCornAvailableCount;

    //虚拟币可得到次数
    private int virtualCornAvailableTimes;

    private Long createUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp updateTime;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Date createTime;
//
//    private Long updateUser;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Date updateTime;
}
