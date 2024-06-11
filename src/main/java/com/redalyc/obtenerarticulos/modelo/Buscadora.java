package com.redalyc.obtenerarticulos.modelo;

import com.redalyc.obtenerarticulos.principal.ControladorRefinador;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Buscadora {

    private static final Logger logger = Logger.getLogger(Buscadora.class.getName());

    private final Pattern articuloOJS = Pattern.compile("(http|https)://\\S*/article/view/[0-9]+");
    private final Pattern datosOJS= Pattern.compile("Vol\\. ([0-9]+) \\S*\\. ([0-9]+) \\(([0-9]+)\\)");

    private final Pattern articulosSCIELO = Pattern.compile("(http|https)://scielo\\.\\S*/scielo\\.php\\?script=sci_arttext&amp;pid=\\S*&amp;lng=\\S*lng=\\S\\S");
    private final Pattern volNumSCIELO = Pattern.compile("(vol|Vol)\\.([0-9]+)");
    private final Pattern noPulSCIELO = Pattern.compile("(no|No)\\.([0-9]+)");
    private final Pattern yearSCIELO = Pattern.compile("20[0123][0-9]");

    public Buscadora(){}

    public RevistaC ojs(String html){
        //CREAR OBJETO A RETORNAR
        ArrayList<String> articulos = new ArrayList<>();
        Volumen volumen = new Volumen();
        RevistaC revistaC = new RevistaC();

        //ENCONTRAR LAS COINCIDENCIAS DE AÑO Y VOLUMEN
        Matcher matcher0 = datosOJS.matcher(html);
        String volNum = "";
        String noPul = "";
        String year = "";

        while(matcher0.find()){
            volNum = matcher0.group(1);
            noPul = matcher0.group(2);
            year = matcher0.group(3);
        }

        //ENCONTRAR LAS COINCIDENCIAS DE LOS ARTÍCULOS
        Matcher matcher1 = articuloOJS.matcher(html);
        while(matcher1.find() && !articulos.contains(matcher1.group())){
            articulos.add(matcher1.group());
        }

        //AGREGAR LOS DATOS AL VOLUMEN
        volumen.setNumero(volNum);
        volumen.setNoPublicacion(noPul);
        volumen.setYear(year);
        volumen.setArticulos(articulos);
        revistaC.setVolumen(volumen);

        logger.info("Articulos de revista OJS obtenidos");
        return revistaC;
    }

    public RevistaC scielo(String html){
        //CREAR OBJETO A RETORNAR
        ArrayList<String> articulosS= new ArrayList<>();
        Volumen volumen = new Volumen();
        RevistaC revistaC = new RevistaC();

        //ENCONTRAR LAS COINCIDENCIAS DE AÑO Y VOLUMEN
        Matcher matcherVol = volNumSCIELO.matcher(html);
        Matcher matcherNo = noPulSCIELO.matcher(html);
        Matcher matcherYear = yearSCIELO.matcher(html);

        String volNum = "";
        String noPul = "";
        String year = "";

        if(matcherVol.find()) volNum = matcherVol.group(2);
        if(matcherNo.find()) noPul = matcherNo.group(2);
        if(matcherYear.find()) year = matcherYear.group();


        //ENCONTRAR LAS COINCIDENCIAS DE LOS ARTÍCULOS
        Matcher matcher3 = articulosSCIELO.matcher(html);
        while(matcher3.find()){
            articulosS.add(matcher3.group().replace("&amp;","&"));
        }

        //AGREGAR LOS DATOS AL VOLUMEN
        volumen.setNumero(volNum);
        volumen.setNoPublicacion(noPul);
        volumen.setYear(year);
        volumen.setArticulos(articulosS);
        revistaC.setVolumen(volumen);

        logger.info("Artículos de revista Scielo obtenidos");

        return revistaC;
    }
}
