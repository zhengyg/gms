package com.taoyuan.gms.core.adminmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.taoyuan.framework.aaa.service.TyUserService;
import com.taoyuan.framework.common.entity.TyUser;
import com.taoyuan.framework.common.entity.TyUserLoginEntity;
import com.taoyuan.framework.common.exception.ValidateException;
import com.taoyuan.framework.common.http.TyResponse;
import com.taoyuan.framework.common.http.TySession;
import com.taoyuan.framework.common.http.TySuccessResponse;
import com.taoyuan.framework.common.util.TyRandomUtil;
import com.taoyuan.gms.api.admin.CardPasswordApi;
import com.taoyuan.gms.core.adminmanage.service.ICardPasswordService;
import com.taoyuan.gms.model.entity.admin.CardPasswordEntity;
import com.taoyuan.gms.model.entity.proxy.CardPassword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class CardPasswordController extends BaseController implements CardPasswordApi {

    @Autowired
    private ICardPasswordService service;

    @Autowired
    private TyUserService userService;

    @Override
    public IPage<Map<String, Object>> retrieve(Map<String, Object> map) {
        Page page = getPage(map);

        QueryWrapper<CardPasswordEntity> wrapper = new QueryWrapper<CardPasswordEntity>();
        if (map.containsKey("keyword")) {
            String keyword = (String) map.get("keyword");
            wrapper.lambda().eq(CardPasswordEntity::getCardId, keyword).or().eq(CardPasswordEntity::getCardPassword,
                    keyword).or().eq(CardPasswordEntity::getRechargeId, keyword);
        }

        if (map.containsKey("cardType")) {
            wrapper.lambda().eq(CardPasswordEntity::getCardType, map.get("cardType"));
        }

        if (map.containsKey("status")) {
            wrapper.lambda().eq(CardPasswordEntity::getStatus, map.get("status"));
        }

        if (map.containsKey("owner")) {
            wrapper.lambda().eq(CardPasswordEntity::getOwner, map.get("owner"));
        }
        wrapper.lambda().orderByDesc(CardPasswordEntity::getCreateTime);
        return service.pageMaps(page, wrapper);
    }

    @Override
    public List<CardPasswordEntity> create(Map<String, Object> map) {
        if (!map.containsKey("cardType")) {
            throw new ValidateException("卡类型不能为空。");
        }
        if (!map.containsKey("number")) {
            throw new ValidateException("数量不能为空。");
        }

        int cardType = (int) map.get("cardType");
        String cardHead = getCardHead(cardType);
        int number = (int) map.get("number");

        List<CardPasswordEntity> entityList = new ArrayList<CardPasswordEntity>();
        for (int i = 0; i < number; i++) {
            //默认生成随机8位id和密码
            String id = TyRandomUtil.getRandomNum(8);
            log.info("generate id is {}", id);
            String pwd = TyRandomUtil.getRandomStr(8);
            log.info("generate password is {}", pwd);
            CardPasswordEntity entity = new CardPasswordEntity();
            entity.setCardType(cardType);
            entity.setCardId(cardHead + id);
            entity.setStatus(1);
            entity.setCardPassword(pwd);
            entity.setCreateTime(new Date());
            entity.setCreateUser(TySession.getCurrentUser().getUserId());
            entity.setOwner(TySession.getCurrentUser().getName());
            entity.setStartTime(new Date());
            entityList.add(entity);
        }
        service.saveBatch(entityList);

        //更新库存

        return entityList;
    }

    @Override
    public TyResponse withdraw(CardPassword cardPassword) {
        log.info("input:{}", cardPassword);
        String cardId = cardPassword.getCardId();
        if (StringUtils.isEmpty(cardId)) {
            throw new ValidateException("卡号不能为空。");
        }

        String pwd = cardPassword.getPassword();
        if (StringUtils.isEmpty(pwd)) {
            throw new ValidateException("密码不能为空。");
        }

        CardPasswordEntity entity = service.getByCardId(cardId);
        log.info("withdraw card password:{}", entity);
        //设置状态为已回收
        entity.setStatus(2);
        entity.setEndTime(new Date());
        service.saveOrUpdate(entity);

        //回收后给用户增加对应金额
        updateBalance(Long.valueOf(entity.getRechargeId()), entity.getMoney());
        return new TySuccessResponse(cardId);
    }

    @Override
    public TyResponse withdrawbatch(List<CardPassword> cardPasswordList) {
        if (CollectionUtils.isEmpty(cardPasswordList)) {
            return new TySuccessResponse(null);
        }

        List<CardPasswordEntity> dbValueList = new ArrayList<CardPasswordEntity>();
        for (CardPassword cp : cardPasswordList) {
            String cardId = cp.getCardId();
            if (StringUtils.isEmpty(cardId)) {
                throw new ValidateException("待回收卡密卡号不能为空。");
            }
            String pwd = cp.getPassword();
            if (StringUtils.isEmpty(pwd)) {
                throw new ValidateException("待回收卡密密码不能为空。");
            }

            CardPasswordEntity dbValue = service.getByCardIdAndPwd(cardId, pwd);
            if (null != dbValue) {
                //设置状态为已回收
                dbValue.setStatus(2);
                dbValue.setEndTime(new Date());
                dbValueList.add(dbValue);
            }

        }

        log.info("card password list:{}", dbValueList);
        service.saveOrUpdateBatch(dbValueList);

        //回收后给用户增加对应金额
        for (CardPasswordEntity cp : dbValueList) {
            updateBalance(Long.valueOf(cp.getRechargeId()), cp.getMoney());
        }

        return new TySuccessResponse(dbValueList);
    }

    @Override
    public TyResponse cancelbatch(List<CardPassword> cardPasswordList) {
        if (CollectionUtils.isEmpty(cardPasswordList)) {
            return new TySuccessResponse(null);
        }

        List<CardPasswordEntity> dbValueList = new ArrayList<CardPasswordEntity>();
        for (CardPassword cp : cardPasswordList) {
            String cardId = cp.getCardId();
            if (StringUtils.isEmpty(cardId)) {
                throw new ValidateException("待撤销卡密卡号不能为空。");
            }

            String pwd = cp.getPassword();
            if (StringUtils.isEmpty(pwd)) {
                throw new ValidateException("待撤销卡密密码不能为空。");
            }

            CardPasswordEntity dbValue = service.getByCardIdAndPwd(cardId, pwd);
            if (null != dbValue) {
                //设置状态为已撤销
                dbValue.setStatus(2);
                dbValue.setEndTime(new Date());
                dbValueList.add(dbValue);
            }

        }

        log.info("card list is {}", dbValueList);
        service.saveOrUpdateBatch(dbValueList);

        Long proxyId = getCurrentUserId();
        BigDecimal cancelMoney = BigDecimal.ZERO;
        //计算需要撤销的总金额
        for (CardPasswordEntity cp : dbValueList) {
            cancelMoney.add(cp.getMoney());
        }
        //更新代理余额
        updateBalance(proxyId, cancelMoney);

        return new TySuccessResponse(dbValueList);
    }

    @Override
    public TyResponse cancel(CardPassword cardPassword) {
        String cardId = cardPassword.getCardId();
        if (StringUtils.isEmpty(cardId)) {
            throw new ValidateException("卡号不能为空。");
        }

        String pwd = cardPassword.getPassword();
        if (StringUtils.isEmpty(pwd)) {
            throw new ValidateException("密码不能为空。");
        }

        CardPasswordEntity entity = service.getByCardId(cardId);
        log.info("cancel object:{}", entity);
        //设置状态为已撤销
        entity.setStatus(3);
        entity.setEndTime(new Date());
        service.saveOrUpdate(entity);

        //更新用户余额
        updateBalance(getCurrentUserId(), entity.getMoney());
        return new TySuccessResponse(cardId);
    }

    @Override
    public TyResponse delete(String id) {
        if (null == id) {
            throw new ValidateException("卡号不能为空。");
        }
        log.info("card password id:{}", id);
        CardPasswordEntity entity = service.getByCardId(id);
        if (!StringUtils.isEmpty(entity.getRechargeId())) {
            throw new ValidateException("不能删除已经兑换的卡。");
        }

        //只有管理员才能删除，代理和会员不能删除
        service.remove(new QueryWrapper<CardPasswordEntity>().eq("card_id", id));
        return new TySuccessResponse(id);
    }

    @Override
    public List<CardPassword> getCardPasswordInfo(List<CardPassword> cardPasswordList) {
        log.info("input is {}", cardPasswordList);
        if (CollectionUtils.isEmpty(cardPasswordList)) {
            return cardPasswordList;
        }

        List<CardPassword> rsltList = new ArrayList<CardPassword>();
        List<String> cardIdList = new ArrayList<String>();
        for (CardPassword cp : cardPasswordList) {
            String cardId = cp.getCardId();
            if (StringUtils.isEmpty(cardId)) {
                throw new ValidateException("卡密卡号不能为空。");
            }

            CardPasswordEntity dbValue = service.getByCardId(cardId);
            if (null == dbValue) {
                cp.setInfo("卡号或密码不正确");
                rsltList.add(cp);
                continue;
            }

            //已回收,已充值
            if (dbValue.getStatus() == 2) {
                cp.setInfo("卡密已回收");
                rsltList.add(cp);
                continue;
            }

            //已撤销
            if (dbValue.getStatus() == 3) {
                cp.setInfo("卡密已撤销");
                rsltList.add(cp);
                continue;
            }

            String pwd = cp.getPassword();
            if (StringUtils.isEmpty(pwd.trim())) {
                cp.setInfo("卡号或密码不正确");
                rsltList.add(cp);
                continue;
            }

            if (pwd.equals(dbValue.getCardPassword())) {
                if (dbValue.getRechargeId() != null) {
                    String rechargeId = dbValue.getRechargeId();
                    TyUser user = userService.getUserById(Long.valueOf(rechargeId));
                    log.info("user info:{}", user);
                    cp.setRechargeId(rechargeId);
                    cp.setName(user.getName());
                    //TODO
                    // 待增加获取用户昵称和QQ
                    cp.setNickName(user.getName());
                    cp.setQq(user.getName());
                }
            } else {
                cp.setInfo("卡号或密码不正确");
            }

            if (cardIdList.contains(cardId)) {
                continue;
            }
            cardIdList.add(cardId);

            rsltList.add(cp);
        }
        return rsltList;
    }

    @Override
    public IPage<Map<String, Object>> query(Map<String, Object> map) {
        Page page = getPage(map);

        QueryWrapper<CardPasswordEntity> wrapper = new QueryWrapper<CardPasswordEntity>();
//        if (map.containsKey("createUser")) {
//            Long owner = Long.valueOf(map.get("createUser").toString());
//            wrapper.lambda().eq(CardPasswordEntity::getCreateUser, owner);
//        }

        if (map.containsKey("keyword")) {
            String keyword = (String) map.get("keyword");
            if (!StringUtils.isEmpty(keyword)) {
                wrapper.lambda().eq(CardPasswordEntity::getCardId, Long.valueOf(keyword)).or().eq
                        (CardPasswordEntity::getCardPassword, keyword).or().eq(CardPasswordEntity::getId, Long.valueOf
                        (keyword));
            }
        }

        if (map.containsKey("cardType")) {
            int cardType = (int) map.get("cardType");
            wrapper.lambda().eq(CardPasswordEntity::getCardType, cardType);
        }

        wrapper.lambda().orderByDesc(CardPasswordEntity::getCreateTime);
        wrapper.lambda().orderByDesc(CardPasswordEntity::getCreateTime);
        return service.pageMaps(page, wrapper);
    }

    private String getCardHead(int cardType) {
        String cardHead = null;
        switch (cardType) {
            case 1:
                cardHead = "10y";
                break;
            case 2:
                cardHead = "20y";
                break;
            case 3:
                cardHead = "30y";
                break;
            case 4:
                cardHead = "redzuan";
                break;
            default:
                throw new ValidateException("不支持的卡类型。");
        }
        return cardHead;
    }

    private String getRandomValue(int length, String randomValue) {
        QueryWrapper<CardPasswordEntity> wrapper = new QueryWrapper<CardPasswordEntity>();
        List<CardPasswordEntity> dbValue = service.list(wrapper);
        for (CardPasswordEntity entity : dbValue) {
            if (entity.getCardId().equals(getCardHead(entity.getCardType()) + randomValue)) {
                randomValue = TyRandomUtil.getRandomNum(length);
                return getRandomValue(length, randomValue);
            }
        }

        return randomValue;
    }
}
