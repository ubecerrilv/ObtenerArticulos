package com.redalyc.obtenerarticulos.modelo;public class Descriptions {
    public String en;
    public String es;

    public Descriptions(String englishDescription, String spanishDescription) {
        this.en = englishDescription;
        this.es = spanishDescription;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String en) {
        this.en = en;
    }

    public String getEs() {
        return es;
    }

    public void setEs(String es) {
        this.es = es;
    }
}
