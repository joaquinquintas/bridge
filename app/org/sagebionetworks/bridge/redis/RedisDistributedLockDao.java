package org.sagebionetworks.bridge.redis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.dao.DistributedLockDao;
import org.sagebionetworks.bridge.exceptions.BridgeServiceException;
import org.sagebionetworks.bridge.exceptions.ConcurrentModificationException;

@Component
public class RedisDistributedLockDao implements DistributedLockDao {

    private static final int EXPIRATION_IN_SECONDS = 3 * 60;
    private JedisOps jedisOps;

    @Autowired
    public void setStringOps(JedisOps jedisOps) {
        this.jedisOps = jedisOps;
    }

    @Override
    public String acquireLock(final Class<?> clazz, final String identifier) {
        return acquireLock(clazz, identifier, EXPIRATION_IN_SECONDS);
    }

    @Override
    public String acquireLock(final Class<?> clazz, final String identifier, final int expireInSeconds) {
        checkNotNull(clazz);
        checkNotNull(identifier);
        checkArgument(expireInSeconds > 0);
        final String redisKey = createRedisKey(clazz, identifier);
        final String lock = BridgeUtils.generateGuid();
        final Long result = jedisOps.setnx(redisKey, lock);
        if (result != 1L) {
            Long expire = jedisOps.ttl(redisKey);
            if (expire < 0L) {
                expire(redisKey, expireInSeconds);
            }
            throw new ConcurrentModificationException("Lock already set.");
        }
        expire(redisKey, expireInSeconds);
        return lock;
    }

    @Override
    public boolean releaseLock(Class<?> clazz, String identifier, String lock) {
        checkNotNull(clazz);
        checkNotNull(identifier);
        checkNotNull(lock);
        final String redisKey = createRedisKey(clazz, identifier);
        final String redisLockId = jedisOps.get(redisKey);
        if (!lock.equals(redisLockId)) {
            return false;
        }
        Long result = jedisOps.del(redisKey);
        if (result != 1L) {
            throw new BridgeServiceException("Lock not released.");
        }
        return true;
    }

    private String createRedisKey(Class<?> clazz, String identifier) {
        String key = identifier + RedisKey.SEPARATOR + clazz.getCanonicalName();
        return RedisKey.LOCK.getRedisKey(key);
    }

    private void expire(final String redisKey, final int expireInSeconds) {
        Long result = jedisOps.expire(redisKey, expireInSeconds);
        if (result != 1L) {
            // Try to recover by deleting the key
            jedisOps.del(redisKey);
            throw new BridgeServiceException("Lock expiration not set.");
        }
    }
}
