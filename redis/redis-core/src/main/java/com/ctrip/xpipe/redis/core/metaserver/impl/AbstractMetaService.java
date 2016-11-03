package com.ctrip.xpipe.redis.core.metaserver.impl;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestOperations;

import com.ctrip.xpipe.redis.core.entity.KeeperMeta;
import com.ctrip.xpipe.redis.core.metaserver.MetaServerConsoleService;
import com.ctrip.xpipe.redis.core.metaserver.MetaServerService;
import com.ctrip.xpipe.spring.RestTemplateFactory;
import com.google.common.base.Function;

/**
 * @author wenchao.meng
 *
 * Sep 5, 2016
 */
public abstract class AbstractMetaService implements MetaServerService{
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	public static int retryTimes = Integer.parseInt(System.getProperty("metaserver.retryTimes", "3"));

	public static int retryIntervalMilli = Integer.parseInt(System.getProperty("metaserver.retryIntervalMilli", "5"));

	protected RestOperations restTemplate = RestTemplateFactory.createCommonsHttpRestTemplate(retryTimes, retryIntervalMilli);
	
	protected <T> T pollMetaServer(Function<String, T> fun) {
		
		List<String> metaServerList = getMetaServerList();

		for (String url : metaServerList) {
			
			try{
				T result = fun.apply(url);
				if (result != null) {
					return result;
				}
			}catch(Exception e){
				logger.error("[pollMetaServer][error poll server]{}", url);
			}
		}
		return null;
	}

	protected abstract List<String> getMetaServerList();
	
	
	@Override
	public KeeperMeta getActiveKeeper(final String clusterId, final String shardId) {
		
		return pollMetaServer(new Function<String, KeeperMeta>() {

			@Override
			public KeeperMeta apply(String metaServerAddress) {
				
				String activeKeeperPath = getRealPath(metaServerAddress, GET_ACTIVE_KEEPER);
				KeeperMeta keeperMeta = restTemplate.getForObject(activeKeeperPath, KeeperMeta.class, clusterId, shardId);
				return keeperMeta;
			}

		});
	}

	public static  String getRealPath(String metaServerAddress, String specificPath) {
		
		return String.format("%s/%s/%s", metaServerAddress, MetaServerConsoleService.PATH_PREFIX, specificPath);
	}
}
