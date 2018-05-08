package enums;

public enum PaperlessRestApiUrls {

    HOST("https://paperless.com.ua"),
    LOGIN("/api/login"),
    UPLOAD("/upload"),
    SHARE("/api/share/"),
    RESOURCE("/api/resource/"),
    TAG("/api/resource/tag/"),
    WITHSIGN("/api/resource/withsign/");

    private final String url;

    PaperlessRestApiUrls(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }



}
