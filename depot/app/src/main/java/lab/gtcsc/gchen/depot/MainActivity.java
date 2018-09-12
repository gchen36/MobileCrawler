package lab.gtcsc.gchen.depot;

import android.annotation.TargetApi;
//import android.content.ContentValues;
import android.app.ActivityManager;
//import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
//import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

//import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.sql.*;

public class MainActivity extends AppCompatActivity {

    boolean loadingFinished = true;
    boolean redirect = false;
    List<String> chain = new ArrayList<>();
    ClickedAd clickedAd = new ClickedAd();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);
        //getSupportActionBar().hide();
        WebView webview = (WebView) findViewById(R.id.load_url_webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new MyJavaScriptInterface(), "HtmlSaver");
        Intent intent = getIntent();
        Uri url = intent.getData();

        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses("org.chromium.webview_shell");

        webview.setWebViewClient(new WebViewController());
        //Toast toast = Toast.makeText(MainActivity.this, url.toString(), Toast.LENGTH_SHORT);
        //toast.show();
        chain.add(url.toString());
        webview.loadUrl(url.toString());
    }

    public class WebViewController extends WebViewClient {
        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!loadingFinished) {
                redirect = true;
            }
            loadingFinished = false;
            String url = request.getUrl().toString();
            //Toast toast = Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT);
            //toast.show();
            if (!chain.contains(url)) {
                chain.add(url);
            }
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap facIcon) {
            loadingFinished = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!redirect) {
                loadingFinished = true;
            }
            if (loadingFinished && !redirect) {
                //Toast toast = Toast.makeText(MainActivity.this, "url loaded", Toast.LENGTH_SHORT);
                //toast.show();
                writeToExternalStorage(chain);
                view.loadUrl("javascript:HtmlSaver.saveHTML" +
                     "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");

            } else {
                redirect = false;
            }
        }
    }

    public void writeToExternalStorage(List<String> urls) {
        String chain = "";
        Timestamp ts = new java.sql.Timestamp((new Date()).getTime());
        clickedAd.setTimestamp(ts);
        for (String url: urls) {
            chain = chain + ", " + url;
        }

        clickedAd.setChain(chain.substring(2));
    }


    class MyJavaScriptInterface {
        @JavascriptInterface
        public void saveHTML(String data) {
            clickedAd.setHtml(data);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shotScreen();
                }
            }, 2000);
        }

        @JavascriptInterface
        public void shotScreen() {
            Toast toast = Toast.makeText(MainActivity.this, "taking screenshot...", Toast.LENGTH_SHORT);
            toast.show();

            View view = getWindow().getDecorView().getRootView();
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            int quality = 100;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, baos);
            byte[] data = baos.toByteArray();
            clickedAd.setLength(data.length);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            clickedAd.setBais(bais);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    insertClickedAd(clickedAd);
                }
            }, 1000);
        }
    }

    public Connection connect() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return DriverManager.getConnection("jdbc:postgresql://10.0.3.2:5432/gchen?user=gchen");
    }

    public long insertClickedAd(ClickedAd clickedAd) {
        String SQL_INSERT = "INSERT INTO post_webview(time, screenshot, redirectchain, html)"
            + "VALUES(?,?,?,?)";

        long id = 0;
        try {
            Connection conn = connect();
            PreparedStatement pst = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            pst.setTimestamp(1, clickedAd.getTimestamp());
            pst.setBinaryStream(2, clickedAd.getBais(), clickedAd.getLength());
            pst.setString(3, clickedAd.getChain());
            pst.setString(4, clickedAd.getHtml());
            int affectedRows = pst.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pst.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            Toast toast = Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }

        return id;
    }
}
