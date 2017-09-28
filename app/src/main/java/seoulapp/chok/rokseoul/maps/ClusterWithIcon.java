package seoulapp.chok.rokseoul.maps;

import android.content.Context;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

/**
 * Modified by JIEUN(The CHOK) on 2016-09-17.
 */

public class ClusterWithIcon extends DefaultClusterRenderer<ClusterItem> {

    public ClusterWithIcon(Context context, GoogleMap map, ClusterManager<ClusterItem> clusterManager) {
        super(context, map, clusterManager);
    }

    public void onBeforeClusterItemRendered(ClusterItem item, MarkerOptions markerOptions){
        super.onBeforeClusterItemRendered(item, markerOptions);
        markerOptions.title(item.getSightN());
        markerOptions.position(item.getPosition());
        markerOptions.snippet(item.getContents());
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        // Always render clusters.
        return cluster.getSize() > 1;
    }

}
