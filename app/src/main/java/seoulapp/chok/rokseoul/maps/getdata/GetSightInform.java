package seoulapp.chok.rokseoul.maps.getdata;


import android.os.AsyncTask;
import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by JIEUN(The CHOK) on 2016-09-27.
 */

public class GetSightInform extends AsyncTask<String, Void, Document> {

    Document doc = null;

    @Override
        protected Document doInBackground(String... urls) {
            URL url;
            try {
                url = new URL(urls[0]);
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder(); //XML문서 빌더 객체를 생성
                doc = db.parse(new InputSource(url.openStream())); //XML문서를 파싱한다.
                doc.getDocumentElement().normalize();

            } catch (Exception e) {
                Log.d("soonsu","3");
            }
            return doc;
        }

        @Override
        protected void onPostExecute(Document document) {

        }

}
