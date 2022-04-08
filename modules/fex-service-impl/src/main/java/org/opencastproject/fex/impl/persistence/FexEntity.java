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

import org.opencastproject.fex.objects.Fex;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Created by ac129583 on 8/18/17.
 */
@Entity(name = "FexEntity")
@IdClass(FexEntityId.class)
@Access(AccessType.FIELD)
@Table(name = "mh_fex")
@NamedQueries({ @NamedQuery(name = "Fex.findAll", query = "select f from FexEntity f"),
        @NamedQuery(name = "Fex.getCount", query = "select COUNT(f) from FexEntity f"),
        @NamedQuery(name = "fexById", query = "select f from FexEntity as f where f.fexId=:fexId "
                + "and f.organization=:organization"),
        @NamedQuery(name = "fexBySeries", query = "select f from FexEntity as f where f.seriesId=:seriesId "
                + "and f.organization=:organization"),
        @NamedQuery(name = "allFexInOrg", query = "select f from FexEntity as f where f.organization=:organization") })
public class FexEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  protected String fexId;

  @Id
  @Column(name = "organization", length = 128)
  protected String organization;

  @Lob
  @Column(name = "seriesId")
  protected String seriesId;

  @Lob
  @Column(name = "lectureId")
  protected String lectureId;

  @Lob
  @Column(name = "receiver")
  protected String receiver;

  @Column(name = "sbs")
  protected boolean sbs;

  /**
   * Default constructor without any import
   */
  public FexEntity() {

  }

  public String getFexId() {
    return fexId;
  }

  public void setFexId(String fexId) {
    if (fexId == null) {
      throw new IllegalArgumentException("Fex id can't be null");
    }
    if (fexId.length() > 128) {
      throw new IllegalArgumentException("Fex id can't be longer than 128 characters");
    }
    this.fexId = fexId;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public void setSeriesId(String seriesId) {
    if (seriesId == null) {
      throw new IllegalArgumentException("Series id can be null");
    }
    if (seriesId.length() > 128) {
      throw new IllegalArgumentException("Series id can't be longer than 128 characters");
    }
    this.seriesId = seriesId;
  }

  public String getLectureId() {
    return lectureId;
  }

  public void setLectureId(String lectureId) {
    this.lectureId = lectureId;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public boolean isSbs() {
    return sbs;
  }

  public void setSbs(boolean sbs) {
    this.sbs = sbs;
  }

  public Fex getFex() {
    Fex fex = new Fex();
    fex.setFexId(fexId);
    fex.setLectureId(lectureId);
    fex.setReceiver(receiver);
    fex.setSeriesId(seriesId);
    fex.setSbs(sbs);
    return fex;
  }
}
