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

import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author G.A.P. II
 */
public class ReciprocalCardinalityNodePruning extends CardinalityNodePruning {
    private static final long serialVersionUID = -6854243558638413610L;

    public ReciprocalCardinalityNodePruning() {
        this(WeightingScheme.ARCS);
    }
    
    public ReciprocalCardinalityNodePruning(WeightingScheme scheme) {
        super(scheme);
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains the comparisons "
               + "that correspond to edges in the blocking graph that are among the top-k weighted "
               + "ones for both adjacent entities/nodes.";
    }

    @Override
    public String getMethodName() {
        return "Reciprocal Cardinality Node Pruning";
    }

    @Override
    protected boolean isValidComparison(int entityId, int neighborId) {
        if (nearestEntities[neighborId] == null) {
            return false;
        }

        if (nearestEntities[neighborId].contains(new Comparison(cleanCleanER, -1, entityId))) {
            return entityId < neighborId;
        }

        return false;
    }
}
