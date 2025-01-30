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
package org.scify.jedai.blockprocessing.comparisoncleaning;

import gnu.trove.iterator.TIntIterator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class BLAST extends WeightedNodePruning {
    private static final long serialVersionUID = -5165499504728933326L;

    public BLAST() {
        this(WeightingScheme.PEARSON_X2);
    }
    
    public BLAST(WeightingScheme scheme) {
        super(scheme);
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains the comparisons "
               + "that correspond to edges in the blocking graph that are exceed 1/4 of the sum "
               + "of the maximum edge weights in the two adjacent node neighborhoods.";
    }
    
    @Override
    public String getMethodName() {
        return "BLAST";
    }
    
    @Override
    protected float getValidWeight(int entityId, int neighborId) {
        float weight = getWeight(entityId, neighborId);
        float edgeThreshold = (averageWeight[entityId] + averageWeight[neighborId]) / 4;

        if (edgeThreshold <= weight) {
            if (entityId < neighborId) {
                return weight;
            }
        }

        return -1;
    }
    
    @Override
    protected void setThreshold(int entityId) {
        threshold = 0;
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            threshold = Math.max(threshold, getWeight(entityId, iterator.next()));
        }
    }
}
