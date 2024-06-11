package com.redalyc.obtenerarticulos.modelo;

import java.util.ArrayList;

public class Volumen {
    private String numero;
    private String noPublicacion;
    private String year;
    private ArrayList<String> articulos;


    public Volumen(String numero, String noPublicacion, String year, ArrayList<String> articulos) {
        this.numero = numero;
        this.noPublicacion = noPublicacion;
        this.year = year;
        this.articulos = articulos;
    }

    public Volumen() {

    }

    public void setArticulos(ArrayList<String> articulos) {
        this.articulos = articulos;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getNumero() {
        return numero;
    }

    public String getYear() {
        return year;
    }

    public ArrayList<String> getArticulos() {
        return articulos;
    }

    public void setNoPublicacion(String noPublicacion) {
        this.noPublicacion = noPublicacion;
    }

    public String getNoPublicacion() {
        return noPublicacion;
    }
}
