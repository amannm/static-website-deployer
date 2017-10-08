package com.amannmalik.staticwebsitedeployer;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ServiceGateway {

    public static String pullStringFromS3(String bucket, String key) {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
            S3Object o = s3.getObject(bucket, key);
            S3ObjectInputStream s3is = o.getObjectContent();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = s3is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            s3is.close();
            return result.toString(StandardCharsets.UTF_8.name());
        } catch (AmazonServiceException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
