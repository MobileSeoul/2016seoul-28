package seoulapp.chok.rokseoul.util;

import java.util.ArrayList;

import seoulapp.chok.rokseoul.maps.models.PlaceDoodleInfo;

/**
 * Created by SeongSik Choi (The CHOK) on 2016. 10. 27..
 */

public class TranslationUtil {
    public TranslationUtil(){

    }

    public static String transPlaceNameENtoKOR(String placeNameEN){
        String transKOR;
        if(placeNameEN.equals("royalpalace")) transKOR = "경복궁";
        else if(placeNameEN.equals("nseoultower")) transKOR = "N서울타워";
        else if(placeNameEN.equals("sungnyemun")) transKOR = "숭례문";
        else if(placeNameEN.equals("deoksugung")) transKOR = "덕수궁";
        else if(placeNameEN.equals("seoul national memorial board")) transKOR = "국립서울현충원";
        else if(placeNameEN.equals("lotteworld")) transKOR = "롯데월드";
        else if(placeNameEN.equals("gwanghwamun")) transKOR = "광화문광장";
        else transKOR = "미등록";
        return transKOR;
    }
    public static String transPlaceNameKORtoEN(String placeNameEN){
        String transEN;
        if(placeNameEN.equals("경복궁")) transEN = "royalpalace";
        else if(placeNameEN.equals("N서울타워")) transEN = "nseoultower";
        else if(placeNameEN.equals("숭례문")) transEN = "sungnyemun";
        else if(placeNameEN.equals("덕수궁")) transEN = "deoksugung";
        else if(placeNameEN.equals("국립서울현충원")) transEN = "seoul national memorial board";
        else if(placeNameEN.equals("롯데월드")) transEN = "lotteworld";
        else if(placeNameEN.equals("광화문광장")) transEN = "gwanghwamun";
        else transEN = "미등록";
        return transEN;
    }

    public static String transPlaceNameSightIDtoEN(String sightID){
        String transEN;
        if(sightID.equals("126508")) transEN = "royalpalace";
        else if(sightID.equals("126535")) transEN = "nseoultower";
        else if(sightID.equals("128162")) transEN = "sungnyemun";
        else if(sightID.equals("126509")) transEN = "deoksugung";
        else if(sightID.equals("126521")) transEN = "seoul national memorial board";
        else if(sightID.equals("126498")) transEN = "lotteworld";
        else if(sightID.equals("775394")) transEN = "gwanghwamun";
        else transEN = "미등록";
        return transEN;
    }

    public static Integer transPlaceIndex(ArrayList<PlaceDoodleInfo> placeDoodleInfoList, String sightNameEN){
        int index = -1;
        int i = 0;
        for(PlaceDoodleInfo placeDoodleInfo : placeDoodleInfoList) {
            if(placeDoodleInfo.placeName.equals(sightNameEN)) index = i;
            i++;
        }
        return index;
    }
    public static boolean possibilityTransPlaceIndex(ArrayList<PlaceDoodleInfo> placeDoodleInfoList, String sightNameEN){
        boolean check = false;
        for(PlaceDoodleInfo placeDoodleInfo : placeDoodleInfoList) {
            if(placeDoodleInfo.placeName.equals(sightNameEN)) check=true;
        }
        return check;
    }

    public static boolean possibilitiyQRPlace(String sightID){
        if(sightID.equals("126508")) return true;
        else if(sightID.equals("126535"))return true;
        else if(sightID.equals("128162"))return true;
        else if(sightID.equals("126509"))return true;
        else if(sightID.equals("126521"))return true;
        else if(sightID.equals("126498"))return true;
        else if(sightID.equals("775394"))return true;
        else return false;
    }
}
