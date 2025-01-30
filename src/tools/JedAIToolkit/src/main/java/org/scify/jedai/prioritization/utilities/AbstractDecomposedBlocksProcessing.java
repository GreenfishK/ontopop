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

import java.util.Iterator;
import java.util.List;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;

/**
 *
 * @author gap2
 */
public class AbstractDecomposedBlocksProcessing {
    
    protected boolean isCleanCleanER;

    protected int datasetLimit;
    protected int noOfEntities;
    
    protected Iterator<Comparison> compIterator;
    
    public AbstractDecomposedBlocksProcessing(List<AbstractBlock> inputBlocks) {
        isCleanCleanER = inputBlocks.get(0).getComparisons().get(0).isCleanCleanER();
        countEntities(inputBlocks);
    }
    
    private void countEntities(List<AbstractBlock> inputBlocks) {
        datasetLimit = -1;
        noOfEntities = -1;
        if (isCleanCleanER) {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison currentComparison = cIterator.next();
                    if (noOfEntities < currentComparison.getEntityId1() + 1) {
                        noOfEntities = currentComparison.getEntityId1() + 1;
                    }

                    if (datasetLimit < currentComparison.getEntityId2() + 1) {
                        datasetLimit = currentComparison.getEntityId2() + 1;
                    }
                }
            }

            int temp = noOfEntities;
            noOfEntities += datasetLimit;
            datasetLimit = temp;
        } else {
            for (AbstractBlock block : inputBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    final Comparison currentComparison = cIterator.next();
                    if (noOfEntities < currentComparison.getEntityId1() + 1) {
                        noOfEntities = currentComparison.getEntityId1() + 1;
                    }
                    if (noOfEntities < currentComparison.getEntityId2() + 1) {
                        noOfEntities = currentComparison.getEntityId2() + 1;
                    }
                }
            }
        }
    }
    
    public Iterator<Comparison> getCompIterator() {
        return compIterator;
    }
 }
