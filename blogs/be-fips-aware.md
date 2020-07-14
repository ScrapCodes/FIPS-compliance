
# Common misconceptions with respect to FIPS mode enabled environment.

1. A FIPS mode enabled environment, will make sure that all application that run on it are also compliant.

    This is not always true, as in some cases, a software makes no guarantees about user applications, compliance.
    For example, in case of IBM SDK, when in FIPS mode - It tries to switch to FIPS approved algorithms whenever
    possible or throws an Exception, when user executes a code that tries to use any crypto algorithm - not in
    compliance with FIPS standard. But, it does not make any guarantee that user code will also be FIPS
    compliant, therefore it is end user's responsibility to be aware about FIPS standard and security requirements
    of the application.

2. Does an application developed for FIPS enabled IBM SDK/Oracle JDK will not work on the same version of the JDK/SDK
without FIPS enabled? 

    Application will work just fine, in fact enabling FIPS limits the available algorithms. That is, the algos,
    which are not FIPS approved are disabled for use.
    
3. Why do we need FIPS in the First place? What does it bring along with it? Is it just a set of rules?

    Today, a few Operating Systems and libraries have a FIPS mode available. 
    Actually, FIPS is just a compliance standard, following all
     the issued guidelines makes your application also compliant. FIPS is definitely not a special cryptographic suite,
     which is not available unless you have the FIPS mode enabled on your OS.
     
     It is required by certain government/non-government bodies as a standard, to make their computing environment
     secured against a well recognised security standard. This protects their computers from being hacked and their
     users' personal information and other classified information from being leaked to the hackers. Hackers are often
     quick to exploit any security vulnerabilities, so it is important to regularly update the FIPS standard and also to
     adhere to latest available guidelines from FIPS. 

# An application developer should be aware of FIPS, here are some common pitfalls.
_This list cannot be exhaustive, and thus it is important to be fully aware of the standard and
computer security in general._

### 1. Use of a non compliant unsupported cipher or simply a non encrypted transport (e.g. http).
Downloading jars, or other sub-modules for your application via http, could lead to MIM attack, an
attacker can simply intercept the traffic and see the jars and other source files to learn about your
source code. Therefore, FIPS has strict guidance available for approved ciphersuite for
transport level security(e.g. TLS or SSL). 
There are some https ciphersuites that are not in compliance with FIPS standard.

For example, list of all the ciphersuites that use RSA key exchange supported by openssl:

```shell script
$> openssl ciphers -v 'kRSA'
TLS_AES_256_GCM_SHA384  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(256) Mac=AEAD
TLS_CHACHA20_POLY1305_SHA256 TLSv1.3 Kx=any      Au=any  Enc=CHACHA20/POLY1305(256) Mac=AEAD
TLS_AES_128_GCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(128) Mac=AEAD
TLS_AES_128_CCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESCCM(128) Mac=AEAD
AES256-GCM-SHA384       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(256) Mac=AEAD
AES256-CCM8             TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM8(256) Mac=AEAD
AES256-CCM              TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM(256) Mac=AEAD
ARIA256-GCM-SHA384      TLSv1.2 Kx=RSA      Au=RSA  Enc=ARIAGCM(256) Mac=AEAD
AES128-GCM-SHA256       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(128) Mac=AEAD
AES128-CCM8             TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM8(128) Mac=AEAD
AES128-CCM              TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM(128) Mac=AEAD
ARIA128-GCM-SHA256      TLSv1.2 Kx=RSA      Au=RSA  Enc=ARIAGCM(128) Mac=AEAD
AES256-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA256
CAMELLIA256-SHA256      TLSv1.2 Kx=RSA      Au=RSA  Enc=Camellia(256) Mac=SHA256
AES128-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA256
CAMELLIA128-SHA256      TLSv1.2 Kx=RSA      Au=RSA  Enc=Camellia(128) Mac=SHA256
NULL-SHA256             TLSv1.2 Kx=RSA      Au=RSA  Enc=None      Mac=SHA256
AES256-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA1
CAMELLIA256-SHA         SSLv3 Kx=RSA      Au=RSA  Enc=Camellia(256) Mac=SHA1
AES128-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA1
SEED-SHA                SSLv3 Kx=RSA      Au=RSA  Enc=SEED(128) Mac=SHA1
CAMELLIA128-SHA         SSLv3 Kx=RSA      Au=RSA  Enc=Camellia(128) Mac=SHA1
IDEA-CBC-SHA            SSLv3 Kx=RSA      Au=RSA  Enc=IDEA(128) Mac=SHA1
RC4-SHA                 SSLv3 Kx=RSA      Au=RSA  Enc=RC4(128)  Mac=SHA1
DES-CBC3-SHA            SSLv3 Kx=RSA      Au=RSA  Enc=3DES(168) Mac=SHA1
NULL-SHA                SSLv3 Kx=RSA      Au=RSA  Enc=None      Mac=SHA1
NULL-MD5                SSLv3 Kx=RSA      Au=RSA  Enc=None      Mac=MD5 
```

