import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StdChecker {

    public static StdStatus check(String std) throws Exception {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://files.stroyinf.ru/cgi-bin/ecat/ecat.fcgi");
        List<NameValuePair> params = new ArrayList<NameValuePair>(8);
        params.add(new BasicNameValuePair("f1", std));
        params.add(new BasicNameValuePair("f2", "1"));
        params.add(new BasicNameValuePair("f3", "0"));
        params.add(new BasicNameValuePair("f4", "0"));
        params.add(new BasicNameValuePair("b", "2"));
        params.add(new BasicNameValuePair("c1", "0"));
        params.add(new BasicNameValuePair("c2", "3"));
        params.add(new BasicNameValuePair("c3", ""));
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream in = entity.getContent()) {

                return getStatus(std, Jsoup.parse(in, "UTF-8", "http://files.stroyinf.ru/cgi-bin/ecat/ecat.fcgi"));
            }
        }
        return StdStatus.ERROR;
    }

    private static StdStatus getStatus(String std, Document html) {
        Element docTable = html.getElementsByClass("doctab1").first();
        Elements rows = docTable.getElementsByClass("m4");

        if (rows.size() == 0) {
            return StdStatus.NOT_FOUND;
        }

        for (Element row : rows) {
            Element firstColumn = row.child(0);
            if (!firstColumn.getElementsByClass("a1").text().equals(std)) {
                continue;
            }

            String stdName = row.getElementsByClass("htxt").first().text();
            String stdNumber = row.getElementsByClass("htxt").get(1).text();

            String stdStatus = row.child(2).text();


            switch (stdStatus) {
                case "действует":
                case "введен впервые":
                case "взамен":
                case "восстановлен на территории рф":
                case "продлен срок действия":
                case "принят":
                    return StdStatus.OK;
                case "не действует":
                case "утратил силу":
                case "заменен":
                case "документ не актуализирован":
                    return StdStatus.OBSOLETE;
                default:
                    return StdStatus.UNKNOWN;
            }
        }
        System.out.println("Returning error on std #" + std);
        return StdStatus.ERROR;
    }
}
