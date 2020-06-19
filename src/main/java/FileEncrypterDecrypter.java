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

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

class FileEncrypterDecrypter {

    private final SecretKey secretKey;
    private final Cipher cipher;
    private final int ivSize;
    FileEncrypterDecrypter(SecretKey secretKey, String cipher, int ivSize) throws NoSuchPaddingException, NoSuchAlgorithmException {
        this.secretKey = secretKey;
        this.cipher = Cipher.getInstance(cipher);
        this.ivSize = ivSize;
    }

    void encrypt(String content, String fileName) throws InvalidKeyException, IOException {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] iv = cipher.getIV();

        FileOutputStream fileOut = new FileOutputStream(fileName);
        CipherOutputStream cipherOut = new CipherOutputStream(fileOut, cipher);
        fileOut.write(iv);
        cipherOut.write(content.getBytes());
    }

    String decrypt(String fileName) throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        String content;

        try (FileInputStream fileIn = new FileInputStream(fileName)) {
            byte[] fileIv = new byte[ivSize];
            fileIn.read(fileIv);
            // cipher.init(Cipher.DECRYPT_MODE, secretKey, (AlgorithmParameterSpec)null, sr); //, new IvParameterSpec(fileIv));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(fileIv));

            try (
                    CipherInputStream cipherIn = new CipherInputStream(fileIn, cipher);
                    InputStreamReader inputReader = new InputStreamReader(cipherIn);
                    BufferedReader reader = new BufferedReader(inputReader)
            ) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }
                content = sb.toString();
            }

        }
        return content;
    }
}