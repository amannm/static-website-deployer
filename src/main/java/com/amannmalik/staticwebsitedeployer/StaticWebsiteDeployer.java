package com.amannmalik.staticwebsitedeployer;

public class StaticWebsiteDeployer {

    private String contentsUrl;
    private String destinationBucket;
    private String destinationKey;

    public void setContentsUrl(String contentsUrl) {
        this.contentsUrl = contentsUrl;
    }

    public void setDestinationBucket(String destinationBucket) {
        this.destinationBucket = destinationBucket;
    }

    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
    }

    public void execute() {
        System.out.println("execution started");

        System.out.println("execution success");
    }

}
