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
package org.scify.jedai.prioritization;

import com.esotericsoftware.minlog.Log;
import java.util.ArrayList;
import java.util.Iterator;
import org.scify.jedai.prioritization.utilities.ProgressiveCEP;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class ProgressiveGlobalTopComparisons extends AbstractHashBasedPrioritization {

    public ProgressiveGlobalTopComparisons(int budget) {
        this(budget, WeightingScheme.ARCS);
    }

    public ProgressiveGlobalTopComparisons(int budget, WeightingScheme wScheme) {
        super(budget, wScheme);
    }

    @Override
    public void developBlockBasedSchedule(List<AbstractBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("No blocks were given as input.");
        }

        if (blocks.get(0) instanceof DecomposedBlock) {
            Log.warn("Decomposed blocks were given as input!");
            Log.warn("The pre-computed comparison weights will be used!");
            
            compIterator = processDecomposedBlocks(blocks);
        } else {
            final ProgressiveCEP pcep = new ProgressiveCEP(comparisonsBudget, wScheme);
            pcep.refineBlocks(blocks);
            compIterator = pcep.getTopComparisons().iterator();
        }
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it orders all comparisons in the input blocks in descending matching likelihood.";
    }

    @Override
    public String getMethodName() {
        return "Progressive Global Top Comparisons";
    }

    @Override
    public boolean hasNext() {
        return compIterator.hasNext();
    }

    @Override
    public Comparison next() {
        return compIterator.next();
    }

    protected Iterator<Comparison> processDecomposedBlocks(List<AbstractBlock> blocks) {
        float minimumWeight = -1;
        final Queue<Comparison> topComparisons = new PriorityQueue<>(2 * comparisonsBudget, new IncComparisonWeightComparator());
        for (AbstractBlock block : blocks) {
            final ComparisonIterator cIterator = block.getComparisonIterator();
            while (cIterator.hasNext()) {
                final Comparison currentComparison = cIterator.next();
                if (minimumWeight < currentComparison.getUtilityMeasure()) {
                    topComparisons.add(currentComparison);
                    if (comparisonsBudget < topComparisons.size()) {
                        final Comparison lastComparison = topComparisons.poll();
                        minimumWeight = lastComparison.getUtilityMeasure();
                    }
                }
            }
        }
        final List<Comparison> sortedTopComparisons = new ArrayList<>(topComparisons);
        sortedTopComparisons.sort(new DecComparisonWeightComparator());
        return sortedTopComparisons.iterator();
    }
}
