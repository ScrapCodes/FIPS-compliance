# FIPS Compliance for Apache Spark.

As developer community in enterprise companies like, health care, finance, telcom, retail, and manufacturing, are
 considering Apache Spark for their data processing, they are also looking for solutions to harden their enterprise
  security, being FIPS compliant comes first to the mind. In principle, this guide can will help any java based application
  to meet FIPS compliance.
  
  For more information on specification related to FIPS compliance please visit: https://csrc.nist.gov/publications/fips
  
  Since this specification is periodically revised by the government, it is important to update this document to meet
  the currently applicable compliance specification.

Cloud HSM and Keep your own key, take enterprise security to another level. We will briefly cover some relevant detail
about these.

_Note: A country specific export guidance for cryptographic software may be applicable, please review your country
 specific export guidance, before proceeding with FIPS setup._
 
_Note: This document is suggestive of various configuration and address common development related issues, and does not
guarantee a FIPS compliance._

## Setting up the environment for enabling FIPS for spark.

### Configuring RHEL 7.7 to be FIPS compliant.

Red Hat enterprise linux, can be setup to be FIPS compliant. The instructions and other subtle details can be found
 here: 
 [External link - RHEL website.](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/7/html/security_guide/chap-federal_standards_and_regulations)

## Configuring JVM

This step is required if, PKCS11 support has to be enabled. By default, this provider is not registered in the JVM on
most systems.

### Step 1.
 
### Install Mozilla NSS libraries.
  
This step is required because, java does not come with native implementation for the PKCS11 standard. But it comes
 with a wrapper, which can be linked with a supported linked library.

