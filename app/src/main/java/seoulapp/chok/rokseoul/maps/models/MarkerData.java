package seoulapp.chok.rokseoul.maps.models;

/**
 * Created by choiseongsik on 2016. 10. 24..
 */

public class MarkerData {
    public String name;
    public Double mapX;
    public Double mapY;
    public String allData;

    public MarkerData(Double mapX, Double mapY, String name, String allData){
        this.mapX = mapX;
        this.mapY = mapY;
        this.name = name;
        this.allData = allData;
    }
}
