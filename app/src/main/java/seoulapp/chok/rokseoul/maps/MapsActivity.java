package seoulapp.chok.rokseoul.maps;

/**
 * Created by JIEUN (The CHOK)
 * Modified by SeongSik Choi (The CHOK) on 2016. 10. 28..
 */

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import seoulapp.chok.rokseoul.R;
import seoulapp.chok.rokseoul.maps.getdata.GetSightInform;
import seoulapp.chok.rokseoul.maps.models.MarkerData;
import seoulapp.chok.rokseoul.maps.models.PlaceDoodleInfo;
import seoulapp.chok.rokseoul.util.TranslationUtil;

public class MapsActivity extends Fragment
        implements OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    public GoogleMap mMap;
    private ClusterManager<ClusterItem> mClusterManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionDenied = false;

    private DatabaseReference mPlaceDatabase;
    private ValueEventListener mPlaceValueEventListener;

    private CheckBox sCheck;
    private CheckBox fCheck;

    private ArrayList<LatLng> latLngTrans;
    private ArrayList<PlaceDoodleInfo> placeDoodleInfoList;
    private ArrayList<MarkerData> markerDataListOfFestival;
    private ArrayList<MarkerData> markerDataListOfSight;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_maps, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MapFragment mapFragment;
        if (Build.VERSION.SDK_INT >Build.VERSION_CODES.KITKAT) {
            mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        } else {
            mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        }

        Log.d("SDK", "SDK version : " + Build.VERSION.SDK_INT + "\nmapFragment : " + mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        sCheck= (CheckBox) view.findViewById(R.id.radio_sight);
        fCheck = (CheckBox) view.findViewById(R.id.radio_festival);

        placeDoodleInfoList = new ArrayList<PlaceDoodleInfo>();
        markerDataListOfFestival = new ArrayList<MarkerData>();
        markerDataListOfSight = new ArrayList<MarkerData>();
        latLngTrans = new ArrayList<LatLng>();

        setPlaceDatabase();
        if(markerDataListOfFestival == null || markerDataListOfFestival.isEmpty()) setClusterData_event();

        sCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mMap.clear();
                mClusterManager.clearItems();
                latLngTrans.clear();
                if(isChecked){
                    if(fCheck.isChecked()){
                        setClusterManager_sight();
                        setClusterManager_event();
                    }else{
                        setClusterManager_sight();
                    }
                }
                else{
                    if(fCheck.isChecked()){
                        setClusterManager_event();
                    }
                }
            }
        });
        fCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mMap.clear();
                mClusterManager.clearItems();
                latLngTrans.clear();
                if(isChecked){
                    if(sCheck.isChecked()){
                        setClusterManager_sight();
                        setClusterManager_event();
                    }else{
                        setClusterManager_event();
                    }
                }
                else{
                    if(sCheck.isChecked()){
                        setClusterManager_sight();
                    }else{
                    }
                }
            }
        });
    }


    //FragmentActivity는 다른 액티비티에서 포함할 수 없대
    //따라서 fragment로만 extend해야함

    @Override
    public void onResume() {
        super.onResume();
        mPlaceDatabase.addListenerForSingleValueEvent(mPlaceValueEventListener);
        Log.d("MapsActivity" , "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mPlaceDatabase != null){
            mPlaceDatabase.removeEventListener(mPlaceValueEventListener);
            Log.d("MapsActivity" , "onPause");
        }
        try {
            mMap.clear();
            placeDoodleInfoList.clear();
            mClusterManager.clearItems();
            markerDataListOfSight.clear();  // pause 시 클리어 안하면 엑티비티 변환 시 마다 클러스터가 추가됨
        }catch(Exception e){
            Log.d("MapsActivity", "Cluster's clearItem error"+e);
        }

    }

    /**
     * @param googleMap
     * 이곳에 리스너들 셋팅
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MapsActivity" , "onMapReady");
        mMap = googleMap;
        mMap.setPadding(0, 200, 0, 220);
        LatLng seoul = new LatLng(37.5666805, 126.9784147);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10));

        mClusterManager = new ClusterManager<ClusterItem>(getActivity(), mMap);
        mMap.setOnCameraChangeListener(mClusterManager);
        //mMap.setOnCameraIdleListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        //자세히보기를 하려면 다음 리스너필요
        mMap.setOnInfoWindowClickListener(mClusterManager);

        //클러스터 눌렀을 때 확대하기
        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<ClusterItem>() {
            @Override
            public boolean onClusterClick(Cluster<ClusterItem> cluster) {
                LatLngBounds.Builder builder = LatLngBounds.builder();
                for (ClusterItem item : cluster.getItems()) {
                    builder.include(item.getPosition());
                }

                // Get the LatLngBounds
                final LatLngBounds bounds = builder.build();

                // Animate camera to the bounds
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;

            }
        });
        mClusterManager.setRenderer(new ClusterWithIcon(getActivity().getApplicationContext(), mMap, mClusterManager));

        setCustomWindow();

        enableMyLocation();

    }

    /**
     * 데이터베이스 셋팅(oncreate꺼 빼온거
     */
    private void setPlaceDatabase(){
        mPlaceDatabase = FirebaseDatabase.getInstance().getReference().child("QRPlace");
        mPlaceValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("mapsac" , "onDataChange");
                try {
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        PlaceDoodleInfo placeDoodleInfo = new PlaceDoodleInfo(snapshot.getKey(),
                                Integer.parseInt(snapshot.child("doodles").getValue().toString()));

                        placeDoodleInfoList.add(placeDoodleInfo);
                        Log.d("MapsActivity",
                                "placename(realtime-maps): " + placeDoodleInfoList.get(placeDoodleInfoList.size()-1).placeName
                                        +" / placeDoodles : " + placeDoodleInfoList.get(placeDoodleInfoList.size()-1).placeDoodles);
                    }

                    if(markerDataListOfSight == null || markerDataListOfSight.isEmpty()) setClusterData_sight();

                }catch (Exception e){
                    Log.e("MapsActivity", "DB Error"+e);
                }
                try {
                    mMap.clear();
                    mClusterManager.clearItems();
                    latLngTrans.clear();
                }catch (Exception e){
                    Log.e("MapsActivity", e.toString());
                }
                if(sCheck.isChecked()) {
                    if (fCheck.isChecked()) {
                        setClusterManager_sight();
                        setClusterManager_event();
                    } else {
                        setClusterManager_sight();
                    }
                }else if(fCheck.isChecked()){
                    setClusterManager_event();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void setClusterManager_sight(){
        for(MarkerData markerData : markerDataListOfSight){
            transLatLng(markerData);
        }
        Log.d("MapsActivity" , "cluster");
        mClusterManager.cluster();
    }
    private void setClusterManager_event(){
        for (MarkerData markerData : markerDataListOfFestival){
            transLatLng(markerData);
        }
        mClusterManager.cluster();
    }

    private void transLatLng(MarkerData markerData){

        LatLng newll = new LatLng(markerData.mapX, markerData.mapY);

        if(latLngTrans.contains(newll)){
            Double a = newll.longitude;
            Double b = newll.latitude;
            a= a+0.0001d;
            b = b+0.0001d;
            newll = new LatLng(b, a);
        }

        latLngTrans.add(0, newll);

        makeMarker(newll.latitude, newll.longitude, markerData.name, markerData.allData);

    }

    private void makeMarker(double lat, double lng, String name, String allData){

        ClusterItem offsetItem = new ClusterItem(lat, lng, name, allData);
        mClusterManager.addItem(offsetItem);
    }

    /******
     * 페스티벌 데이터셋
     */
    private void setClusterData_event() {
        /*
        * 1. input되는 행사의 시작일, 종료일은 sysdate로!
        * */

        String curTime = new SimpleDateFormat("yyyyMMdd").format(new Date());
        ArrayList<String> eventList = new ArrayList<>();

        try {
            StringBuilder urlBuilder = new StringBuilder("http://api.visitkorea.or.kr/openapi/service/rest/KorService/searchFestival");
            urlBuilder.append("?" + URLEncoder.encode("ServiceKey", "UTF-8") +getResources().getString(R.string.tour_api_key));/*Service Key*/
            urlBuilder.append("&" + URLEncoder.encode("contentTypeId", "UTF-8") + "=" + URLEncoder.encode("15", "UTF-8")); /*타입 ID*/
            urlBuilder.append("&" + URLEncoder.encode("eventStartDate", "UTF-8") + "=" + URLEncoder.encode(curTime, "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("eventEndDate", "UTF-8") + "=" + URLEncoder.encode(curTime, "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("arrange", "UTF-8") + "=" + URLEncoder.encode("A", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("areaCode", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("listYN", "UTF-8") + "=" + URLEncoder.encode("Y", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("pageNo", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("numOfRows", "UTF-8") + "=" + URLEncoder.encode("100", "UTF-8"));//보통 이벤트가 60개 넘는 일이 없어서..100개로  끊어놓음..
            urlBuilder.append("&" + URLEncoder.encode("MobileOS", "UTF-8") + "=" + URLEncoder.encode("ETC", "UTF-8"));
            urlBuilder.append("&" + URLEncoder.encode("MobileApp", "UTF-8") + "=" + URLEncoder.encode("RokSEOUL", "UTF-8")); /*어플이름*/

            Document output = new GetSightInform().execute(urlBuilder.toString()).get();

            // 2. x좌표, y좌표, 이벤트명, 행사시작일자, 행사종료일자를 가져온다.

            NodeList nodeList = output.getElementsByTagName("item");

            for(int i=0; i<nodeList.getLength(); i++) {
                Node nameNode = nodeList.item(i);
                Element nameElmnt = (Element) nameNode;

                eventList.add(i,nameElmnt.getElementsByTagName("title").item(0).getChildNodes().item(0).getNodeValue());            //축제명
                eventList.add(i+1,nameElmnt.getElementsByTagName("mapy").item(0).getChildNodes().item(0).getNodeValue());           //X좌표
                eventList.add(i+2,nameElmnt.getElementsByTagName("mapx").item(0).getChildNodes().item(0).getNodeValue());           //Y좌표
                eventList.add(i+3,nameElmnt.getElementsByTagName("eventstartdate").item(0).getChildNodes().item(0).getNodeValue()); //행사시작일자
                eventList.add(i+4,nameElmnt.getElementsByTagName("eventenddate").item(0).getChildNodes().item(0).getNodeValue());   //행사종료일자

                //받아온 것들을 5파라미터로 addFestivalItem해준다
                String festName = "\n"+eventList.get(i);
                Double festMapX = Double.parseDouble(eventList.get(i+1));
                Double festMapY = Double.parseDouble(eventList.get(i+2));
                String festStartDate = eventList.get(i+3);
                String festEndDate = eventList.get(i+4);
                System.out.println("*********축제, 좌표, 일정출력 "+i+". : "+festName+", "+festMapX+", "+festMapY+", "+festStartDate+", "+festEndDate);


                String allData = festStartDate+"~"+festEndDate;
                MarkerData markerDataOfFestival = new MarkerData(festMapX, festMapY, festName, allData);

                markerDataListOfFestival.add(markerDataOfFestival);

            }

        }catch (Exception e){
            System.out.println("에러 : "+e.toString());
        }

    }

    /**
    * 유적지마커 데이터셋
    * */
    private void setClusterData_sight() {

        ArrayList<String> latlngList = new ArrayList<>();

        latlngList.add("37.5801859611");
        latlngList.add("126.9767235747");
        latlngList.add("경복궁");

        latlngList.add("37.5516394747");
        latlngList.add("126.9876206116");
        latlngList.add("N서울타워");

        latlngList.add("37.560157");
        latlngList.add("126.975281");
        latlngList.add("숭례문");

        latlngList.add("37.565830");
        latlngList.add("126.975200");
        latlngList.add("덕수궁");

        latlngList.add("37.500168");
        latlngList.add("126.973221");
        latlngList.add("국립서울현충원");

        latlngList.add("37.511107");
        latlngList.add("127.098199");
        latlngList.add("롯데월드");

        latlngList.add("37.574051");
        latlngList.add("126.976786");
        latlngList.add("광화문광장");


        //위도, 경도, 관광지 명을 파라미터로보낸다 setCustomWindow

        for (int i = 0; i < latlngList.size(); i+=3) {
            double lat = Double.parseDouble(latlngList.get(i));
            double lng = Double.parseDouble(latlngList.get(i + 1));
            String sightName = latlngList.get(i+2);
            addItems(lat, lng, sightName);
        }
    }

    private void addItems(double lat, double lng, String sightN) {

        //GetSightInform class로부터 공공데이터 받아온 후 여기서 xml파싱한다

        try {
            //double lat, lng;
            StringBuilder urlBuilder = new StringBuilder("http://api.visitkorea.or.kr/openapi/service/rest/KorService/detailIntro"); /*URL*/

            /*필요 파라미터*/
            urlBuilder.append("?" + URLEncoder.encode("ServiceKey", "UTF-8") + getResources().getString(R.string.tour_api_key));/*Service Key*/
            urlBuilder.append("&" + URLEncoder.encode("contentTypeId", "UTF-8") + "=" + URLEncoder.encode("12", "UTF-8")); /*타입 ID*/

            if(sightN.equals("경복궁")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("126508", "UTF-8"));
            }else if(sightN.equals("N서울타워")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("126535", "UTF-8"));
            } else if(sightN.equals("숭례문")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("128162", "UTF-8"));
            }else if(sightN.equals("덕수궁")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("126509", "UTF-8"));
            }else if(sightN.equals("국립서울현충원")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("126521", "UTF-8"));
            }else if(sightN.equals("롯데월드")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("126498", "UTF-8"));
            }else if(sightN.equals("광화문광장")){
                urlBuilder.append("&" + URLEncoder.encode("contentId", "UTF-8") + "=" + URLEncoder.encode("775394", "UTF-8"));
            }
             /*콘텐츠ID  경복궁 : 126508, N서울타워 :126535*/

            urlBuilder.append("&" + URLEncoder.encode("MobileOS", "UTF-8") + "=" + URLEncoder.encode("ETC", "UTF-8")); /*OS 구분*/
            urlBuilder.append("&" + URLEncoder.encode("MobileApp", "UTF-8") + "=" + URLEncoder.encode("RokSEOUL", "UTF-8")); /*어플이름*/
            urlBuilder.append("&" + URLEncoder.encode("introYN", "UTF-8") + "=" + URLEncoder.encode("Y", "UTF-8")); /*뭔지 모르겠는데 인트로*/

            Document output = new GetSightInform().execute(urlBuilder.toString()).get();
            /*
            * XML 파싱
            * */

            String allData = "";
            NodeList nodeList = output.getElementsByTagName("item");
            Node nameNode = nodeList.item(0);
            Element nameElmnt = (Element) nameNode;
            String sightsID = nameElmnt.getElementsByTagName("contentid").item(0).getChildNodes().item(0).getNodeValue();

            allData+= "이용시간\n"+
                    nameElmnt.getElementsByTagName("usetime").item(0).getChildNodes().item(0).getNodeValue()+"\n";
            try {
                allData += "\n쉬는날\n" +
                        nameElmnt.getElementsByTagName("restdate").item(0).getChildNodes().item(0).getNodeValue() + "\n";
            }catch(Exception e){
                Log.e("MapsActivity", "쉬는날"+e.toString());
                allData += "\n쉬는날\n" + "연중무휴\n";
            }
            allData+= "\n주차시설\n"+
                    nameElmnt.getElementsByTagName("parking").item(0).getChildNodes().item(0).getNodeValue()+"\n";

            if(sightsID.equals("126508") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))){
                sightN = "\n경복궁";
                allData+= String.valueOf("\n\n"+ placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID)))
                        .placeDoodles);
            }else if(sightsID.equals("126535") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))){
                sightN = "\nN서울타워(남산타워)";
                allData+= String.valueOf("\n\n"+ placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("N서울타워")))
                        .placeDoodles);
            }else if(sightsID.equals("128162") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))) {
                sightN = "\n숭례문";
                allData += String.valueOf("\n\n" + placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("숭례문")))
                        .placeDoodles);
            }else if(sightsID.equals("126509") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))) {
                sightN = "\n덕수궁";
                allData += String.valueOf("\n\n" + placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("덕수궁")))
                        .placeDoodles);
            }else if(sightsID.equals("126521") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))) {
                sightN = "\n국립서울현충원";
                allData += String.valueOf("\n\n" + placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("국립서울현충원")))
                        .placeDoodles);
            }else if(sightsID.equals("126498") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))) {
                sightN = "\n롯데월드";
                allData += String.valueOf("\n\n" + placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("롯데월드")))
                        .placeDoodles);
            }else if(sightsID.equals("775394") && TranslationUtil.possibilityTransPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameSightIDtoEN(sightsID))) {
                sightN = "\n광화문광장";
                allData += String.valueOf("\n\n" + placeDoodleInfoList
                        .get(TranslationUtil.transPlaceIndex(placeDoodleInfoList, TranslationUtil.transPlaceNameKORtoEN("광화문광장")))
                        .placeDoodles);
            }else {
                sightN = "\n"+TranslationUtil.transPlaceNameENtoKOR(TranslationUtil.transPlaceNameSightIDtoEN(sightsID));
                allData +="\n\n0";
            }

            allData = allData.replaceAll("<br (/)>|<br>","");

            MarkerData markerDataOfSight = new MarkerData(lat, lng, sightN, allData);
            markerDataListOfSight.add(markerDataOfSight);

        }catch (Exception e){
            Log.e("MapsActivity" , "sightN data set Error" +e);
        }

    }


    /*
    * 커스터마이징 한 윈도우어댑터시작-----------
    * */
    private void setCustomWindow() {

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            View v = getActivity().getLayoutInflater().inflate(R.layout.infowindow, null);
            TextView tvName = (TextView) v.findViewById(R.id.tv_name);
            TextView tvContent = (TextView) v.findViewById(R.id.tv_content);
            TextView tvDoodleCount = (TextView) v.findViewById(R.id.tv_doodleCount);

            @Override
            public View getInfoWindow(Marker marker) {

                tvName.setText(marker.getTitle());

                if(marker.getSnippet().contains("\n\n\n")){
                    tvContent.setText("\n"+marker.getSnippet().substring(0, marker.getSnippet().indexOf("\n\n\n")+1));
                    tvDoodleCount.setText(marker.getSnippet().substring(marker.getSnippet().indexOf("\n\n\n")+3));
                }else{
                    tvContent.setText("\n"+marker.getSnippet());
                    tvDoodleCount.setVisibility(View.GONE);
                    v.findViewById(R.id.tv_doodleSub).setVisibility(View.GONE);
                }
                return v;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String title = marker.getTitle().replaceFirst("\n","");
                String url = "https://m.search.naver.com/search.naver?&where=m&sm=mtp_hty&query="+title;
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);

            }
        });
    }



    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission((AppCompatActivity) getActivity(), LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(getActivity(), "현재 사용자의 위치를 표시합니다", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        } else {
            mPermissionDenied = true;
        }
    }


}
