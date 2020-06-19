/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.FileReader;


public class CipherSuite {

    public static void main(String[] args) {
        try {
            SecretKey secretKey = new SecretKeySpec("testing1testing1".getBytes(), "AES");
            FileEncrypterDecrypter fileEncrypterDecrypter =
                    new FileEncrypterDecrypter(secretKey, "AES/CBC/NoPadding", 16);
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            StringBuilder sb = new StringBuilder();
            String temp = reader.readLine();
            while (temp != null) {
                sb.append(temp);
                temp = reader.readLine();
            }
            fileEncrypterDecrypter.encrypt(sb.toString(), args[1]);
            String textContent = fileEncrypterDecrypter.decrypt(args[1]);
            assert (textContent.equals(sb.toString()));
            System.out.println(textContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}