Currently, [Mozilla NSS](https://developer.mozilla.org/en-US/docs/Mozilla/Projects/NSS) is the only widely used open source
   implementation available. This library has a FIPS enabled mode, but it is not a FIPS certified library.

1.1. Usually this library comes pre-installed on a RHEL system. Verify it's existence by:        
            ```$ readelf -d /lib64/libnss3.so```
                   
1.2. However, if the latest version of this library is desired and to enable FIPS module at the compile time - a
 build from source is required. Note that, FIPS module for NSS library is already installed in RHEL 7.8 with FIPS mode enabled.
 So, it is not necessary to build from source.

  1.2.1. Download the latest version of Mozilla NSS library from their website and following the instructions provided.
  
  1.2.2. For RHEL 7, following set of commands for building the library were helpful.
  
  ```shell script
    $ subscription-manager repos --enable rhel-server-rhscl-7-rpms
    $ yum install -y devtoolset-8
    $ yum install -y zlib-devel gcc-c++
    $ make -C nss nss_build_all USE_64=1 NSS_FORCE_FIPS=1 NSS_ENABLE_WERROR=0
```
After executing the above commands, build is generated under the directory `nss_$version/dist/`. Locate the lib folder
containing `.so` libraries. Usually it is of the form, `nss_$version/dist/Linux3.10_x86_64_cc_glibc_PTH_64_DBG.OBJ/lib`.

```shell script
    $ mkdir ~/nsslibs/
    $ cp nss_$version/dist/Linux3.10_x86_64_cc_glibc_PTH_64_DBG.OBJ/lib/* ~/nsslibs/
```
### Step 2.

#### Configure JVM to use the NSS libs.
For this blog, oracle jdk 1.8 was used for testing the steps. The same steps might work with openjdk as well.

2.1 Create PKCS11 configuration file for JVM as follows.
    
        name=NSS
        # if pre-installed NSS library is used, following would suffice.
        nssLibraryDirectory=/lib64
        # if custom build nss library is used, uncomment the following and point it to the correct lib location.
        #nssLibraryDirectory=/home/username/nsslibs
        nssDbMode=noDb
        attributes=compatibility
        showInfo=true
        nssModule=fips

Save the above file as pkcs11.cfg and note its location.

2.2 Edit JVM security providers to include PKCS11 module.
    
Usually located at, `jdk1.8.0_251/jre/lib/security` as `java.security` file. Locate the section for list of providers,
as follows:

            #
            # List of providers and their preference orders (see above):
            #
            
            security.provider.1=sun.security.provider.Sun
            security.provider.2=sun.security.rsa.SunRsaSign
            security.provider.3=sun.security.ec.SunEC
            security.provider.4=com.sun.net.ssl.internal.ssl.Provider
            security.provider.5=com.sun.crypto.provider.SunJCE
            security.provider.6=sun.security.jgss.SunProvider
            security.provider.7=com.sun.security.sasl.Provider
            security.provider.8=org.jcp.xml.dsig.internal.dom.XMLDSigRI
            security.provider.9=sun.security.smartcardio.SunPCSC
            # Enable by adding following row
            security.provider.10=sun.security.pkcs11.SunPKCS11 /path_to/pkcs11.cfg

Add the last row as shown above, and specify the correct path to `pkcs11.cfg` file created in the previous step. 
    
2.3 To verify the setup was done properly.

```Java

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

public class ListCryptoProviders {
    public static void main(String[] args) {
        try {
            Provider p[] = Security.getProviders();
            for (int i = 0; i < p.length; i++) {
                if (p[i].toString().startsWith("SunPKCS11")) {
                    System.out.println(p[i]);
                    for (Enumeration e = p[i].keys(); e.hasMoreElements(); )
                        System.out.println("\t" + e.nextElement());
                }
            }
        } catch (Exception e) {
            System.out.println("Provider is not properly configured.");
            e.printStackTrace();
        }
    }
}

```

2.3.1 Save above file as, ListCryptoProviders.java and compile as:
    
        javac ListCryptoProviders.java

2.3.2 Run the test as:
        
        Java ListCryptoProviders
        
Should produce the output containing:

        SunPKCS11-NSS version 1.8
        ...

## Configuring Spark

1. Setup the correct JVM.

        export JAVA_HOME=<path_to_JDK>
            
2. Configure Spark security.
        
                    
           # FIPS compliant spark configuration example.
        
        #general    
        spark.serializer    org.apache.spark.serializer.KryoSerializer
        spark.authenticate  true
        spark.authenticate.secret   1234567890123456
        
        #i/o
        spark.io.encryption.enabled true
        spark.io.encryption.keySizeBits 256
        spark.io.encryption.keygen.algorithm AES
        spark.io.encryption.commons.config.secure.random.java.algorithm        PKCS11
        spark.io.encryption.commons.config.cipher.transformation       AES/CTR/PKCS5Padding
        
        #n/w
        spark.network.crypto.enabled   true
        spark.network.crypto.saslFallback 	false
        spark.network.crypto.keyLength    256
        spark.network.crypto.keyFactoryAlgorithm        PBKDF2WithHmacSHA256
        spark.network.crypto.config.secure.random.java.algorithm        PKCS11
        spark.network.crypto.config.cipher.transformation       AES/CTR/PKCS5Padding
        spark.network.crypto.keyFactoryAlgorithm    PBKDF2WithHmacSHA256
        
        #SSL
        spark.ssl.enabled true
        spark.ssl.keyPassword changeit!
        spark.ssl.keyStorePassword changeit!
        spark.ssl.keyStore /path/to/keystore
        spark.ssl.keyStoreType pkcs12
        spark.ssl.enabledAlgorithms    TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
        spark.ssl.needClientAuth     false
        spark.ssl.protocol TLSv1.2

In order to complete SSL setup, a keystore is required, it can be created as follows, if it is not already available.

        $ $JAVA_HOME/bin/keytool -genkey -storetype pkcs12 -keyalg RSA -alias spark -keystore /path/to/keystore

Specify the above as `$SPARK_HOME/conf/spark-defaults.conf`


_Note: It is recommended to use the latest release of Spark at all points in time, to include the fixes to all the
critical CVE(s). http://spark.apache.org/security.html_

## Testing the setup.

Test the setup using, Spark standalone mode. Follow the instruction here: 

http://spark.apache.org/docs/latest/spark-standalone.html

## Special notes for IBM SDK 8

[IBM SDK 8 documentation](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0)

Detailed documentation of IBM SDK FIPS Cryptographic module validation program. 
[IBM SDK FIPS compliance](https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/JCEFIPSDocs/ibmjcefips.html)

### Configuring JVM.

1. Setup the correct JVM.

        export JAVA_HOME=<path_to_IBM SDK>
            
2. Add the IBMJCEFIPS provider, to use IBM SDK support for FIPS.

  a) Edit the section on security providers in $JAVA_HOME/jre/lib/security/java.security file.
    
        #
        # List of providers and their preference orders (see above):
        #
        security.provider.1=com.ibm.crypto.fips.provider.IBMJCEFIPS 
        security.provider.2=com.ibm.crypto.plus.provider.IBMJCEPlusFIPS
        security.provider.3=com.ibm.jsse2.IBMJSSEProvider2
        security.provider.4=com.ibm.crypto.provider.IBMJCE
        #security.provider.5=com.ibm.security.jgss.IBMJGSSProvider
        #security.provider.6=com.ibm.security.cert.IBMCertPath
        #security.provider.6=com.ibm.security.sasl.IBMSASL
        
  b) Edit spark configuration to include:

        #IBM SDK specific options, to use FIPS approved provider.
        spark.driver.extraJavaOptions "-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
        spark.executor.extraJavaOptions "-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
  
  c) If using the standalone mode, all the spark demons need to be started with above configurations.
  
        export SPARK_MASTER_OPTS="-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
        export SPARK_WORKER_OPTS="-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
        
