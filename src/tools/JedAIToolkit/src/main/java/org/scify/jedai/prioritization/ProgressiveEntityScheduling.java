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

import java.util.Iterator;
import java.util.List;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.VertexWeight;
import org.scify.jedai.prioritization.utilities.ProgressiveEntityComparisons;
import org.scify.jedai.prioritization.utilities.ProgressiveWNP;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class ProgressiveEntityScheduling extends AbstractHashBasedPrioritization {

    protected int comparisonCounter;

    protected Iterator<VertexWeight> entityIterator;
    protected ProgressiveEntityComparisons pec;

    public ProgressiveEntityScheduling(int budget, WeightingScheme wScheme) {
        super(budget, wScheme);
        comparisonCounter = 0;
    }

    @Override
    public void developBlockBasedSchedule(List<AbstractBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("No blocks were given as input.");
        }

        final ProgressiveWNP pwnp = new ProgressiveWNP(wScheme);
        pwnp.refineBlocks(blocks);
        compIterator = pwnp.getSortedTopComparisons().iterator();
        entityIterator = pwnp.getSortedEntities().iterator();

        pec = new ProgressiveEntityComparisons(wScheme);
        pec.refineBlocks(blocks);
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it orders all entities in decreasing average comparison weight \n"
                + "and then prioritizes all comparisons involving the current entity by ordering them in decreasing matching likelihood.";
    }

    @Override
    public String getMethodName() {
        return "Progressive Entity Scheduling";
    }

    @Override
    public boolean hasNext() {
        if (comparisonCounter < comparisonsBudget) {
            if (!compIterator.hasNext()) {
                while (entityIterator.hasNext()) {
                    final VertexWeight currentEntity = entityIterator.next();
                    final List<Comparison> entityComparisons = pec.getSortedEntityComparisons(currentEntity.getPos());
                    if (entityComparisons != null && !entityComparisons.isEmpty()) {
                        compIterator = entityComparisons.iterator();
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public Comparison next() {
        comparisonCounter++;
        return compIterator.next();
    }
}
