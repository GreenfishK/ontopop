/*
* Copyright [2016-2020] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.scify.jedai.datamodel;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The representation of a single entity or record. An entity is comprised of one or more
 * {@link #getAttributes() Attribute} name/value pairs and a {@link #getEntityUrl() URL} which 
 * uniquely identifies each entity.
 *
 * @author G.A.P. II
 */
public class EntityProfile implements Serializable {

    private static final long serialVersionUID = 122354534453243447L;

    private final Set<Attribute> attributes;
    private final String entityUrl;

    public EntityProfile(String url) {
        requireNonNull(url, "url cannot be null");
        entityUrl = url;
        attributes = new HashSet<>();
    }

    public void addAttribute(String propertyName, String propertyValue) {
        attributes.add(new Attribute(propertyName, propertyValue));
    }

    public String getEntityUrl() {
        return entityUrl;
    }

    public int getProfileSize() {
        return attributes.size();
    }
    
    public Set<Attribute> getAttributes() {
        return attributes;
    }
    
    @Override
    public boolean equals (Object another) {
        if (this == another) {
            return true;
        }
        if (!(another instanceof EntityProfile)) {
            return false;
        }
        EntityProfile other = (EntityProfile) another;
        return entityUrl.equals(other.entityUrl)
            && attributes.equals(other.attributes);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + entityUrl.hashCode();
        h += (h << 5) + attributes.hashCode();
        return h;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        attributes.forEach(attribute -> 
            sb.append(attribute.getName()).append(":").append(attribute.getValue()).append(","));
        if (sb.length() > 0) {
          sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
}