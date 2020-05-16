package liquibase.change.ext.s3.change;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

public class S3ClientDelegator {

    private static S3Client s3Client;

    private static final Map<GetObjectRequest, String> cache = new HashMap<>();

    public static InputStream getObject(String bucket, String key) {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
        // Liquibase requests it multiple times. So, we need an internal cache to avoid multiple requests to S3
        if (cache.containsKey(request)) {
            String content = cache.get(request);
            return new ByteArrayInputStream(content.getBytes());
        }
        ResponseInputStream<GetObjectResponse> response = getS3Client().getObject(request);

        try {
            String content = IoUtils.toUtf8String(response);
            cache.put(request, content);
            return new ByteArrayInputStream(content.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = createS3Client();
        }
        return s3Client;
    }

    private static S3Client createS3Client() {
        S3ClientBuilder s3ClientBuilder = S3Client.builder();
        String region = System.getProperty("aws.region");
        if (region != null) {
            s3ClientBuilder.region(Region.of(region));
        } else {
            s3ClientBuilder.region(Region.US_EAST_1);
        }
        return s3ClientBuilder.build();
    }
}
