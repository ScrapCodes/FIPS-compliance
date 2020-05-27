# `Keep your own Key` and `FIPS compliance` with Apache Spark.

Now that Cloud HSM is widely available, In other words, a Hardware Security Module available for Rent.
What does it mean for us? How can we leverage this in our applications.

A Cloud HSM is a machine with specialized cryptographic processors or accelerators, that makes cryptography 
both fast and secure. We have all heard about a very popular cryptanalysis attack, where an attacker extracts
RSA keys, by simply observing the CPU FAN noise[1]. Since a dedicated Cryptographic co-processor helps in
processing all the data related to cryptography, the CPU noise tracking becomes irrelevant. So this helps makes,
Cloud HSM very secure for enterprise usage. It also helps take off load from CPU, for processing ciphering and
deciphering of data and thus makes cryptography faster. 

There is another benefit of dedicated hardware for cryptography, i.e. they generate and store the keys on the device
 and keep them very safe. In this way, keys are not stored in the regular persistent storage like a system Hard drive 
or a NAS or SAN. In fact, keys are generated and stored on these devices and they have the ability to detect
any attempt to tamper with the device, including a physical damage and auto erase the keys[2]. In this way, any
 data that was accessible using these keys become inaccessible to anyone.

So, given the value of storing a key very securely and reliably. Now can we access the system with a single key, 
i.e. all the data encryption and authentication is achieved by a single or a group of keys. All of which is handled
by a Cloud HSM instance. In this post, we will consider some salient features of `Keep your own key`, for large
scale data processing engine like Apache Spark.

TODO: add instructions for using Spark on Openshift with key as mounted secret.

#References

1. RSA Key Extraction via Low-Bandwidth Acoustic Cryptanalysis. Daniel et al. 
[link](https://phys.org/news/2013-12-trio-rsa-encryption-keys-noise.html)
2. https://en.wikipedia.org/wiki/Hardware_security_module
3. https://www.openshift.com