### Configuring Spark.

1. How to troubleshoot _OpenSSL internal error, assertion failed: FATAL FIPS SELFTEST FAILURE_ ?

Error snippet:


        fips.c(145): OpenSSL internal error, assertion failed: FATAL FIPS SELFTEST FAILURE
        JVMDUMP039I Processing dump event "abort", detail "" at 2020/05/18 02:02:13 - please wait.
        JVMDUMP032I JVM requested System dump using '/home/user/sparkcore.20200518.020213.28807.0001.dmp' in response to an event
        JVMDUMP010I System dump written to /home/user/spark/core.20200518.020213.28807.0001.dmp
        JVMDUMP032I JVM requested Java dump using '/home/user/spark/javacore.20200518.020213.28807.0002.txt' in response to an event


On examining the dump, the most interesting thread is,

        3XMTHREADINFO      "netty-rpc-connection-0" J9VMThread:0x00000000029CD600, omrthread_t:0x00007FBC7C2453B8, java/lang/Thread:0x00000000C07724F8, state:R, prio=5
        3XMJAVALTHREAD            (java/lang/Thread getId:0x34, isDaemon:true)
        3XMTHREADINFO1            (native thread ID:0x70E4, native priority:0x5, native policy:UNKNOWN, vmstate:R, vm thread flags:0x00000020)
        3XMTHREADINFO2            (native stack address range from:0x00007FBCDCFCE000, to:0x00007FBCDD00E000, size:0x40000)
        3XMCPUTIME               CPU usage total: 0.315326548 secs, current category="Application"
        3XMHEAPALLOC             Heap bytes allocated since last GC cycle=739928 (0xB4A58)
        3XMTHREADINFO3           Java callstack:
        4XESTACKTRACE                at org/apache/commons/crypto/cipher/OpenSslNative.initIDs(Native Method)
        4XESTACKTRACE                at org/apache/commons/crypto/cipher/OpenSsl.<clinit>(OpenSsl.java:95)
        4XESTACKTRACE                at org/apache/commons/crypto/cipher/OpenSslCipher.<init>(OpenSslCipher.java:57)
        4XESTACKTRACE                at sun/reflect/NativeConstructorAccessorImpl.newInstance0(Native Method)
        4XESTACKTRACE                at sun/reflect/NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:83)
        4XESTACKTRACE                at sun/reflect/DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:57(Compiled Code))
        4XESTACKTRACE                at java/lang/reflect/Constructor.newInstance(Constructor.java:437)
        4XESTACKTRACE                at org/apache/commons/crypto/utils/ReflectionUtils.newInstance(ReflectionUtils.java:88)
        4XESTACKTRACE                at org/apache/commons/crypto/cipher/CryptoCipherFactory.getCryptoCipher(CryptoCipherFactory.java:160)
        4XESTACKTRACE                at org/apache/spark/network/crypto/AuthEngine.initializeForAuth(AuthEngine.java:214)
    ......
    
It is trying to access the native openssl library via a JNA method. My guess here is, JNA library is not compiled for Open J9.

Fortunately, we do not need to do that, or try to debug the native C code, we turn off this optional module and use IBM SDK's
support for FIPS. Which is also the recommended way in the case of IBM SDK, as it opens up the possibility of using IBM's cryptographic h/w
accelerators. 

So we can fix this by using JAVA mode for cipher suites, as follows.

Configure spark with following:

For networking,

        spark.network.crypto.config.secure.random.classes   org.apache.commons.crypto.random.JavaCryptoRandom
        spark.network.crypto.config.cipher.classes  org.apache.commons.crypto.cipher.JceCipher

For disk i/o,

        spark.io.encryption.commons.config.cipher.classes   org.apache.commons.crypto.cipher.JceCipher
        spark.io.encryption.commons.config.secure.random.classes    org.apache.commons.crypto.random.JavaCryptoRandom
        

2. Why IBM SDK, uses it's own internal implementation of secure random which is FIPS compliant?

> Caller-provided IBMSecureRandom number generators are ignored
>   To be FIPS 140-2 compliant during key pair generation, version 1.8 ignores caller-provided random number generators
>   and instead uses the internal FIPS-approved SHA2DRBG generator. This update does not require changes to your application code. 

[More details here](https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/JCEFIPSDocs/ibmjcefips.html)

3. IBM SDK supported SSL ciphersuite and details of selected ciphersuite and it's FIPS compliance.

