package com.redalyc.obtenerarticulos.principal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.redalyc.obtenerarticulos.modelo.ArticleInfo;
import com.redalyc.obtenerarticulos.modelo.Descriptions;
import com.redalyc.obtenerarticulos.modelo.RevistaC;
import com.redalyc.obtenerarticulos.modelo.Volumen;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.microsoft.playwright.Playwright.create;


@RestController
public class ControladorObtenedor {

    private static final Logger logger = Logger.getLogger(ControladorObtenedor.class.getName());

    @PostMapping("/getOJSData")
    public String getDataOJS(@RequestBody RevistaC revistaOJS) {

        // Guardar el total de Revistas, Volumenes y Articulos
        int totalRevistas = 0;
        int totalVolumenes = 0;
        int totalArticulos = 0;

        totalRevistas++; // sumar una revista

        int claveRevista = revistaOJS.getClave(); // obtener la clave de la revista
        Volumen volumenRevista = revistaOJS.getVolumen(); // obtener los volumenes de la revista

        // Obtener los datos del volumen
        String numero = volumenRevista.getNumero();
        String noPublicacion = volumenRevista.getNoPublicacion();
        String year = volumenRevista.getYear();
        ArrayList<String> articulos = volumenRevista.getArticulos();

        totalVolumenes++; // sumar un volumen
        totalArticulos += articulos.size(); // sumar los articulos del volumen

        for (String articulo : articulos) {
            processArticleOJS(claveRevista, volumenRevista, numero, noPublicacion, year, articulo);
        }

        // Print the total of journals(clave), volumes, and articles
        return String.format("Total de journals: %d, total de volumenes: %d, total de articulos: %d",
                totalRevistas, totalVolumenes, totalArticulos);
    }

    private void processArticleOJS(int claveRevista, Volumen volumenRevista, String numero, String noPublicacion, String year, String articulo) {
        String pdf = "";
        String pathFolder = "";

        try (Playwright playwright = create()) {
            Browser browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(0)); // Esperar a que la página cargue
                page.navigate(articulo);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error al navegar a la url: " + articulo, e);
                return;
            }

            ArticleInfo articleInfo = extractArticleInfoOJS(page);
            pdf = articleInfo.getPdf();

            pathFolder = String.format("/PruebasRedalyc/%d/%s_%s_%s/%s", claveRevista, numero, noPublicacion, year, articleInfo.getIdentificador());
            createFolderOJS(pathFolder);
            saveArticleInfoAsJsonOJS(articleInfo, pathFolder);
            downloadPdfOJS(pdf, pathFolder);

