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

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

/**
 *
 * @author G.A.P. II
 */

public class EquivalenceCluster {
    
    private final TIntList entityIdsD1;
    private final TIntList entityIdsD2;
    
    public EquivalenceCluster() {
        entityIdsD1 = new TIntArrayList();
        entityIdsD2 = new TIntArrayList();
    }
    
    public void addEntityIdD1(int id) {
        entityIdsD1.add(id);
    }
    
    public void addEntityIdD2(int id) {
        entityIdsD2.add(id);
    }
    
    public TIntList getEntityIdsD1() {
        return entityIdsD1;
    }
    
    public TIntList getEntityIdsD2() {
        return entityIdsD2;
    }

    public void loadBulkEntityIdsD1(TIntCollection ids) {
        entityIdsD1.addAll(ids);
    }

    public void loadBulkEntityIdsD2(TIntCollection ids) {
        entityIdsD2.addAll(ids);
    }
}