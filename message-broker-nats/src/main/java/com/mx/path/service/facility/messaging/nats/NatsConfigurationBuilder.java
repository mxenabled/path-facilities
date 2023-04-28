package com.mx.path.service.facility.messaging.nats;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.mx.path.core.common.lang.Strings;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import io.nats.client.Options;

@SuppressFBWarnings
public class NatsConfigurationBuilder {

  private final NatsConfiguration configuration;

  public NatsConfigurationBuilder(NatsConfiguration configuration) {
    this.configuration = configuration;
  }

  public final Options buildNATSConfiguration() {
    if (configuration == null) {
      throw new NatsMessageBrokerConfigurationException("Nats configuration not provided");
    }

    if (!configuration.isEnabled()) {
      throw new NatsMessageBrokerConfigurationException("Nats configuration disabled");
    }

    String[] natsServers = configuration.getServers().split(",");
    if (!configuration.isTlsDisabled() && Strings.isNotBlank(configuration.getTlsCaCertPath()) && Strings.isNotBlank(configuration.getTlsClientCertPath()) && Strings.isNotBlank(configuration.getTlsClientKeyPath())) {
      return new Options.Builder()
          .servers(natsServers)
          .sslContext(getSslContext())
          .maxReconnects(-1)
          .build();
    } else {
      return new Options.Builder()
          .servers(natsServers)
          .maxReconnects(-1)
          .build();
    }
  }

  // Private

  private SSLContext getSslContext() {
    SSLContext context = null;
    try {
      //Create our certs and key converters to go from bouncycastle to java
      JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
      JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();

      //Load the certs and keys
      X509Certificate tlsCaCert = certConverter.getCertificate((X509CertificateHolder) readPemObjectFromFile(configuration.getTlsCaCertPath()));
      X509Certificate tlsClientCert = certConverter.getCertificate((X509CertificateHolder) readPemObjectFromFile(configuration.getTlsClientCertPath()));
      KeyPair tlsClientKey = keyConverter.getKeyPair((PEMKeyPair) readPemObjectFromFile(configuration.getTlsClientKeyPath()));

      //Setup the CA cert
      KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      caKeyStore.load(null, null);
      caKeyStore.setCertificateEntry("ca-certificate", tlsCaCert);
      TrustManagerFactory trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManager.init(caKeyStore);

      //Setup the cert / key pair
      KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      clientKeyStore.load(null, null);
      clientKeyStore.setCertificateEntry("certificate", tlsClientCert);
      Certificate[] certificatesArray = new Certificate[] { tlsClientCert };
      char[] emptyPassword = new char[] {};
      clientKeyStore.setKeyEntry("private-key", tlsClientKey.getPrivate(), emptyPassword, certificatesArray);
      KeyManagerFactory keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManager.init(clientKeyStore, emptyPassword);

      //Create ssl context
      context = SSLContext.getInstance("TLSv1.2");
      context.init(keyManager.getKeyManagers(), trustManager.getTrustManagers(), null);
    } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException e) {
      throw new NatsMessageBrokerConfigurationException("Failed to load nats SSL certificates", e);
    }
    return context;
  }

  private Object readPemObjectFromFile(String path) {
    try (PEMParser pemParser = new PEMParser(new FileReader(path))) {
      return pemParser.readObject();
    } catch (FileNotFoundException e) {
      throw new NatsMessageBrokerConfigurationException("Unable to create Nats SSL Context - File not found " + path, e);
    } catch (IOException e) {
      throw new NatsMessageBrokerConfigurationException("Unable to create Nats SSL Context - Cannot read file " + path, e);
    }
  }
}
