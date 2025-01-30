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

import gnu.trove.map.TObjectIntMap;

/**
 *
 * @author gap2
 */
public class AttributeClusters {

    private final float[] clustersEntropy;
    private final TObjectIntMap<String> attributeToClusterId;

    public AttributeClusters(float[] entropy, TObjectIntMap<String> mapping) {
        clustersEntropy = entropy;
        attributeToClusterId = mapping;
    }

    public float getClusterEntropy(int clusterId) {
        return clustersEntropy[clusterId];
    }

    public int getClusterId(String attributeName) {
        return attributeToClusterId.get(attributeName);
    }
    
    public int getNoOfClusters() {
        return clustersEntropy.length;
    }
}
