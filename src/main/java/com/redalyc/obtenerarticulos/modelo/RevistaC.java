package com.redalyc.obtenerarticulos.modelo;

import java.util.ArrayList;

public class RevistaC {
    private int clave;
    Volumen volumen;

    RevistaC(int clave, ArrayList<Volumen> volumnes){
        this.clave=clave;
    }

    public RevistaC(){

    }

    public void setClave(int clave) {
        this.clave = clave;
    }

    public int getClave() {
        return clave;
    }

    public void setVolumen(Volumen volumen) {
        this.volumen = volumen;
    }

    public Volumen getVolumen() {
        return volumen;
    }
}
