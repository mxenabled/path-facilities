package com.mx.path.service.facility.store.redis;

import java.io.File;
import java.util.Set;
import java.util.function.Function;

import lombok.Getter;

import com.mx.path.core.common.configuration.Configuration;
import com.mx.path.core.common.store.Store;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
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
  @Getter
  private StatefulRedisClusterConnection<String, String> redisClusterConnection;

  public final void setConnection(StatefulRedisConnection<String, String> connection) {
    this.connection = connection;
  }

  public final void setRedisClusterConnection(StatefulRedisClusterConnection<String, String> redisClusterConnection) {
    this.redisClusterConnection = redisClusterConnection;
  }

  public RedisStore(@Configuration RedisStoreConfiguration redisStoreConfiguration) {
    this.configuration = redisStoreConfiguration;
  }

  @Override
  public final void delete(String key) {
    if (configuration.isCluster()) {
      safeClusterCall("delete", (conn) -> {
        conn.sync().del(key);
        return Void.TYPE;
      });
    } else {
      safeCall("delete", (conn) -> {
        conn.sync().del(key);
        return Void.TYPE;
      });
    }
  }

  @Override
  public final void deleteSet(String key, String value) {
    if (configuration.isCluster()) {
      safeClusterCall("deleteSet", (conn) -> {
        conn.sync().srem(key, value);
        return Void.TYPE;
      });
    } else {
      safeCall("deleteSet", (conn) -> {
        conn.sync().srem(key, value);
        return Void.TYPE;
      });
    }
  }

  @Override
  public final String get(String key) {
    if (configuration.isCluster()) {
      return safeClusterCall("get", (conn) -> {
        return conn.sync().get(key);
      });
    } else {
      return safeCall("get", (conn) -> {
        return conn.sync().get(key);
      });
    }
  }

  @Override
  public final Set<String> getSet(String key) {
    if (configuration.isCluster()) {
      return safeClusterCall("getSet", (conn) -> {
        return conn.sync().smembers(key);
      });
    } else {
      return safeCall("getSet", (conn) -> {
        return conn.sync().smembers(key);
      });
    }
  }

  @Override
  public final boolean inSet(String key, String value) {
    if (configuration.isCluster()) {
      return safeClusterCall("inSet", (conn) -> {
        return conn.sync().sismember(key, value);
      });
    } else {
      return safeCall("inSet", (conn) -> {
        return conn.sync().sismember(key, value);
      });
    }
  }

  @Override
  public final void put(String key, String value, long expirySeconds) {
    if (configuration.isCluster()) {
      safeClusterCall("put", (conn) -> {
        conn.sync().set(key, value);
        conn.sync().expire(key, expirySeconds);
        return Void.TYPE;
      });
    } else {
      safeCall("put", (conn) -> {
        conn.sync().set(key, value);
        conn.sync().expire(key, expirySeconds);
        return Void.TYPE;
      });
    }
  }

  @Override
  public final void put(String key, String value) {
    throw new RedisStoreUnsupportedException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final void putSet(String key, String value, long expirySeconds) {
    if (configuration.isCluster()) {
      safeClusterCall("putSet", (conn) -> {
        conn.sync().sadd(key, value);
        conn.sync().expire(key, expirySeconds);
        return Void.TYPE;
      });
    } else {
      safeCall("putSet", (conn) -> {
        conn.sync().sadd(key, value);
        conn.sync().expire(key, expirySeconds);
        return Void.TYPE;
      });
    }
  }

  @Override
  public final void putSet(String key, String value) {
    throw new RedisStoreUnsupportedException(PUT_UNSUPPORTED_OPERATION);
  }

  @Override
  public final boolean putIfNotExist(String key, String value, long expirySeconds) {
    if (configuration.isCluster()) {
      return safeClusterCall("putIfNotExist", (conn) -> {
        boolean result = conn.sync().setnx(key, value);
        if (result) {
          conn.sync().expire(key, expirySeconds);
        }
        return result;
      });
    } else {
      return safeCall("putIfNotExist", (conn) -> {
        boolean result = conn.sync().setnx(key, value);
        if (result) {
          conn.sync().expire(key, expirySeconds);
        }

        return result;
      });
    }
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

  final synchronized StatefulRedisClusterConnection<String, String> buildClusterConnection() {
    try {
      ClientResources resources = ClientResources.builder()
          .ioThreadPoolSize(configuration.getIoThreadPoolSize())
          .computationThreadPoolSize(configuration.getComputationThreadPoolSize())
          .build();

      RedisURI redisUri = RedisURI.Builder.redis(configuration.getHost(), configuration.getPort())
          .withSsl(configuration.isSsl())
          .withVerifyPeer(false)
          .build();

      RedisClusterClient redisClusterClient = RedisClusterClient.create(resources, redisUri);
      SslOptions sslOptions = SslOptions.builder()
          .keyManager(new File(configuration.getCertFile()), new File(configuration.getKeyFile()), configuration.getPasswordFile().toCharArray())
          .build();
      ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
          .sslOptions(sslOptions).build();
      redisClusterClient.setOptions(clusterClientOptions);

      return redisClusterClient.connect();
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

  private <T> T safeClusterCall(String operation, Function<StatefulRedisClusterConnection<String, String>, T> runnable) {
    try {
      return (T) runnable.apply(clusterConnection());
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

  private StatefulRedisClusterConnection clusterConnection() {
    if (redisClusterConnection == null) {
      redisClusterConnection = buildClusterConnection();
    }
    return redisClusterConnection;
  }
}
