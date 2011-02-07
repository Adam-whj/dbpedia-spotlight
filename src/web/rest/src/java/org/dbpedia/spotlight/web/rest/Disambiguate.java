package org.dbpedia.spotlight.web.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * REST Web Service
 */

@Path("/disambiguate")
@Consumes("text/plain")
public class Disambiguate {
    @Context
    private UriInfo context;

    // Disambiguation interface
    private static SpotlightInterface disambigInterface = new SpotlightInterface(Server.disambiguator);

    @GET
    @Produces("text/xml")
    public String getXML(@QueryParam("text") String text,
                         @DefaultValue("0.3") @QueryParam("confidence") double confidence,
                         @DefaultValue("30") @QueryParam("support") int support,
                         @DefaultValue("") @QueryParam("types") String dbpediaTypes,
                         @DefaultValue("") @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue("whitelist") @QueryParam("policy") String policy,
                         @DefaultValue("true") @QueryParam("coreferenceResolution") boolean coreferenceResolution) throws Exception {

        return disambigInterface.getXML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
    }

    @GET
    @Produces("application/json")
    public String getJSON(@DefaultValue("") @QueryParam("text") String text,
                          @DefaultValue("0.3") @QueryParam("confidence") Double confidence,
                          @DefaultValue("30") @QueryParam("support") int support,
                          @DefaultValue("") @QueryParam("types") String dbpediaTypes,
                          @DefaultValue("") @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue("whitelist") @QueryParam("policy") String policy,
                          @DefaultValue("true") @QueryParam("coreferenceResolution") boolean coreferenceResolution) throws Exception {

        return disambigInterface.getJSON(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
    }

    @GET
    @Produces("application/rdf+xml")
    public String getRDF(@DefaultValue("") @QueryParam("text") String text,
                         @DefaultValue("0.3") @QueryParam("confidence") Double confidence,
                         @DefaultValue("30") @QueryParam("support") int support,
                         @DefaultValue("") @QueryParam("types") String dbpediaTypes,
                         @DefaultValue("") @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue("whitelist") @QueryParam("policy") String policy,
                         @DefaultValue("true") @QueryParam("coreferenceResolution") boolean coreferenceResolution) throws Exception {

        return disambigInterface.getRDF(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
    }

}
