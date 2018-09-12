package lab.gtcsc.gchen.depot;

import java.io.ByteArrayInputStream;
import java.sql.Timestamp;

/**
 * Created by gchen on 11/21/17.
 */

public class ClickedAd {
    private Timestamp timestamp;
    private ByteArrayInputStream bais;
    private int length;
    private String chain;
    private String html;

    public ClickedAd() {}

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setBais(ByteArrayInputStream bais) {
        this.bais = bais;
    }

    public ByteArrayInputStream getBais() {
        return bais;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getChain() {
        return chain;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getHtml() {
        return html;
    }
}