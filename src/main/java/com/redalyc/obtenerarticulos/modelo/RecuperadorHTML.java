package com.redalyc.obtenerarticulos.modelo;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class RecuperadorHTML {

    /*
    * @brief Función para recuperar el html de una página web.
    * @param URL de la página a descargar el HTML.
    * @return Regresa el HTML de la página pasada como parámetro.*/
    public String obtenerHTML(String url){
        StringBuilder res = new StringBuilder();
        try {
            URL link = new URL(url);
            HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();

            // Establecer el User-Agent para simular una solicitud desde un navegador
            httpConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {
                String linea;

                // Leer y guardar cada línea del HTML
                while ((linea = reader.readLine()) != null) {
                    res.append(linea);
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("La URL es inválida: " + url);
            //e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Ocurrió un error al intentar leer la página: " + url);
            //e.printStackTrace();
        }

        return res.toString();
    }
}
