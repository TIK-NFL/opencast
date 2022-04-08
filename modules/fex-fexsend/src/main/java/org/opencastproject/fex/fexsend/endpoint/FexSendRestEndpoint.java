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
package org.opencastproject.fex.fexsend.endpoint;

/**
 * Created by ac129583 on 30.10.17.
 */

import org.opencastproject.fex.api.FexException;
import org.opencastproject.fex.api.FexSendService;
import org.opencastproject.fex.api.FexService;
import org.opencastproject.fex.objects.Fex;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("")
@RestService(
        name = "fexsend",
        title = "Fex Send Service",
        abstractText = "This service performs sending of media files.",
        notes = {
          "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
          "If the service is down or not working it will return a status 503, this means the the underlying service is "
                  + "not working and is either restarting or has failed",
          "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                  + "other words, there is a bug! You should file an error report with your server logs from the time "
                  + "when the error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"
        }
)
public class FexSendRestEndpoint extends AbstractJobProducerEndpoint {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(FexSendRestEndpoint.class);

  /**
   * The rest docs
   */
  protected String docs;

  /**
   * The Fex-Send service
   */
  protected FexSendService service;

  /**
   * The service registry
   */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * The Series service
   */
  protected SeriesService seriesService = null;

  /**
   * The Fex service
   */
  protected FexService fexService = null;

  /**
   * Callback from the OSGi declarative services to set the FexSend service.
   *
   * @param fexSendService the fexSend service
   */
  protected void setFexSendService(FexSendService fexSendService) {
    this.service = fexSendService;
  }

  /**
   * Callback from the OSGi declarative services to set the series service.
   *
   * @param seriesService the series service
   */
  protected void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Callback from the OSGi declarative services to set the fex service.
   *
   * @param fexService the  fex service
   */
  protected void setFexService(FexService fexService) {
    this.fexService = fexService;
  }

  /**
   * Sends Fex for a track.
   *
   * @param trackAsXml the track xml to send
   * @return the job in the body of a JAX-RS response
   * @throws Exception
   */
  @POST
  @Path("")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
          name = "sendFex",
          description = "Submit a track for sending corresponding fex.",
          restParameters = {
            @RestParameter(description = "The track to send.", isRequired = true, name = "track",
                    type = RestParameter.Type.FILE)
          },
          responses = {
            @RestResponse(description = "The job ID to use when searching for corresponding fex request.",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The \"track\" is NULL or not a valid track type.",
                    responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "No Fex request found for Track",
                    responseCode = HttpServletResponse.SC_NO_CONTENT),
            @RestResponse(description = "The underlying service could not send the track.",
                    responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
          },
          returnDescription = "The job ID to use when polling for the resulting mpeg7 catalog."
  )
  public Response sendFex(@FormParam("track") String trackAsXml) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(trackAsXml)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("track must not be null").build();
    }
    // Deserialize the track
    MediaPackage mediaPackage = MediaPackageElementParser.getFromXml(trackAsXml).getMediaPackage();
    String seriesId = mediaPackage.getSeries();
    Fex fex = fexService.getFexBySeriesId(seriesId);
    if (fex == null) {
      logger.info("No corresponding fex request found.");
      return Response.status(Response.Status.NO_CONTENT).build();
    }

    try {
      Job job = service.fexSend(Arrays.asList(mediaPackage.getTracks()), fex);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (FexException e) {
      logger.warn("Sending fex failed: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer) {
      return (JobProducer) service;
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

}
