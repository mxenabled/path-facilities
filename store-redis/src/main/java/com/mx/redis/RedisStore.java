package com.mx.redis;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

import lombok.Getter;

import com.mx.common.collections.ObjectMap;
import com.mx.common.store.Store;

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

  private static final int DEFAULT_COMPUTATION_THREAD_POOL_SIZE = 5;
  private static final int DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS = 10;
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 6379;
  private static final int DEFAULT_THREAD_POOL_SIZE = 5;
  private static final String PUT_UNSUPPORTED_OPERATION = "Put operations must have an expiration";

  @Getter
  private final ObjectMap configurations;
  @Getter
  private StatefulRedisConnection<String, String> connection;

  public final void setConnection(StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  public RedisStore(ObjectMap configurations) {
    this.configurations = configurations;
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
          .ioThreadPoolSize(configurations.getAsInteger("ioThreadPoolSize", DEFAULT_THREAD_POOL_SIZE))
          .computationThreadPoolSize(configurations.getAsInteger("computationThreadPoolSize", DEFAULT_COMPUTATION_THREAD_POOL_SIZE))
          .build();

      RedisClient redisClient = RedisClient.create(resources, new RedisURI(configurations.getAsString("host", DEFAULT_HOST), configurations.getAsInteger("port", DEFAULT_PORT), Duration.ofSeconds(configurations.getAsInteger("connectionTimeoutSeconds", DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS))));

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
