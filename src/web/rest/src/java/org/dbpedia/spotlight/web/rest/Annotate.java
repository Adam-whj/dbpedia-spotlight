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

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST Web Service
 */

@Path("/annotate")
@Consumes("text/plain")
public class Annotate {

    @Context
    private UriInfo context;

    // Annotation interface
    private static SpotlightInterface annotationInterface = SpotlightInterface.getInstance(Server.getAnnotator(), Server.getConfiguration());

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getRDF(@DefaultValue(ServerConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                         @DefaultValue(ServerConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                         @DefaultValue(ServerConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                         @DefaultValue(ServerConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                         @DefaultValue(ServerConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue(ServerConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                         @DefaultValue(ServerConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution) {
        try {
            return annotationInterface.getHTML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.TEXT_HTML).build());
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_XHTML_XML)
    public String getRDFa(@DefaultValue(ServerConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                          @DefaultValue(ServerConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(ServerConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(ServerConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(ServerConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(ServerConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(ServerConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution) {

        try {
            return annotationInterface.getRDFa(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
        } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_XHTML_XML).build());
        }
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    public String getXML(@DefaultValue(ServerConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                         @DefaultValue(ServerConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                         @DefaultValue(ServerConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                         @DefaultValue(ServerConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                         @DefaultValue(ServerConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                         @DefaultValue(ServerConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                         @DefaultValue(ServerConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution) {

        try {
            return annotationInterface.getXML(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
       } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.TEXT_XML).build());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJSON(@DefaultValue(ServerConfiguration.DEFAULT_TEXT) @QueryParam("text") String text,
                          @DefaultValue(ServerConfiguration.DEFAULT_CONFIDENCE) @QueryParam("confidence") Double confidence,
                          @DefaultValue(ServerConfiguration.DEFAULT_SUPPORT) @QueryParam("support") int support,
                          @DefaultValue(ServerConfiguration.DEFAULT_TYPES) @QueryParam("types") String dbpediaTypes,
                          @DefaultValue(ServerConfiguration.DEFAULT_SPARQL) @QueryParam("sparql") String sparqlQuery,
                          @DefaultValue(ServerConfiguration.DEFAULT_POLICY) @QueryParam("policy") String policy,
                          @DefaultValue(ServerConfiguration.DEFAULT_COREFERENCE_RESOLUTION) @QueryParam("coreferenceResolution") boolean coreferenceResolution) {

        try {
            return annotationInterface.getJSON(text, confidence, support, dbpediaTypes, sparqlQuery, policy, coreferenceResolution);
       } catch (Exception e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST). entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build());
        }
    }
    
}