            browser.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Error al procesar el articulo. Revista: %d, Volumen: %s, No. Publicación: %s, Articulo: %s",
                    claveRevista, volumenRevista, noPublicacion, articulo), e);
        }
    }

    private ArticleInfo extractArticleInfoOJS(Page page) {
        Gson gson = new Gson();

        // Obtener la descripcion del articulo del meta tag DC.Description con el filtro de lenguaje en español y en inglés
        String descriptionsJson = safeEvaluateOJS(page, """
                () => {
                    const metas = document.head.querySelectorAll('meta[name="DC.Description"]');
                    const descriptions = {};
                    metas.forEach(meta => {
                        const lang = meta.getAttribute('xml:lang') || 'en';
                        const content = meta.getAttribute('content');
                        descriptions[lang] = content;
                    });
                    return JSON.stringify(descriptions);
                }""");

        // Convertir el JSON en un mapa usando Gson
        Map<String, String> descriptions = gson.fromJson(descriptionsJson, Map.class);

        // Acceder a los valores del mapa
        String englishDescription = descriptions.get("en");
        String spanishDescription = descriptions.get("es");

        ArticleInfo articleInfo = new ArticleInfo();
        articleInfo.setDescripcion(new Descriptions(englishDescription, spanishDescription));
        articleInfo.setIdentificador(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Identifier"]')?.getAttribute('content')"""));
        articleInfo.setUri(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Identifier.URI"]')?.getAttribute('content')"""));
        articleInfo.setDerechos(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Rights"]')?.getAttribute('content')"""));
        articleInfo.setFuente(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Source"]')?.getAttribute('content')"""));
        articleInfo.setVolumen(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Source.Volume"]')?.getAttribute('content')"""));
        articleInfo.setUriFuente(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Source.URI"]')?.getAttribute('content')"""));
        articleInfo.setTema(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Subject"]')?.getAttribute('content')"""));
        articleInfo.setTitulo(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Title"]')?.getAttribute('content')"""));
        articleInfo.setTituloAlternativo(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Title.Alternative"]')?.getAttribute('content')"""));
        articleInfo.setJournalTitle(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_journal_title"]')?.getAttribute('content')"""));
        articleInfo.setJournalAbbreviation(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_journal_abbrev"]')?.getAttribute('content')"""));
        articleInfo.setAutor(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_author"]')?.getAttribute('content')"""));
        articleInfo.setInstitucion(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_author_institution"]')?.getAttribute('content')"""));
        articleInfo.setFecha(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_date"]')?.getAttribute('content')"""));
        articleInfo.setPdf(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="citation_pdf_url"]')?.getAttribute('content')"""));

        // Obtener las múltiples referencias del articulo del meta tag citation_reference
        String referenciasJson = safeEvaluateOJS(page, """
                () => {
                    const metas = document.head.querySelectorAll('meta[name="citation_reference"]');
                    const references = [];
                    metas.forEach(meta => {
                        const content = meta.getAttribute('content');
                        references.push(content);
                    });
                    return JSON.stringify(references);
                }""");
        String[] referencias = gson.fromJson(referenciasJson, String[].class);
        articleInfo.setReferencia(referencias);

        articleInfo.setIssn(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Source.ISSN"]')?.getAttribute('content')"""));

        articleInfo.setCreated(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Date.created"]')?.getAttribute('content')"""));

        articleInfo.setModified(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Date.modified"]')?.getAttribute('content')"""));

        articleInfo.setIssued(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Date.issued"]')?.getAttribute('content')"""));

        articleInfo.setSubmitted(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Date.dateSubmitted"]')?.getAttribute('content')"""));

        articleInfo.setType(safeEvaluateOJS(page, """
                () => document.head.querySelector('meta[name="DC.Type.articleType"]')?.getAttribute('content')"""));

        return articleInfo;
    }

    private void createFolderOJS(String pathFolder) {
        File folder = new File(pathFolder);
        boolean success = folder.mkdirs();

        if (success) {
            logger.info("Se ha creado la carpeta: " + pathFolder);
        } else {
            logger.warning("Error al crear la carpeta: " + pathFolder);
        }
    }

    private void saveArticleInfoAsJsonOJS(ArticleInfo articleInfo, String pathFolder) {
        Gson gsonData = new GsonBuilder().setPrettyPrinting().create();
        String json = gsonData.toJson(articleInfo);

        try (FileWriter file = new FileWriter(pathFolder + "/articleInfo.json")) {
            file.write(json);
            logger.info("JSON object has been saved to articleInfo.json");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al guardar el objeto JSON en un archivo", e);
        }
    }

    private void downloadPdfOJS(String pdfUrl, String pathFolder) {
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(pathFolder + "/article.pdf")) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                logger.info("PDF downloaded successfully.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error: Unable to download the PDF file.", e);
        }
    }

    // Helper method to safely evaluate and return a string
    String safeEvaluateOJS(Page page, String script) {
        Object result = page.evaluate(script);
        return result != null ? result.toString() : "";
    }

    @PostMapping("/getScieloData")
    public String getDataScielo(@RequestBody RevistaC revistaScielo) throws FileNotFoundException{
        // Guardar el total de Revistas, Volumenes y Articulos
        int totalRevistas = 0;
        int totalVolumenes = 0;
        int totalArticulos = 0;
        int articuloID = 0;

        totalRevistas++; // sumar una revista

        int claveRevista = revistaScielo.getClave(); // obtener la clave de la revista
        Volumen volumenRevista = revistaScielo.getVolumen(); // obtener los volumenes de la revista

        // Obtener los datos del volumen
        String numero = volumenRevista.getNumero();
        String noPublicacion = volumenRevista.getNoPublicacion();
        String year = volumenRevista.getYear();
        ArrayList<String> articulos = volumenRevista.getArticulos();

        totalVolumenes++; // sumar un volumen
        totalArticulos += articulos.size(); // sumar los articulos del volumen

        for (String articulo : articulos) {
            articuloID++;
            processArticleScielo(claveRevista, volumenRevista, numero, noPublicacion, year, articulo, articuloID);
        }

        // Print the total of journals(clave), volumes, and articles
        return String.format("Total de journals: %d, total de volumenes: %d, total de articulos: %d",
                totalRevistas, totalVolumenes, totalArticulos);
    }

    private void processArticleScielo(int claveRevista, Volumen volumenRevista, String numero, String noPublicacion, String year, String articulo, int totalArticulos) {
        String pdf = "";
        String xml = "";
        String pathFolder = "";

        try (Playwright playwright = create()) {
            Browser browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            try {
                page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(0)); // Esperar a que la página cargue
                page.navigate(articulo);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error al navegar a la url: " + articulo, e);
                return;
            }

            ArticleInfo articleInfo = extractArticleInfoScielo(page);
            pdf = articleInfo.getPdf();
            xml = articleInfo.getXml();

            pathFolder = String.format("../../WS/%d/%s_%s_%s/%s", claveRevista, numero, noPublicacion, year, totalArticulos);
            createFolderScielo(pathFolder);
            saveArticleInfoAsJsonScielo(articleInfo, pathFolder);
            downloadPdfScielo(pdf, pathFolder);
            downloadXmlScielo(xml, pathFolder);

            browser.close();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Error al procesar el articulo. Revista: %d, Volumen: %s, No. Publicación: %s, Articulo: %s",
                    claveRevista, volumenRevista, noPublicacion, articulo), e);
        }
    }

    private ArticleInfo extractArticleInfoScielo(Page page) {
        ArticleInfo articleInfo = new ArticleInfo();

        articleInfo.setPdf(safeEvaluateScielo(page, """
                () => document.head.querySelector('meta[name="citation_pdf_url"]')?.getAttribute('content')"""));

        articleInfo.setXml(page.locator("#toolBox > div:nth-child(5) > ul > li:nth-child(2) > a").getAttribute("href"));
        return articleInfo;
    }

    private void createFolderScielo(String pathFolder) {
        File folder = new File(pathFolder);
        boolean success = folder.mkdirs();

        if (success) {
            logger.info("Se ha creado la carpeta: " + pathFolder);
        } else {
            logger.warning("Error al crear la carpeta: " + pathFolder);
        }
    }

    private void saveArticleInfoAsJsonScielo(ArticleInfo articleInfo, String pathFolder) {
        Gson gsonData = new GsonBuilder().setPrettyPrinting().create();
        String json = gsonData.toJson(articleInfo);

        try (FileWriter file = new FileWriter(pathFolder + "/articleInfo.json")) {
            file.write(json);
            logger.info("JSON object has been saved to articleInfo.json");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error al guardar el objeto JSON en un archivo", e);
        }
    }

    private void downloadPdfScielo(String pdfUrl, String pathFolder) {
        try {
            URL url = new URL(pdfUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(pathFolder + "/article.pdf")) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                logger.info("PDF downloaded successfully.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error: Unable to download the PDF file.", e);
        }
    }

    private void downloadXmlScielo(String xmlUrl, String pathFolder) {
        try {
            URL url = new URL(xmlUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(pathFolder + "/article.xml")) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                logger.info("XML downloaded successfully.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error: Unable to download the XML file.", e);
        }
    }

    // Helper method to safely evaluate and return a string
    String safeEvaluateScielo(Page page, String script) {
        Object result = page.evaluate(script);
        return result != null ? result.toString() : "";
    }

}