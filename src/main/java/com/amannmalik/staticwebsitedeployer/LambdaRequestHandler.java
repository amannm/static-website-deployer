package com.amannmalik.staticwebsitedeployer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

public class LambdaRequestHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null) {
            throw new RuntimeException("environment variable 'WEBHOOK_SECRET' is required");
        }

        String bucket = System.getenv("DESTINATION_BUCKET");
        if (bucket == null) {
            throw new RuntimeException("environment variable 'DESTINATION_BUCKET' is required");
        }

        String key = System.getenv("DESTINATION_BUCKET_KEY");
        if (key == null) {
            key = "";
        }

        StaticWebsiteDeployer deployer = new StaticWebsiteDeployer();
        deployer.setDestinationBucket(bucket);
        deployer.setDestinationKey(key);

        extractPushEvent(input, secret, deployer);


    }

    private static void extractPushEvent(InputStream input, String secret, StaticWebsiteDeployer deployer) {

        JsonObject object;
        try (JsonReader parser = Json.createReader(input)) {
            object = parser.readObject();
        } catch (JsonParsingException e) {
            throw new RuntimeException("malformed request", e);
        }

        if (!object.containsKey("headers")) {
            throw new RuntimeException();
        }
        JsonObject headers = object.getJsonObject("headers");

        if (!headers.containsKey("X-Hub-Signature")) {
            throw new RuntimeException();
        }
        String signature = headers.getString("X-Hub-Signature");

        if (!object.containsKey("body")) {
            throw new RuntimeException();
        }
        String bodyString = object.getString("body");

        if (!validateEvent(signature, bodyString, secret)) {
            throw new RuntimeException("invalid signature");
        }

        JsonObject body;
        try (JsonReader parser = Json.createReader(new StringReader(bodyString))) {
            body = parser.readObject();
        } catch (JsonParsingException e) {
            throw new RuntimeException("malformed body", e);
        }

        if (!body.containsKey("ref")) {
            throw new RuntimeException();
        }
        String ref = body.getString("ref");
        if (!"refs/heads/master".equals(ref)) {
            //TODO: not an exception
            throw new RuntimeException("non-master push");
        }

        if (!body.containsKey("repository")) {
            throw new RuntimeException();
        }
        JsonObject repository = body.getJsonObject("repository");
        if (!repository.containsKey("contents_url")) {
            throw new RuntimeException("missing contents_url");
        }
        String contentsUrl = repository.getString("contents_url");

        deployer.setContentsUrl(contentsUrl);
    }

    private static boolean validateEvent(String signature, String body, String secret) {
        //TODO: this
        return true;
    }


}