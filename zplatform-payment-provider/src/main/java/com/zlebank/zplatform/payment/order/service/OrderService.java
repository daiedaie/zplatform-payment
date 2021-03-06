/* 
 * OrderService.java  
 * 
 * version TODO
 *
 * 2016年10月11日 
 * 
 * Copyright (c) 2016,zlebank.All rights reserved.
 * 
 */
package com.zlebank.zplatform.payment.order.service;

import com.zlebank.zplatform.payment.exception.PaymentOrderException;
import com.zlebank.zplatform.payment.order.bean.SimpleOrderBean;

/**
 * 订单生成接口
 *
 * @author guojia
 * @version
 * @date 2016年10月11日 下午4:01:32
 * @since 
 */
public interface OrderService {

	/**
	 * 生成消费订单
	 * @param orderBean 订单数据bean
	 * @return 受理订单号TN
	 */
	public String createConsumeOrder(final SimpleOrderBean orderBean) throws PaymentOrderException;
}
