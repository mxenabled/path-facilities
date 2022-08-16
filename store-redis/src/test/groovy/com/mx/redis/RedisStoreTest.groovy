package com.mx.redis

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

import com.mx.common.collections.ObjectMap
import com.mx.common.session.SessionInfo

import org.mockito.Mockito

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

import spock.lang.Specification

class RedisStoreTest extends Specification implements WithMockery {

  StatefulRedisConnection<String, String> connection
  RedisCommands<String, String> commands
  RedisStore subject
  ObjectMap configurations

  def setup() {
    connection = mock(StatefulRedisConnection.class, Mockito.RETURNS_DEEP_STUBS)
    commands = mock(RedisCommands.class)
    when(connection.sync()).thenReturn(commands)

    subject = new RedisStore(configurations)
    subject.setConnection(connection)
  }

  def "setters/getters"() {
    when:
    def conn = mock(StatefulRedisConnection.class)
    subject.setConnection(conn)

    then:
    subject.getConnection() == conn
    subject.getConfigurations() == configurations
  }

  def "delete"() {
    when:
    subject.delete("id")

    then:
    verify(commands).del("id") || true
  }

  def "deleteSet"() {
    when:
    subject.deleteSet("id", "value")

    then:
    verify(commands).srem("id", "value") || true
  }

  def "get"() {
    when:
    when(commands.get("key1")).thenReturn("value1")

    then:
    subject.get("key1") == "value1"
  }

  def "getSet"() {
    given:
    Set<String> set = new HashSet<>()
    set.add("key1")

    when:
    when(commands.smembers("key1")).thenReturn(set)

    then:
    subject.getSet("key1") == set
  }

  def "inSet"() {
    when:
    when(commands.sismember("key1", "value1")).thenReturn(true)

    then:
    subject.inSet("key1", "value1")
  }

  def "put"() {
    when:
    subject.put("key1", "value1", 60)

    then:
    verify(commands).set("key1", "value1") || true
    verify(commands).expire("key1", 60) || true
  }

  def "put with two arguments"() {
    when:
    subject.put("key1", "value1")

    then:
    thrown(UnsupportedOperationException)
  }

  def "putSet"() {
    when:
    subject.putSet("key1", "value1", 60)

    then:
    verify(commands).sadd("key1", "value1") || true
    verify(commands).expire("key1", 60) || true
  }

  def "putSet with two arguments"() {
    when:
    subject.putSet("key1", "value1")

    then:
    thrown(UnsupportedOperationException)
  }

  def "putIfNotExist with two arguments"() {
    when:
    subject.putIfNotExist("key1", "value1")

    then:
    thrown(UnsupportedOperationException)
  }

  def "setIfNotExist when does not exist"() {
    when:
    when(commands.setnx("key1", "value1")).thenReturn(true) // The key was set
    subject.putIfNotExist("key1", "value1", 60)

    then:
    verify(commands).expire("key1", 60) || true
  }

  def "setIfNotExist when does exist"() {
    when:
    when(commands.setnx("key1", "value1")).thenReturn(false) // The key already set
    subject.putIfNotExist("key1", "value1", 60)

    then:
    verify(commands, never()).expire("key1", 60) || true
  }

  def "setIfNotExist with expiration when does not exist"() {
    when:
    when(commands.setnx("key1", "value1")).thenReturn(true) // The key was set
    subject.putIfNotExist("key1", "value1", 12)

    then:
    verify(commands).expire("key1", 12) || true
  }

  def "setIfNotExist with expiration when does exist"() {
    when:
    when(commands.setnx("key1", "value1")).thenReturn(false) // The key already set
    subject.putIfNotExist("key1", "value1", 12)

    then:
    verify(commands, never()).expire("key1", 12) || true
  }

  def "status"() {
    when:
    when(commands.ping()).thenReturn("OK")

    then:
    subject.status() == "OK"
  }
}
