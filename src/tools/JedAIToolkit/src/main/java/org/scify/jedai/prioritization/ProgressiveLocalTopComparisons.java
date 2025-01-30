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
import org.scify.jedai.prioritization.utilities.ProgressiveCNP;
import java.util.List;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.DecomposedBlock;
import org.scify.jedai.prioritization.utilities.ProgressiveCNPDecomponsedBlocks;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class ProgressiveLocalTopComparisons extends AbstractHashBasedPrioritization {

    public ProgressiveLocalTopComparisons(int budget) {
        this(budget, WeightingScheme.ARCS);
    }

    public ProgressiveLocalTopComparisons(int budget, WeightingScheme wScheme) {
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

            final ProgressiveCNPDecomponsedBlocks pcnp = new ProgressiveCNPDecomponsedBlocks(comparisonsBudget, blocks);
            compIterator = pcnp.getCompIterator();
        } else {
            final ProgressiveCNP pcnp = new ProgressiveCNP(comparisonsBudget, wScheme);
            pcnp.refineBlocks(blocks);
            compIterator = pcnp.getTopComparisons().iterator();
        }
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it extracts the k top-weighted comparisons per entity from the input blocks and orders all of them in decreasing matching likelihood. "
                + "It uses the same k as Cardinality Node Pruning.";
    }

    @Override
    public String getMethodName() {
        return "Progressive Local Top Comparisons";
    }

    @Override
    public boolean hasNext() {
        return compIterator.hasNext();
    }

    @Override
    public Comparison next() {
        return compIterator.next();
    }
}
