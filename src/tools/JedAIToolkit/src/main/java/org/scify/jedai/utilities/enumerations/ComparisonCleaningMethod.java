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

package org.scify.jedai.utilities.enumerations;

import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityEdgePruning;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.blockprocessing.comparisoncleaning.ComparisonPropagation;
import org.scify.jedai.blockprocessing.comparisoncleaning.ReciprocalCardinalityNodePruning;
import org.scify.jedai.blockprocessing.comparisoncleaning.ReciprocalWeightedNodePruning;
import org.scify.jedai.blockprocessing.comparisoncleaning.WeightedEdgePruning;
import org.scify.jedai.blockprocessing.comparisoncleaning.WeightedNodePruning;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.comparisoncleaning.BLAST;
import org.scify.jedai.blockprocessing.comparisoncleaning.CanopyClustering;
import org.scify.jedai.blockprocessing.comparisoncleaning.ExtendedCanopyClustering;

/**
 *
 * @author GAP2
 */

public enum ComparisonCleaningMethod {
    BLAST,
    CANOPY_CLUSTERING,
    CARDINALITY_EDGE_PRUNING,
    CARDINALITY_NODE_PRUNING,
    COMPARISON_PROPAGATION,
    EXTENDED_CANOPY_CLUSTERING,
    RECIPROCAL_CARDINALITY_NODE_PRUNING,
    RECIPROCAL_WEIGHTING_NODE_PRUNING,
    WEIGHTED_EDGE_PRUNING,
    WEIGHTED_NODE_PRUNING;

    public static IBlockProcessing getDefaultConfiguration(ComparisonCleaningMethod coclMethod) {
        switch (coclMethod) {
            case BLAST:
                return new BLAST();
            case CANOPY_CLUSTERING:
                return new CanopyClustering(0.7f, 0.8f);
            case CARDINALITY_EDGE_PRUNING:
                return new CardinalityEdgePruning();
            case CARDINALITY_NODE_PRUNING:
                return new CardinalityNodePruning();
            case COMPARISON_PROPAGATION:
                return new ComparisonPropagation();
            case EXTENDED_CANOPY_CLUSTERING:
                return new ExtendedCanopyClustering(20, 10);
            case RECIPROCAL_CARDINALITY_NODE_PRUNING:
                return new ReciprocalCardinalityNodePruning();
            case RECIPROCAL_WEIGHTING_NODE_PRUNING:
                return new ReciprocalWeightedNodePruning();
            case WEIGHTED_EDGE_PRUNING:
                return new WeightedEdgePruning();
            case WEIGHTED_NODE_PRUNING:
                return new WeightedNodePruning();
            default:
                return new ComparisonPropagation();
        }
    }
    
    public static IBlockProcessing getMetablockingMethod(ComparisonCleaningMethod mbMethod, WeightingScheme wScheme) {
        switch (mbMethod) {
            case BLAST:
                return new BLAST(wScheme);
            case CARDINALITY_EDGE_PRUNING:
                return new CardinalityEdgePruning(wScheme);
            case CARDINALITY_NODE_PRUNING:
                return new CardinalityNodePruning(wScheme);
            case RECIPROCAL_CARDINALITY_NODE_PRUNING:
                return new ReciprocalCardinalityNodePruning(wScheme);
            case RECIPROCAL_WEIGHTING_NODE_PRUNING:
                return new ReciprocalWeightedNodePruning(wScheme);
            case WEIGHTED_EDGE_PRUNING:
                return new WeightedEdgePruning(wScheme);
            case WEIGHTED_NODE_PRUNING:
                return new WeightedNodePruning(wScheme);
            default:
                return new ComparisonPropagation();
        }
    }
}
