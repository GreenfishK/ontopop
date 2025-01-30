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

import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author G.A.P. II
 */
public class ReciprocalWeightedNodePruning extends WeightedNodePruning {
    private static final long serialVersionUID = -4814139206044282753L;

    public ReciprocalWeightedNodePruning() {
        this(WeightingScheme.ARCS);
    }
    
    public ReciprocalWeightedNodePruning(WeightingScheme scheme) {
        super(scheme);
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains the comparisons "
               + "that correspond to edges in the blocking graph that are exceed the average edge weight "
               + "in both adjacent node neighborhoods.";
    }
    
    @Override
    public String getMethodName() {
        return "Reciprocal Weighted Node Pruning";
    }

    @Override
    protected float getValidWeight(int entityId, int neighborId) {
        float weight = getWeight(entityId, neighborId);
        boolean inNeighborhood1 = averageWeight[entityId] <= weight;
        boolean inNeighborhood2 = averageWeight[neighborId] <= weight;
        
        if (inNeighborhood1 && inNeighborhood2) {
            if (entityId < neighborId) {
                return weight;
            }
        }
        
        return -1;
    }
}
