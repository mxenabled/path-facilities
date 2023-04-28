package com.mx.path.service.facility.store.redis;

import java.util.Set;
import java.util.function.Function;

import lombok.Getter;

import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.store.Store;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;

/**
 * Redis key-value store
 */
public class RedisStore implements Store {

  private static final String PUT_UNSUPPORTED_OPERATION = "Put operations must have an expiration";

  @Getter
  private final RedisStoreConfiguration configuration;
  @Getter
  private StatefulRedisConnection<String, String> connection;

  public final void setConnection(StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  public RedisStore(@Configuration RedisStoreConfiguration redisStoreConfiguration) {
    this.configuration = redisStoreConfiguration;
  }

  @Override
  public final void delete(String key) {
    safeCall("delete", (conn) -> {
      conn.sync().del(key);
      return Void.TYPE;
    });
  }

  @Override
  public final void deleteSet(String key, String value) {
    safeCall("deleteSet", (conn) -> {
      conn.sync().srem(key, value);
      return Void.TYPE;
    });
  }

  @Override
  public final String get(String key) {
    return safeCall("get", (conn) -> {
      return conn.sync().get(key);
    });
  }

  @Override
  public final Set<String> getSet(String key) {
    return safeCall("getSet", (conn) -> {
      return conn.sync().smembers(key);
    });
  }

  @Override
  public final boolean inSet(String key, String value) {
    return safeCall("inSet", (conn) -> {
      return conn.sync().sismember(key, value);
    });
  }

  @Override
  public final void put(String key, String value, long expirySeconds) {
    safeCall("put", (conn) -> {
      conn.sync().set(key, value);
      conn.sync().expire(key, expirySeconds);
      return Void.TYPE;
    });
  }

  @Override
  public final void put(String key, String value) {
    throw new RedisStoreUnsupportedException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final void putSet(String key, String value, long expirySeconds) {
    safeCall("putSet", (conn) -> {
      conn.sync().sadd(key, value);
      conn.sync().expire(key, expirySeconds);
      return Void.TYPE;
    });
  }

  @Override
  public final void putSet(String key, String value) {
    throw new RedisStoreUnsupportedException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final boolean putIfNotExist(String key, String value, long expirySeconds) {
    return safeCall("putIfNotExist", (conn) -> {
      boolean result = conn.sync().setnx(key, value);
      if (result) {
        conn.sync().expire(key, expirySeconds);
      }

      return result;
    });
  }

  @Override
  public final boolean putIfNotExist(String key, String value) {
    throw new RedisStoreUnsupportedException(PUT_UNSUPPORTED_OPERATION);
  }

  // Private

  final synchronized StatefulRedisConnection<String, String> buildConnection() {
    try {
      ClientResources resources = ClientResources.builder()
          .ioThreadPoolSize(configuration.getIoThreadPoolSize())
          .computationThreadPoolSize(configuration.getComputationThreadPoolSize())
          .build();

      RedisClient redisClient = RedisClient.create(resources, new RedisURI(configuration.getHost(), configuration.getPort(), configuration.getTimeout()));

      // RESP3 executes a HELLO command to discover the protocol before executing any commands made by the client.
      // This can cause issues if we are communicating over a proxy and the proxy doesn't speak RESP3. For now, it is safer
      // to default to RESP2 until RESP3 becomes more normalized.
      ClientOptions options = ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
      redisClient.setOptions(options);

      return redisClient.connect();
    } catch (RedisException e) {
      throw new RedisStoreConnectionException("An error occurred connecting to redis", e);
    }
  }

  private <T> T safeCall(String operation, Function<StatefulRedisConnection<String, String>, T> runnable) {
    try {
      return runnable.apply(connection());
    } catch (RedisStoreConnectionException e) {
      throw e;
    } catch (RedisException e) {
      throw new RedisStoreOperationException("Redis error occurred on " + operation, e);
    } catch (RuntimeException e) {
      throw new RedisStoreOperationException("Unknown exception thrown by redis on " + operation, e);
    }
  }

  private StatefulRedisConnection<String, String> connection() {
    if (connection == null) {
      connection = buildConnection();
    }

    return connection;
  }
}
