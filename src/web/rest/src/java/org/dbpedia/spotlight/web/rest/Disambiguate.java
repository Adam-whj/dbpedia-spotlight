/**
 * Copyright 2011 Pablo Mendes, Max Jakob
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
