package com.byteBazar.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public SqsAsyncClient sqsAsyncClient(PaymentProperties props) {
        PaymentProperties.Messaging m = props.getMessaging();
        SqsAsyncClientBuilder builder = SqsAsyncClient.builder()
            .region(Region.of(m.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        if (m.getEndpoint() != null && !m.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(m.getEndpoint()));
        }
        return builder.build();
    }
}
