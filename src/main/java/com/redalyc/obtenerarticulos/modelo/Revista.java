package com.redalyc.obtenerarticulos.modelo;


public class Revista {

    private int clave;
    private String url;

    Revista(){}
    Revista (int clave, String url){
        this.clave = clave;
        this.url = url;
    }

    public int getClave() {
        return clave;
    }

    public String getUrl() {
        return url;
    }

    public void setClave(int clave) {
        this.clave = clave;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
