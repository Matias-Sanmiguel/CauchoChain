

import java.util.Date;

public class block {

    public String hash;
    public String hashAnterior;
    private String datos; //our data will be a simple message.
    private long timeStamp; //as number of milliseconds since 1/1/1970.

        public block (String datos,String hashAnterior ) {
            this.datos = datos;
            this.hashAnterior = hashAnterior;
            this.timeStamp = new Date().getTime();

    }
}

