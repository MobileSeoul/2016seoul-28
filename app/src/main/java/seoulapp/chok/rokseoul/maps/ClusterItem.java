package seoulapp.chok.rokseoul.maps;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by JIEUN(The CHOK) on 2016-09-17.
 */

public class ClusterItem implements com.google.maps.android.clustering.ClusterItem{

    private final LatLng mPosition;
    private String sightN;
    private String contents;

    public ClusterItem(double lat, double lng, String sightN, String contents){
        mPosition = new LatLng(lat, lng);
        this.sightN = sightN;
        this.contents = contents;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public String getSightN(){ return sightN;}

    public String getContents(){ return contents;}


}
