#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Before building the docker image, first build and make a Spark distribution following
# the instructions in http://spark.apache.org/docs/latest/building-spark.html or
# download it http://spark.apache.org/downloads.html. It is compatible with spark 2.4.x versions.
# If this docker file is being used in the context of building your images from a Spark
# distribution, the docker build command should be invoked from the top level directory
# of the Spark distribution. E.g.:
# cp Dockerfile spark_release/spark_ubi_dockerfile/Dockerfile
# cd spark_release/
# docker build --file ./spark_ubi_dockerfile/Dockerfile -t scrapcodes/spark:v2.4.x-ubi7-ibm-sdk .

ARG spark_uid=185

FROM registry.access.redhat.com/ubi7/ubi
USER root
LABEL maintainer = "Prashant Sharma"

RUN yum update --disableplugin=subscription-manager -y && rm -rf /var/cache/yum
RUN yum install --disableplugin=subscription-manager glibc -y && rm -rf /var/cache/yum
RUN mkdir -p /opt/spark/work-dir && \
        touch /opt/spark/RELEASE && \
        rm /bin/sh && \
        ln -sv /bin/bash /bin/sh && \
        echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
        chgrp root /etc/passwd && chmod ug+rw /etc/passwd


ENV TINI_VERSION="v0.19.0"
ENV IBM_SDK_VER=8.0.6.10
ENV SYSTEM_ARCH=x86_64
ENV IBM_SDK_VER2=8.0-6.10

# ADD Tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
RUN chmod +x /tini && mv /tini /usr/bin/tini

# ADD IBM SDK 8 for specified architecture.
# By downloading/running this docker image you agree to the license http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?popup=Y&la_formnum=&li_formnum=L-SMKR-AVSEUH&title=IBM%20SDK,%20Java%20Technology%20Edition,%20Version%208&accepted_url=http://public.dhe.ibm.com/ibmdl/export/pub/systems/cloud/runtimes/java/8.0.6.10/linux/x86_64/ibm-java-sdk-8.0-6.10-x86_64-archive.bin
RUN mkdir /ibmsdk
ADD http://public.dhe.ibm.com/ibmdl/export/pub/systems/cloud/runtimes/java/${IBM_SDK_VER}/linux/${SYSTEM_ARCH}/ibm-java-sdk-${IBM_SDK_VER2}-${SYSTEM_ARCH}-archive.bin /ibmsdk/

RUN echo 'LICENSE_ACCEPTED=TRUE' >> /ibmsdk/install.properties
RUN echo "USER_INSTALL_DIR=/ibmsdk/ibm-java-${SYSTEM_ARCH}-80" >> /ibmsdk/install.properties
RUN cd /ibmsdk && chmod +x ./ibm-java-sdk-${IBM_SDK_VER2}-${SYSTEM_ARCH}-archive.bin && \
 ./ibm-java-sdk-${IBM_SDK_VER2}-${SYSTEM_ARCH}-archive.bin -f /ibmsdk/install.properties -i silent && \
 rm ./ibm-java-sdk-${IBM_SDK_VER2}-${SYSTEM_ARCH}-archive.bin

ENV JAVA_HOME=/ibmsdk/ibm-java-${SYSTEM_ARCH}-80/
# Enable FIPS mode in ibm-sdk
RUN sed -i 's/security.provider.1.*/security.provider.1=com.ibm.crypto.fips.provider.IBMJCEFIPS/g' $JAVA_HOME/jre/lib/security/java.security
RUN sed -i 's/security.provider.2.*/security.provider.2=com.ibm.crypto.plus.provider.IBMJCEPlusFIPS/g' $JAVA_HOME/jre/lib/security/java.security
RUN sed -i 's/security.provider.3.*/security.provider.3=com.ibm.jsse2.IBMJSSEProvider2/g' $JAVA_HOME/jre/lib/security/java.security
RUN sed -i 's/security.provider.4.*/security.provider.4=com.ibm.crypto.provider.IBMJCE/g' $JAVA_HOME/jre/lib/security/java.security
RUN sed -i 's/\(security.provider.[5-9].*\)/#\1/g' $JAVA_HOME/jre/lib/security/java.security
RUN sed -i 's/\(security.provider.1[0-9].*\)/#\1/g' $JAVA_HOME/jre/lib/security/java.security

COPY jars /opt/spark/jars
COPY bin /opt/spark/bin
COPY sbin /opt/spark/sbin
COPY kubernetes/dockerfiles/spark/entrypoint.sh /opt/
COPY examples /opt/spark/examples
COPY kubernetes/tests /opt/spark/tests
COPY data /opt/spark/data

ENV SPARK_HOME /opt/spark

WORKDIR /opt/spark/work-dir

ENTRYPOINT [ "/opt/entrypoint.sh" ]

# Specify the User that the actual main process will run as
USER ${spark_uid}
