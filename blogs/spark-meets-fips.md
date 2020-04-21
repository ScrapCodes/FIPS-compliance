# FIPS Compliance for Apache Spark.

As developer community in enterprise companies like, health care, finance, telcom, retail, and manufacturing, are
 considering Apache Spark for their data processing, they are also looking for solutions to harden their enterprise
  security, being FIPS compliant comes first to the mind. In principle, this guide will help any java based application
  to meet FIPS compliance. 
  
  For more information on specification related to FIPS compliance please visit: https://csrc.nist.gov/publications/fips
  
  Since this specification is periodically revised by the government, it is important to update this document to meet
  the currently applicable compliance specification.

_Note: A country specific export guidance for cryptographic software may be applicable, please review your country
 specific export guidance, before proceeding with FIPS setup._
 
## Setting up the environment.

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
   implementation available. This library has a FIPS mode.

1.1. Usually this library comes pre-installed on a RHEL system. So nothing to do, we can verify it's existence by:        
            ```$ readelf -d /lib64/libnss3.so```
                   
1.2. However, if the latest version of this library is desired and also to enforce FIPS mode at the compile time. A
 build from source is required. Note that, FIPS mode can also be enabled later, if not enforced at compile time.

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
        
Would produce the output containing:

        SunPKCS11-NSS version 1.8
        ...

## Configuring Spark

1. Setup the correct JVM.

            JAVA_HOME=<path_to_JDK>
            
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

You need to create a keystore, if it is not already available.

        $ $JAVA_HOME/bin/keytool -genkey -storetype pkcs12 -keyalg RSA -alias spark -keystore /path/to/keystore

Specify the above as `$SPARK_HOME/conf/spark-defaults.conf`


_Note: It is recommended to use the latest release of Spark at all points in time, to include the fixes to all the
critical CVE(s). http://spark.apache.org/security.html_

## Testing the setup.

Test the setup using, Spark standalone mode. Follow the instruction here: 

http://spark.apache.org/docs/latest/spark-standalone.html
