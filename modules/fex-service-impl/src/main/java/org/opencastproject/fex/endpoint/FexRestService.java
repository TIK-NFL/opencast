/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.fex.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.fex.api.FexException;
import org.opencastproject.fex.api.FexService;
import org.opencastproject.fex.objects.Fex;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for Fex Service
 */
@Path("/")
@RestService(
      name = "fexservice",
      title = "Fex Service",
      abstractText = "This service creates, edits and retrieves and helps managing fex.",
      notes = "API for creating, editing and managing fex"
)
public class FexRestService {

  /**
   * Suffix to mark descending ordering of results
   */
  public static final String DESCENDING_SUFFIX = "_DESC";
  private static final String FEX_ELEMENT_CONTENT_TYPE_PREFIX = "fex/";
  /**
   * Logging utility
   */
  private static final Logger logger = LoggerFactory.getLogger(FexRestService.class);
  /**
   * Default number of items on page
   */
  private static final int DEFAULT_LIMIT = 20;
  /**
   * Default server URL
   */
  protected String serverUrl = "http://localhost:8080";
  /**
   * Service url
   */
  protected String serviceUrl = null;
  /**
   * FexService
   */
  private FexService fexService;

  /**
   * OSGi callback for setting fex service.
   */
  public void setService(FexService fexService) {
    this.fexService = fexService;
  }

