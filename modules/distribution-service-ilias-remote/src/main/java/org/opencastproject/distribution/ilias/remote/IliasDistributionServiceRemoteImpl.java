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
package org.opencastproject.distribution.ilias.remote;

import static java.lang.String.format;
import static org.opencastproject.util.HttpUtil.param;
import static org.opencastproject.util.HttpUtil.post;
import static org.opencastproject.util.JobUtil.jobFromHttpResponse;
import static org.opencastproject.util.data.functions.Options.join;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.RemoteBase;

import com.google.gson.Gson;

import org.apache.http.client.methods.HttpPost;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A remote distribution service invoker. */
public class IliasDistributionServiceRemoteImpl extends RemoteBase
        implements DistributionService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IliasDistributionServiceRemoteImpl.class);

  /** The property to look up and append to REMOTE_SERVICE_TYPE_PREFIX */
  private static final String PARAM_CHANNEL_ID = "channelId";
  private static final String PARAM_MEDIAPACKAGE = "mediapackage";
  private static final String PARAM_ELEMENT_ID = "elementId";

  private final Gson gson = new Gson();

  /** The distribution channel identifier */
  private String distributionChannel;

  public IliasDistributionServiceRemoteImpl() {
    super(JOB_TYPE_PREFIX + "ilias");
  }

    public String getDistributionType() {
        return this.distributionChannel;
    }

  /** activates the component */
  protected void activate(ComponentContext cc) {
    this.distributionChannel = (String) cc.getProperties().get(CONFIG_KEY_STORE_TYPE);
    super.serviceType = JOB_TYPE_PREFIX + this.distributionChannel;
    logger.info(format("Setting jobtype to %s",super.serviceType));
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
    logger.info(format("Distributing %s to %s@%s", elementId, channelId, distributionChannel));
    final HttpPost req = post(param(PARAM_CHANNEL_ID, channelId),
                              param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                              param(PARAM_ELEMENT_ID, gson.toJson(elementId)));
    for (Job job : join(runRequest(req, jobFromHttpResponse))) {
      return job;
    }
    throw new DistributionException(format("Unable to distribute element '%s' of "
                                                   + "mediapackage '%s' using a remote destribution service proxy",
                                           elementId, mediaPackage.getIdentifier().toString()));
    }

    @Override
    public Job retract(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
      logger.info(format("Retracting %s from %s@%s", elementId, channelId, distributionChannel));
      final HttpPost req = post("/retract",
                              param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                              param(PARAM_ELEMENT_ID, gson.toJson(elementId)),
                              param(PARAM_CHANNEL_ID, channelId));
      for (Job job : join(runRequest(req, jobFromHttpResponse))) {
        return job;
      }
      throw new DistributionException(format("Unable to retract element '%s' of "
                                                   + "mediapackage '%s' using a remote destribution service proxy",
                                           elementId, mediaPackage.getIdentifier().toString()));
  }
}