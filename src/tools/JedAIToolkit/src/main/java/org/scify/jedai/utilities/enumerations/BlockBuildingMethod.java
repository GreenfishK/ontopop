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

import org.scify.jedai.blockbuilding.ExtendedQGramsBlocking;
import org.scify.jedai.blockbuilding.ExtendedSortedNeighborhoodBlocking;
import org.scify.jedai.blockbuilding.ExtendedSuffixArraysBlocking;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.LSHMinHashBlocking;
import org.scify.jedai.blockbuilding.LSHSuperBitBlocking;
import org.scify.jedai.blockbuilding.QGramsBlocking;
import org.scify.jedai.blockbuilding.SortedNeighborhoodBlocking;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockbuilding.SuffixArraysBlocking;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.comparisoncleaning.ComparisonPropagation;
import org.scify.jedai.blockprocessing.comparisoncleaning.WeightedEdgePruning;
import org.scify.jedai.blockprocessing.IBlockProcessing;

/**
 *
 * @author G.A.P. II
 */
public enum BlockBuildingMethod {
    EXTENDED_Q_GRAMS_BLOCKING,
    EXTENDED_SORTED_NEIGHBORHOOD,
    EXTENDED_SUFFIX_ARRAYS,
    LSH_MINHASH_BLOCKING,
    LSH_SUPERBIT_BLOCKING,
    Q_GRAMS_BLOCKING,
    SORTED_NEIGHBORHOOD,
    STANDARD_BLOCKING,
    SUFFIX_ARRAYS;

    public static IBlockBuilding getDefaultConfiguration(BlockBuildingMethod blbuMethod) {
        switch (blbuMethod) {
            case EXTENDED_Q_GRAMS_BLOCKING:
                return new ExtendedQGramsBlocking();
            case EXTENDED_SORTED_NEIGHBORHOOD:
                return new ExtendedSortedNeighborhoodBlocking();
            case EXTENDED_SUFFIX_ARRAYS:
                return new ExtendedSuffixArraysBlocking();
            case LSH_MINHASH_BLOCKING:
                return new LSHMinHashBlocking();
            case LSH_SUPERBIT_BLOCKING:
                return new LSHSuperBitBlocking();
            case Q_GRAMS_BLOCKING:
                return new QGramsBlocking();
            case SORTED_NEIGHBORHOOD:
                return new SortedNeighborhoodBlocking();
            case SUFFIX_ARRAYS:
                return new SuffixArraysBlocking();
            case STANDARD_BLOCKING:
            default:
                return new StandardBlocking();
        }
    }

    public static IBlockProcessing getDefaultBlockCleaning(BlockBuildingMethod blbuMethod) {
        switch (blbuMethod) {
            case EXTENDED_Q_GRAMS_BLOCKING:
                return new BlockFiltering(0.50f);
            case EXTENDED_SORTED_NEIGHBORHOOD:
                return new BlockFiltering(0.45f);
            case EXTENDED_SUFFIX_ARRAYS:
                return null;
            case Q_GRAMS_BLOCKING:
                return new BlockFiltering(0.50f);
            case SORTED_NEIGHBORHOOD:
                return null;
            case SUFFIX_ARRAYS:
                return null;
            case STANDARD_BLOCKING:
            default:
                return new BlockFiltering(0.55f);
        }
    }

    public static IBlockProcessing getDefaultComparisonCleaning(BlockBuildingMethod blbuMethod) {
        switch (blbuMethod) {
            case EXTENDED_Q_GRAMS_BLOCKING:
                return new WeightedEdgePruning(WeightingScheme.EJS);
            case EXTENDED_SORTED_NEIGHBORHOOD:
                return new WeightedEdgePruning(WeightingScheme.JS);
            case EXTENDED_SUFFIX_ARRAYS:
                return new ComparisonPropagation();
            case Q_GRAMS_BLOCKING:
                return new WeightedEdgePruning(WeightingScheme.ECBS);
            case SORTED_NEIGHBORHOOD:
                return new ComparisonPropagation();
            case SUFFIX_ARRAYS:
                return new ComparisonPropagation();
            case STANDARD_BLOCKING:
            default:
                return new WeightedEdgePruning(WeightingScheme.CBS);
        }
    }
}
