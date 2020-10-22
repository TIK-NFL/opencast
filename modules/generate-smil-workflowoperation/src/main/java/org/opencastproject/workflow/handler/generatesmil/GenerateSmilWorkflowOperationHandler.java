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
package org.opencastproject.workflow.handler.generatesmil;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Workflow operation for generating a smil file for a given start and stop time.
 */
public class GenerateSmilWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  public static final String OPT_INTIME = "intime";

  public static final String OPT_OUTTIME = "outtime";

  public static final String OPT_SOURCE_FLAVORS = "source-flavors";

  public static final String OPT_TARGET_SMIL_FLAVOR = "target-smil-flavor";

  /** The default file name for generated Smil catalogs. */
  private static final String TARGET_FILE_NAME = "cut.smil";

  private Workspace workspace = null;

  private SmilService smilService = null;

  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    logger.info("Registering generate smil workflow operation handler");
  }
 
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(GenerateSmilWorkflowOperationHandler.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running generate smil workflow operation on workflow {}", workflowInstance.getId());

    int intime = -1;
    int outtime = -1;

    // Required configuration
    String intimeOpt = getConfig(workflowInstance, OPT_INTIME);
    String outtimeOpt = getConfig(workflowInstance, OPT_OUTTIME);
    MediaPackageElementFlavor sourceFlavors = MediaPackageElementFlavor
            .parseFlavor(getConfig(workflowInstance, OPT_SOURCE_FLAVORS));
    MediaPackageElementFlavor targetSmilFlavor = MediaPackageElementFlavor
            .parseFlavor(getConfig(workflowInstance, OPT_TARGET_SMIL_FLAVOR));

    if (intimeOpt != null) {
      intime = Integer.parseInt(intimeOpt) * 1000;
    }

    if (outtimeOpt != null) {
      outtime = Integer.parseInt(outtimeOpt) * 1000;
    }

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    SmilResponse smilResponse = smilService.createNewSmil(mediaPackage);

    Track[] tracks = mediaPackage.getTracks(sourceFlavors);

    try {
      smilResponse = smilService.addParallel(smilResponse.getSmil());

      final String parentId = smilResponse.getEntity().getId();

      final Long duration = (long) outtime - intime;
      smilResponse = smilService.addClips(smilResponse.getSmil(), parentId, tracks, intime, duration);
    } catch (SmilException e) {
      throw new WorkflowOperationException(e);
    }
    Smil smil = smilResponse.getSmil();
 
    //set default catalog Id if there is none existing
    String catalogId = smil.getId();
    Catalog[] catalogs = mediaPackage.getCatalogs();

    //get the first smil/cutting  catalog-ID to overwrite it with new smil info
    for (Catalog p: catalogs) {
       if (p.getFlavor().matches(targetSmilFlavor)) {
         logger.debug("Set Idendifier for Smil-Catalog to: " + p.getIdentifier());
         catalogId = p.getIdentifier();
       break;
       }
     }
     Catalog catalog = mediaPackage.getCatalog(catalogId);

    URI smilURI;
    try (InputStream is = IOUtils.toInputStream(smil.toXML(), "UTF-8")) {
      smilURI = workspace.put(mediaPackage.getIdentifier().toString(), catalogId, TARGET_FILE_NAME, is);
    } catch (SAXException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new WorkflowOperationException(e);
    } catch (JAXBException e) {
      logger.error("Error while serializing the SMIL catalog to XML: {}", e.getMessage());
      throw new WorkflowOperationException(e);
    } catch (MalformedURLException e) {
      logger.error("Error creating URL for he SMIL catalog to XML: {}", e.getMessage());
      throw new WorkflowOperationException(e);
    } catch (IOException e) {
      logger.error("Error writing the SMIL catalog: {}", e.getMessage());
      throw new WorkflowOperationException(e);
    }

    if (catalog == null) {
      MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      catalog = (Catalog) mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
              targetSmilFlavor);
      mediaPackage.add(catalog);
    }
    catalog.setURI(smilURI);
    catalog.setIdentifier(catalogId);
    catalog.setMimeType(MimeTypes.XML);

    // setting the URI to a new source so the checksum will most like be invalid
    catalog.setChecksum(null);

    return createResult(workflowInstance.getMediaPackage(), Action.CONTINUE);
  }
}
