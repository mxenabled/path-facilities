package com.mx.redis;

import java.time.Duration;
import java.util.Set;

import lombok.Getter;

import com.mx.common.collections.ObjectMap;
import com.mx.common.store.Store;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
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
    connection().sync().del(key);
  }

  @Override
  public final void deleteSet(String key, String value) {
    connection().sync().srem(key, value);
  }

  @Override
  public final String get(String key) {
    return connection().sync().get(key);
  }

  @Override
  public final Set<String> getSet(String key) {
    return connection().sync().smembers(key);
  }

  @Override
  public final boolean inSet(String key, String value) {
    return connection().sync().sismember(key, value);
  }

  @Override
  public final void put(String key, String value, long expirySeconds) {
    connection().sync().set(key, value);
    connection().sync().expire(key, expirySeconds);
  }

  @Override
  public final void put(String key, String value) {
    throw new UnsupportedOperationException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final void putSet(String key, String value, long expirySeconds) {
    connection().sync().sadd(key, value);
    connection().sync().expire(key, expirySeconds);
  }

  @Override
  public final void putSet(String key, String value) {
    throw new UnsupportedOperationException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final boolean putIfNotExist(String key, String value, long expirySeconds) {
    boolean result = connection().sync().setnx(key, value);
    if (result) {
      connection().sync().expire(key, expirySeconds);
    }

    return result;
  }

  @Override
  public final boolean putIfNotExist(String key, String value) {
    throw new UnsupportedOperationException(PUT_UNSUPPORTED_OPERATION);
  }

  // Private

  private synchronized StatefulRedisConnection<String, String> buildConnection() {
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
  }

  private StatefulRedisConnection<String, String> connection() {
    if (connection == null) {
      connection = buildConnection();
    }

    return connection;
  }

}
