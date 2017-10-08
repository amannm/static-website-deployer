package com.amannmalik.staticwebsitedeployer;

public class StaticWebsiteDeployer {

    private String contentsUrl;
    private String destinationBucket;
    private String destinationKey;
    private String repositoryPath;

    public void setContentsUrl(String contentsUrl) {
        this.contentsUrl = contentsUrl;
    }

    public void setDestinationBucket(String destinationBucket) {
        this.destinationBucket = destinationBucket;
    }

    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }


    public void execute() {
        System.out.println("execution started");
        String rootUrl = contentsUrl.replace("{+path}", repositoryPath);
        // get json array from there, each object with property type=file has property download_url
        // otherwise type=dir needs to be navigated to
        // also name and size properties
        System.out.println("execution success");
    }

}