The following OpenSSL wiki discusses all the FIPS compliant ciphersuites.
[OpenSSL documentation - FIPS compliance and various TLS v1.2 Ciphersuites](https://wiki.openssl.org/index.php/FIPS_mode_and_TLS)

IBM SDK, supported list of ciphersuites.
[IBM SDK supported ciphersuites](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/jsse2Docs/ciphersuites.html)

So, we used: `SSL_ECDHE_RSA_WITH_AES_256_GCM_SHA384` which is both FIPS approved and supported by IBM SDK.
 Also, supported by many browsers including `firefox` and `safari`.

4. Setting up the keystore with IBM SDK.

        
        $JAVA_HOME/bin/keytool -genkeypair -storetype jks -keyalg RSA -alias spark -keystore `pwd`/keystore4 -storepass changeit! -keypass changeit!

 
### Putting it all together, sample spark configuration for IBM SDK which is FIPS compliant.

    # FIPS compliant spark configuration example.
    
    #IBM SDK specific options, to use FIPS approved provider.
    spark.driver.extraJavaOptions "-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
    spark.executor.extraJavaOptions "-Dcom.ibm.jsse2.usefipsProviderName=IBMJCEFIPS -Dssl.SocketFactory.provider=com.ibm.jsse2.SSLSocketFactoryImpl -Dssl.ServerSocketFactory.provider=com.ibm.jsse2.SSLServerSocketFactoryImpl"
      
    #general    
    spark.serializer    org.apache.spark.serializer.KryoSerializer
    spark.authenticate  true
    spark.authenticate.secret   1234567890123456

    #n/w
    spark.network.crypto.enabled   true
    spark.network.crypto.saslFallback   false
    spark.network.crypto.keyLength    256
    spark.network.crypto.keyFactoryAlgorithm        PBKDF2WithHmacSHA256
    
    # This is FIPS 140-2 compliant secure random implementation, that comes with IBM SDK.
    # Setting any value here has no effect at all. When FIPS mode is enabled,
    # IBM SDK choses SHA2DRBG algorithm for secure random, regardless of what user has configured.
    # See section 2, for more details.
    spark.io.encryption.commons.config.secure.random.java.algorithm  SHA2DRBG 
    
    # It is important to use JAVA classes, otherwise apache commons crypto package will try to use 
    # native openssl jna library. This library does not play well with IBM SDK. See section 1.
    spark.network.crypto.config.secure.random.classes   org.apache.commons.crypto.random.JavaCryptoRandom
    spark.network.crypto.config.cipher.classes  org.apache.commons.crypto.cipher.JceCipher
    
    spark.network.crypto.config.cipher.transformation       AES/CTR/PKCS5Padding
    spark.network.crypto.keyFactoryAlgorithm    PBKDF2WithHmacSHA256
    
    #i/o
    spark.io.encryption.enabled true
    spark.io.encryption.keySizeBits 256
    spark.io.encryption.keygen.algorithm AES
    spark.io.encryption.commons.config.secure.random.classes    org.apache.commons.crypto.random.JavaCryptoRandom
    spark.io.encryption.commons.config.secure.random.java.algorithm       SHA2DRBG
    spark.io.encryption.commons.config.cipher.classes   org.apache.commons.crypto.cipher.JceCipher
    spark.io.encryption.commons.config.cipher.transformation       AES/CTR/PKCS5Padding
    
    #SSL
    spark.ssl.enabled true
    spark.ssl.keyPassword changeit!
    spark.ssl.keyStorePassword changeit!
    spark.ssl.keyStore /path/to/keystore4
    spark.ssl.keyStoreType JKS
    spark.ssl.enabledAlgorithms   SSL_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    spark.ssl.needClientAuth        false
    spark.ssl.protocol      TLSv1.2

## Finally, can we have a docker image with all that packed in?
Yes, checkout the ubi7/ubi8 based docker images for both Spark2 and Spark3 in [docker](docker/) folder.

They are published for ready use on docker hub, as follows:

1) Based on spark 2.4.5
    
    a) scrapcodes/spark:v2.4.5-ubi7-ibm-sdk
    
    b) scrapcodes/spark:v2.4.5-ubi7-ibm-sdk-fips-mode

2) Based on spark 3.1.x

    a) scrapcodes/v3.1.0-5052d9557d-ubi7-ibm-sdk-fips
    
    b) scrapcodes/v3.1.0-5052d9557d-ubi7-ibm-sdk

# References

1. How to configure IBM SDK to use cryptographic h/w accelerators and PKCS11. [link](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/pkcs11implDocs/hardwareconfig.html)
2. Does the use of IBM SDK with FIPS mode, guarantees the FIPS compliance of application running on top of it? 
    > The property does not verify that you are using the correct protocol or cipher suites that are required for FIPS 140-2 compliance
[link](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/jsse2Docs/enablefips.html)
3. Keep your own key. [link](https://www.ibm.com/cloud/blog/announcements/keep-your-own-key-for-kubernetes-apps-with-highly-sensitive-data)