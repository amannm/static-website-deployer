package com.amannmalik.staticwebsitedeployer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.time.Instant;
import java.util.*;

public class LambdaRequestHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {

        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null) {
            throw new RuntimeException("environment variable 'WEBHOOK_SECRET' is required");
        }

        String path = System.getenv("REPOSITORY_PATH");
        if (path == null) {
            path = "";
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
        deployer.setRepositoryPath(path);

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


        Map<String, TreeMap<Long, String>> changes = new HashMap<String, TreeMap<Long, String>>();

        if (!body.containsKey("commits")) {
            throw new RuntimeException();
        }
        JsonArray commits = body.getJsonArray("commits");
        if (commits.isEmpty()) {
            //TODO: not an exception
            throw new RuntimeException("no commits");
        }
        commits.stream().map(v -> (JsonObject) v).forEach(c -> {
            long timestamp = Instant.parse(c.getString("timestamp")).toEpochMilli();
            if (c.containsKey("added")) {
                c.getJsonArray("added").stream().map(v -> (JsonString) v).map(JsonString::getString).forEach(s -> {
                    changes.computeIfAbsent(s, k -> new TreeMap<>()).put(timestamp, "added");
                });
                c.getJsonArray("modified").stream().map(v -> (JsonString) v).map(JsonString::getString).forEach(s -> {
                    changes.computeIfAbsent(s, k -> new TreeMap<>()).put(timestamp, "modified");
                });
                c.getJsonArray("removed").stream().map(v -> (JsonString) v).map(JsonString::getString).forEach(s -> {
                    changes.computeIfAbsent(s, k -> new TreeMap<>()).put(timestamp, "delete");
                });
            }
        });

        Set<String> deleteFiles = new HashSet<>();
        Set<String> putFiles = new HashSet<>();

        changes.forEach((k, v) -> {
            String lastOperation = v.lastEntry().getValue();
            if ("removed".equals(lastOperation)) {
                if (v.size() > 1) {
                    String firstOperation = v.firstEntry().getValue();
                    if (!"added".equals(firstOperation)) {
                        deleteFiles.add(k);
                    } else {
                        //added and deleted in same push, no change
                    }
                } else {
                    deleteFiles.add(k);
                }
            } else {
                putFiles.add(k);
            }
        });

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
        SignatureVerifier verifier = new SignatureVerifier(secret);
        return verifier.verify(body, signature);
    }


}