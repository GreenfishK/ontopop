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

import java.io.Serializable;

/**
 *
 * @author gap2
 */

public class IdDuplicates implements Serializable {
 
    private static final long serialVersionUID = 7234234586147L;
    
    private final int entityId1;
    private final int entityId2;
    
    public IdDuplicates(int id1, int id2) {
        entityId1 = id1;
        entityId2 = id2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IdDuplicates other = (IdDuplicates) obj;
        if (this.entityId1 != other.entityId1) {
            return false;
        }
        return this.entityId2 == other.entityId2;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.entityId1;
        hash = 83 * hash + this.entityId2;
        return hash;
    }
    
    public int getEntityId1() {
        return entityId1;
    }
    
    public int getEntityId2() {
        return entityId2;
    }
}