And all the ciphersuites that use RSA key exchange, including TLS v1.2, and are allowed in FIPS mode.
(Run on openssl(with fips enabled) on a RHEL 8.x server in FIPS mode)
```shell script
$> openssl version
OpenSSL 1.1.1c FIPS  28 May 2019
```

```shell script
$> openssl ciphers -v 'kRSA+FIPS'
TLS_AES_256_GCM_SHA384  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(256) Mac=AEAD
TLS_CHACHA20_POLY1305_SHA256 TLSv1.3 Kx=any      Au=any  Enc=CHACHA20/POLY1305(256) Mac=AEAD
TLS_AES_128_GCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(128) Mac=AEAD
TLS_AES_128_CCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESCCM(128) Mac=AEAD
AES256-GCM-SHA384       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(256) Mac=AEAD
AES256-CCM              TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM(256) Mac=AEAD
AES128-GCM-SHA256       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(128) Mac=AEAD
AES128-CCM              TLSv1.2 Kx=RSA      Au=RSA  Enc=AESCCM(128) Mac=AEAD
AES256-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA256
AES128-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA256
AES256-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA1
AES128-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA1
DES-CBC3-SHA            SSLv3 Kx=RSA      Au=RSA  Enc=3DES(168) Mac=SHA1
```

The above comparision shows, that in FIPS mode, there are certain ciphersuites disabled, e.g. RC4-SHA or NULL-MD5 etc.

This means, when an application is using HTTPS, we still need to make sure, it is using the approved
ciphersuite, for both key exchange and encryption. Having FIPS enabled environment may be helpful.

For example:

A spark job using an external service for model scoring of input data, can reveal the details of the data to both,
any one who intercepts the message (i.e. MIM attack) and the owner of the Model scoring service itself.

### 2. Using or accessing a public cloud object storage and all the implications.

A cloud object storage hosted on a public cloud, may store data in encrypted format, but it does not restrict the cloud 
provider from accessing the data. And this can be a matter of concern for some government/non-government bodies for storing
user data. This can be overcome by either first encrypting the data with FIPS approved algorithm,
and then storing on public cloud, never transmitting the keys. Or one can have a private cloud
instance of object store and still using the encrypted data.

A cloud object storage, fully compliant with FIPS standard may be used.

### 3. Accessing hadoop for example.

Hadoop has both encrypted and non encrypted modes of operation. If in encrypted mode, all the network i/o and disk storage
can be encrypted with a FIPS approved ciphersuite. 
[HDFS Transparent encryption](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/TransparentEncryption.html)


### 4. Storing data on a non-encrypted/non-fips compliant NFS.

NFS can be made FIPS compliant by turning on the FIPS mode on it, more details available with NFS provider.
[RHEL documentation for Securing NFS](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/7/html/storage_administration_guide/s1-nfs-security)