  public void activate(ComponentContext cc) {
    if (cc == null) {
      this.serverUrl = "http://localhost:8080";
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      logger.debug("Configured server url is {}", ccServerUrl);
      this.serverUrl = Objects.requireNonNullElse(ccServerUrl, "http://localhost:8080");
    }
    serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  public String getFexXmlUrl(String fexId) {
    return UrlSupport.concat(serverUrl, serviceUrl, fexId + ".xml");
  }

  public String getFexJsonUrl(String fexId) {
    return UrlSupport.concat(serverUrl, serviceUrl, fexId + ".json");
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{fexId:.+}.json")
  @RestQuery(
        name = "getAsJson", description = "Returns the fex with the given identifier",
        returnDescription = "Returns the fex object",
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex object."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found.")
        }
  )
  public Response getFexJason(@PathParam("fexId") String fexId) {
    logger.debug("Fex Lookup: {}", fexId);
    try {
      Fex fex = this.fexService.getFex(fexId);
      return Response.ok(Fex.toJson(fex)).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Could not retrieve fex: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/bySeriesId/{seriesId:.+}.json")
  @RestQuery(
        name = "getAsJson",
        description = "Returns the fex with the given series identifier",
        returnDescription = "Returns the fex object",
        pathParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The series identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex object."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this series identifier was found.")
        }
  )
  public Response getFexBySeriesIdJason(@PathParam("seriesId") String seriesId) {
    logger.debug("Fex Lookup: {}", seriesId);
    try {
      Fex fex = this.fexService.getFexBySeriesId(seriesId);
      return Response.ok(Fex.toJson(fex)).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Could not retrieve fex: {}", e.getMessage());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("allFex.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
        name = "getAll", description = "Returns a list of all fex",
        returnDescription = "Json list of identifier of all fex",
        responses = {
          @RestResponse(responseCode = SC_OK, description = "A list with fex"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error while processing the request")
        }
  )
  public Response getAllFex() {
    try {
      List<Fex> allFex = fexService.getAllFex();
      JSONArray fexJsonArr = new JSONArray();
      for (Fex fex : allFex) {
        fexJsonArr.add(Fex.toJson(fex));
      }
      JSONObject resultJson = new JSONObject();
      resultJson.put("fex", fexJsonArr);
      return Response.ok(resultJson.toJSONString()).build();
    } catch (FexException e) {
      logger.warn("Unable to get all fex: {}", e.getStackTrace());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (NullPointerException e) {
      logger.info("No fex available");
      return null;
    }
  }

  @GET
  @Path("{id}/receiver")
  @RestQuery(
        name = "getReceiver",
        description = "Returns receiver of the fex",
        returnDescription = "Returns receiver of the fex",
        pathParameters = {
          @RestParameter(name = "id", description = "ID of fex", isRequired = true, type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex' organization"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Fex with specified ID does not exist")
        }
  )
  public Response getReceiver(@PathParam("id") String fexId) {
    try {
      String receiver = fexService.getFexReceiver(fexId);
      return Response.ok(receiver).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to get fex receiver with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("{id}/lectureId")
  @RestQuery(
        name = "getLectureId", description = "Returns lecture id of the fex",
        returnDescription = "Returns lecture id of the fex",
        pathParameters = {
          @RestParameter(name = "id", description = "ID of fex", isRequired = true, type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex' lecture id"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Fex with specified ID does not exist")
        }
  )
  public Response getLectureId(@PathParam("id") String fexId) {
    try {
      String lectureId = fexService.getLectureId(fexId);
      return Response.ok(lectureId).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to get fex lecture id with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("{id}/sbs")
  @RestQuery(
        name = "getSbs",
        description = "Returns sbs status of the fex",
        returnDescription = "Returns sbs status of the fex",
        pathParameters = {
          @RestParameter(name = "id", description = "ID of fex", isRequired = true, type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex' sbs status"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Fex with specified ID does not exist")
        }
  )
  public Response getSbs(@PathParam("id") String fexId) {
    try {
      boolean sbs = fexService.isSbs(fexId);
      return Response.ok(Boolean.toString(sbs)).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to get sbs status of fex with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("{id}/seriesId")
  @RestQuery(
        name = "getSeriesId", description = "Returns series id of the fex",
        returnDescription = "Returns series id of the fex",
        pathParameters = {
          @RestParameter(name = "id", description = "ID of fex", isRequired = true, type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The fex' series id"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Fex with specified ID does not exist")
        }
  )
  public Response getSeriesId(@PathParam("id") String fexId) {
    try {
      String seriesId = fexService.getFexSeriesId(fexId);
      return Response.ok(seriesId).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to get series id of fex with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{fexId:.+}/receiver")
  @RestQuery(
        name = "updateReceiver",
        description = "Update receiver of the fex",
        returnDescription = "No content.",
        restParameters = {
          @RestParameter(name = "receiver", isRequired = true, description = "The receiver of the fex", type = STRING)
        },
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The receiver has been updated."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing "
                  + "in the request.")
        }
  )
  public Response updateReceiver(@PathParam("fexId") String fexId, @FormParam("receiver") String receiver) {
    if (receiver == null) {
      logger.warn("Receiver parameter is null.");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      fexService.updateReceiver(fexId, receiver);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update receiver of Fex with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{fexId:.+}/lectureId")
  @RestQuery(
        name = "updateLectureId",
        description = "Update lecture id of the fex",
        returnDescription = "No content.",
        restParameters = {
          @RestParameter(name = "lectureId", isRequired = true, description = "The lectureId of the fex", type = STRING)
        },
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The lecture id has been updated."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing "
                  + "in the request.")
        }
  )
  public Response updateLectureId(@PathParam("fexId") String fexId, @FormParam("lectureId") String lectureId) {
    if (lectureId == null) {
      logger.warn("Lecture id parameter is null.");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      fexService.updateLectureId(fexId, lectureId);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update lecture id of fex with id {}: {}", fexId, lectureId);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{fexId:.+}/seriesId")
  @RestQuery(
        name = "updateSeriesId",
        description = "Update series id of the fex",
        returnDescription = "No content.",
        restParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The seriesId of the fex", type = STRING)
        },
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The series id has been updated."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing "
                  + "in the request.")
        }
  )
  public Response updateSeriesId(@PathParam("fexId") String fexId, @FormParam("seriesId") String seriesId) {
    if (seriesId == null) {
      logger.warn("Series id parameter is null.");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      fexService.updateSeriesId(fexId, seriesId);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update series id of fex with id {}: {}", fexId, seriesId);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{fexId:.+}/sbs")
  @RestQuery(
        name = "updateSbs",
        description = "Update sbs status of the fex",
        returnDescription = "No content.",
        restParameters = {
          @RestParameter(name = "sbs", isRequired = true, description = "The sbs status of the fex", type = STRING)
        },
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The sbs status has been updated."),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required path or form params were missing "
                  + "in the request.")
        }
  )
  public Response updateSbs(@PathParam("fexId") String fexId, @FormParam("sbs") boolean sbs) {
    try {
      fexService.updateIsSbsStatus(fexId, sbs);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to update sbs status of fex with id {}: {}", fexId, sbs);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("/{fexId:.+}")
  @RestQuery(
        name = "delete",
        description = "Delete a fex",
        returnDescription = "No content.",
        pathParameters = {
          @RestParameter(name = "fexId", isRequired = true, description = "The fex identifier", type = STRING)
        },
        responses = {
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No fex with this identifier was found."),
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The fex was deleted.")
        }
  )
  public Response deleteSeries(@PathParam("fexId") String fexId) {
    try {
      fexService.deleteFex(fexId);
      return Response.status(Response.Status.NO_CONTENT).build();
    } catch (NotFoundException e) {
      logger.warn("Fex with ID {} does not exist", fexId);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      logger.warn("Unable to delete fex with id {}: {}", fexId, ExceptionUtils.getStackTrace(e));
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/count")
  @RestQuery(
        name = "count",
        description = "Returns the number of fex",
        returnDescription = "number of fex",
        responses = {
          @RestResponse(responseCode = SC_OK, description = "The number of fex")
        }
  )
  public Response getCount() {
    try {
      int count = fexService.countFex();
      return Response.ok(count).build();
    } catch (FexException e) {
      logger.warn("Could not count fex: {}", e.getMessage());
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/")
  @RestQuery(
        name = "addFex",
        description = "Adds a Fex",
        returnDescription = "No content.",
        restParameters = {
          @RestParameter(name = "fex", isRequired = true, description = "The fex document", type = TEXT)
        },
        responses = {
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The required form params were missing "
                  + "in the request."),
          @RestResponse(responseCode = SC_CREATED, description = "The Fex has been created.")
        }
  )
  public Response addFex(@FormParam("fex") String fex) {
    if (fex == null) {
      logger.warn("ID of Fex that should be added is null");
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      fexService.addFex(fex);
      return Response.status(Response.Status.CREATED).build();
    } catch (FexException e) {
      logger.error("Unable to create new Fex: {}", e.getStackTrace());
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

}

