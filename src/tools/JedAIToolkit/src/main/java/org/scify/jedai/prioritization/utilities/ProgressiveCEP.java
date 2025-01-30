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
package org.scify.jedai.prioritization.utilities;

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityEdgePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gap2
 */
public class ProgressiveCEP extends CardinalityEdgePruning {

    private final int maxComparisons;

    private List<Comparison> topComparisons;

    public ProgressiveCEP(int maxComparisons, WeightingScheme wScheme) {
        super(wScheme);
        this.maxComparisons = maxComparisons;
    }

    @Override
    public String getMethodName() {
        return "Progressive Cardinality Edge Pruning";
    }

    public List<Comparison> getTopComparisons() {
        return topComparisons;
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        setTopKEdges();
        topComparisons = new ArrayList<>(topKEdges);
        topComparisons.sort(new DecComparisonWeightComparator());
        return null;
    }

    @Override
    protected void setThreshold() {
        threshold = maxComparisons;

        Log.info(getMethodName() + " Threshold \t:\t" + threshold);
    }
}
