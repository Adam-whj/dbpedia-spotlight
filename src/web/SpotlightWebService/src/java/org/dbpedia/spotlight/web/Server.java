package org.dbpedia.spotlight.web;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import org.dbpedia.spotlight.annotate.Annotator;
import org.dbpedia.spotlight.annotate.DefaultAnnotator;
import org.dbpedia.spotlight.disambiguate.DefaultDisambiguator;
import org.dbpedia.spotlight.disambiguate.Disambiguator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 */

public class Server {

    protected static final Annotator annotator = new DefaultAnnotator(new File(Configuration.spotterFile), new File(Configuration.indexDirectory));

    protected static final Disambiguator disambiguator = new DefaultDisambiguator(new File(Configuration.indexDirectory));

    private static volatile Boolean running = true;

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException, ClassNotFoundException {

        URI serverURI = new URI("http://localhost:2222/");       // "http://localhost:"+args[0]+"/"

        ResourceConfig resources = new ClassNamesResourceConfig(
                Class.forName("org.dbpedia.spotlight.web.Annotate"),
                Class.forName("org.dbpedia.spotlight.web.Disambiguate"));

        HttpServer server = HttpServerFactory.create(serverURI, resources);

        server.start();
        System.err.println("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI);

        //Open browser
        try {
            URI example = new URI(serverURI.toString() + "rest/Disambiguate?text=President%20[[Obama]]%20called%20Wednesday%20on%20Congress%20to%20extend%20a%20tax%20break%20for%20students%20included%20in%20last%20year%27s%20economic%20stimulus%20package,%20arguing%20that%20the%20policy%20provides%20more%20generous%20assistance.&confidence=0.2&support=20");
            java.awt.Desktop.getDesktop().browse(example);
        }
        catch (Exception e) {
            System.err.println("Could not open browser. " + e);
        }

        while(running) {
            Thread.sleep(100);
        }

        //Stop the HTTP server
        server.stop(0);
    }

}
