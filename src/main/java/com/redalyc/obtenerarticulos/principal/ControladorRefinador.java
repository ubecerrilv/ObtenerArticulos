package com.redalyc.obtenerarticulos.principal;

import com.redalyc.obtenerarticulos.modelo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
public class ControladorRefinador {

    @Autowired
    private RestTemplate restTemplate;

    private Buscadora buscadora = new Buscadora();

    RecuperadorHTML recuperador = new RecuperadorHTML();

    /*
     * @brief Función para manejar la petición con las revistas
     * @param Cuerpo JSON con la clave y url de la revista
     * @return JSON con el URL de los artículos de la revista
     */
    //TODO Cambiar los sop por logs
    @PostMapping("/SearchArticles")
    public void obtenerArt(@RequestBody Revista revista) {
        //OBTENER LOS VALORES DE LA REVISTA
        String url = revista.getUrl();
        int clave = revista.getClave();

        //FILTRAR LA REVISTA
        String html = recuperador.obtenerHTML(url);

        //OBJETO A REGRESAR
        RevistaC revistaC = new RevistaC();

        if (html.contains("<meta name=\"generator\" content=\"Open Journal Systems")) {
            revistaC = buscadora.ojs(html);
            revistaC.setClave(clave);
            restTemplate.postForLocation("http://localhost:6000/getOJSData", revistaC);
        } else if (html.contains("scielo")) {
            revistaC = buscadora.scielo(html);
            revistaC.setClave(clave);
            restTemplate.postForLocation("http://localhost:6000/getScieloData",revistaC);
        } else {
            System.out.println("No se pudo encontrar la revista como OJS o ScIELO");
        }//FIN DEL FILTRADO

    }
}