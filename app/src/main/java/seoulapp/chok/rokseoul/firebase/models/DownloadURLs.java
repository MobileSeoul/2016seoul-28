package seoulapp.chok.rokseoul.firebase.models;

/**
 * Created by SeongSik Choi (The CHOK) on 2016. 10. 13..
 */

public class DownloadURLs {
    private String url;
    private String thumbnail;
    private String direction;
    private String placeName;
    private String fileName;
    private int azimuth;
    private int pitch;
    private int roll;
    private String createdTime;

    public DownloadURLs(){

    }
    public DownloadURLs(String createdTime, String fileName, String placeName, String direction, String url, String thumbnail, int azimuth, int pitch, int roll){
        this.createdTime = createdTime;
        this.direction = direction;
        this.url = url;
        this.thumbnail = thumbnail;
        this.placeName = placeName;
        this.fileName = fileName;
        this.azimuth = azimuth;
        this.pitch = pitch;
        this.roll = roll;
    }
    public String getUrl(){ return url; }
    public String getThumbnail(){ return thumbnail; }
    public String getDirection() { return direction; }
    public String getPlaceName() { return placeName;}
    public String getFileName() { return fileName;}
    public Integer getAzimuth() { return azimuth; }
    public Integer getPitch() { return pitch; }
    public Integer getRoll() { return roll; }
    public String getCreatedTime() { return createdTime; }

}
