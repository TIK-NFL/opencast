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
package org.opencastproject.fex.impl.persistence;

import java.io.Serializable;

/**
 * IdClass for the FexEntity.
 */
public class FexEntityId implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String fexId;
  private String organization;

  FexEntityId() {

  }

  FexEntityId(String fexId, String organization) {
    this.fexId = fexId;
    this.organization = organization;
  }

  public String getFexId() {
    return fexId;
  }

  public void setFexId(String fexId) {
    this.fexId = fexId;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Override
  public int hashCode() {
    return (fexId + organization).hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    FexEntityId other = (FexEntityId) obj;
    if (fexId == null) {
      if (other.fexId != null) {
        return false;
      }
    } else if (!fexId.equals(other.fexId)) {
      return false;
    }
    if (organization == null) {
      if (other.organization != null) {
        return false;
      }
    } else if (!organization.equals(other.organization)) {
      return false;
    }
    return true;
  }
}
