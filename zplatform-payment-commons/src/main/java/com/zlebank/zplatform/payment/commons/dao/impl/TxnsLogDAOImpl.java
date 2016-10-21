/* 
 * TxnsLogDAOImpl.java  
 * 
 * version TODO
 *
 * 2016年9月13日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.payment.commons.dao.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.zlebank.zplatform.payment.commons.dao.TxnsLogDAO;
import com.zlebank.zplatform.payment.commons.enums.TradeStatFlagEnum;
import com.zlebank.zplatform.payment.exception.PaymentRouterException;
import com.zlebank.zplatform.payment.pojo.PojoTxnsLog;
import com.zlebank.zplatform.payment.quickpay.bean.PayBean;
import com.zlebank.zplatform.payment.risk.enums.RiskLevelEnum;

/**
 * Class Description
 *
 * @author guojia
 * @version
 * @date 2016年9月13日 下午5:33:02
 * @since
 */
@Repository
public class TxnsLogDAOImpl extends HibernateBaseDAOImpl<PojoTxnsLog> implements
		TxnsLogDAO {

	private static final Logger log = LoggerFactory
			.getLogger(TxnsLogDAOImpl.class);

	public void saveTxnsLog(PojoTxnsLog txnsLog) {
		super.saveEntity(txnsLog);
	}

	/**
	 *
	 * @param txnseqno
	 * @return
	 */
	@Override
	@Transactional(readOnly=true)
	public PojoTxnsLog getTxnsLogByTxnseqno(String txnseqno) {
		Criteria criteria = getSession().createCriteria(PojoTxnsLog.class);
		criteria.add(Restrictions.eq("txnseqno", txnseqno));
		return (PojoTxnsLog) criteria.uniqueResult();
	}

	/**
	 *
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED,rollbackFor=Throwable.class)
	public void riskTradeControl(String txnseqno, String coopInsti,
			String merchNo, String memberId, String busiCode, String txnAmt,
			String cardType, String cardNo) throws PaymentRouterException {
		// TODO Auto-generated method stub
		log.info("trade risk control start");
		int riskLevel = 0;
		int riskOrder = 0;
		RiskLevelEnum riskLevelEnum = null;
		String riskInfo = "";

		Session session = getSession();
		SQLQuery query = (SQLQuery) session.createSQLQuery(
				"SELECT FNC_GETRISK(?,?,?,?,?,?,?) AS RISK FROM DUAL")
				.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
		Object[] paramaters = new Object[] { coopInsti, merchNo, memberId,
				busiCode, txnAmt, cardType, cardNo };
		if (null != paramaters) {
			for (int i = 0; i < paramaters.length; i++) {
				query.setParameter(i, paramaters[i]);
			}
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> riskList = query.list();
		log.info("trade risk result:" + JSON.toJSONString(riskList));
		if (riskList.size() > 0) {
			riskInfo = riskList.get(0).get("RISK") + "";
			if (riskInfo.indexOf(",") > 0) {
				String[] riskInfos = riskInfo.split(",");
				try {
					riskOrder = Integer.valueOf(riskInfos[0]);
					riskLevel = Integer.valueOf(riskInfos[1]);
					riskLevelEnum = RiskLevelEnum.fromValue(riskLevel);
					log.info("riskOrder:" + riskOrder);
					log.info("riskLevel:" + riskLevel);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new PaymentRouterException("T034");
				}
			} else {
				riskLevelEnum = RiskLevelEnum.fromValue(Integer
						.valueOf(riskInfo));
			}
		} else {
			throw new PaymentRouterException("T034");
		}
		if (RiskLevelEnum.PASS == riskLevelEnum) {// 交易通过
			return;
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void initretMsg(String txnseqno) {
		// TODO Auto-generated method stub       
		String hql = "update PojoTxnsLog set payretcode = '',payretinfo='',retcode='',retinfo='' where txnseqno = ?  ";
		Session session = getSession();
		Query query = session.createQuery(hql);
		query.setParameter(0, txnseqno);
		int rows = query.executeUpdate();
		log.info("initretmsg sql :{},effect rows:{}", hql, rows);
	}

	/**
	 *
	 * @param txnseqno
	 * @param payBean
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updateBankCardInfo(String txnseqno, PayBean payBean) {
		// TODO Auto-generated method stub
		String hql = "update PojoTxnsLog set pan = ?,cardtype = ?,cardinstino = ?,panName = ? where txnseqno = ? ";
		Session session = getSession();
		Query query = session.createQuery(hql);
		query.setParameter(0, payBean.getCardNo());
		query.setParameter(1, payBean.getCardType());
		query.setParameter(2, payBean.getBankCode());
		query.setParameter(3, payBean.getCardKeeper());
		query.setParameter(4, txnseqno);
		int rows = query.executeUpdate();
		log.info("updateBankCardInfo() sql :{},effect rows:{}", hql, rows);
	}
	
	public Long getTxnFee(PojoTxnsLog txnsLog){
        //交易序列号，扣率版本，业务类型，交易金额，会员号，原交易序列号，卡类型 
		SQLQuery sqlQuery = getSession().createSQLQuery("select FNC_GETFEES(?,?,?,?,?,?,?) as fee from dual");
		Object[] paramates = new Object[]{txnsLog.getTxnseqno(),txnsLog.getFeever(),txnsLog.getBusicode(),txnsLog.getAmount(),txnsLog.getAccsecmerno(),txnsLog.getTxnseqnoOg(),txnsLog.getCardtype()};
		for(int i=0;i<paramates.length;i++){
			sqlQuery.setParameter(i, paramates[i]);
		}
		List<Map<String, Object>> feeList = sqlQuery.list();
        
        if(feeList.size()>0){
            if(StringUtils.isNotEmpty((String)feeList.get(0).get("FEE"))){
                return 0L;
            }else{
                return Long.valueOf(feeList.get(0).get("FEE")+"");
            }
            
        }
        return 0L;
    }
	
	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updateTradeStatFlag(String txnseqno,
			TradeStatFlagEnum tradeStatFlagEnum) {
		String hql = "update PojoTxnsLog set tradestatflag = ? where txnseqno = ?";
		Query query = getSession().createQuery(hql);
		query.setParameter(0, tradeStatFlagEnum.getStatus());
		query.setParameter(1, txnseqno);
		int rows = query.executeUpdate();
		log.info("updateTradeStatFlag() effect rows:" + rows);
	}
}
