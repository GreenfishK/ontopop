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
package org.scify.jedai.datamodel;

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.utilities.IConstants;

import java.util.Iterator;

/**
 *
 * @author G.A.P. II
 */
public class ComparisonIterator implements IConstants, Iterator<Comparison> {

    private long executedComparisons;
    private final long totalComparisons;

    private int innerLoop;
    private int innerLimit;
    private int outerLoop;
    private int outerLimit;

    private final AbstractBlock block;

    ComparisonIterator(AbstractBlock block) {
        this.block = block;
        executedComparisons = 0;
        totalComparisons = block.getNoOfComparisons();

        if (block instanceof BilateralBlock) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            innerLoop = -1; // so that counting in function next() starts from 0
            innerLimit = bilBlock.getIndex2Entities().length - 1;
            outerLoop = 0;
            outerLimit = bilBlock.getIndex1Entities().length - 1;
        } else if (block instanceof UnilateralBlock) {
            UnilateralBlock uniBlock = (UnilateralBlock) block;
            innerLoop = 0;
            innerLimit = uniBlock.getEntities().length - 1;
            outerLoop = 0;
            outerLimit = uniBlock.getEntities().length - 1;
        } else if (block instanceof DecomposedBlock) {
            innerLoop = -1;
            innerLimit = -1;
            outerLoop = -1; // so that counting in function next() starts from 0
            outerLimit = -1;
        }
    }

    @Override
    public boolean hasNext() {
        return executedComparisons < totalComparisons;
    }

    @Override
    public Comparison next() {
        if (totalComparisons <= executedComparisons) {
            Log.error("All comparisons were already executed!");
            return null;
        }

        executedComparisons++;
        if (block instanceof BilateralBlock) {
            BilateralBlock bilBlock = (BilateralBlock) block;
            innerLoop++;
            if (innerLimit < innerLoop) {
                innerLoop = 0;
                outerLoop++;
                if (outerLimit < outerLoop) {
                    Log.error("All comparisons were already executed!");
                    return null;
                }
            }

            return new Comparison(true, bilBlock.getIndex1Entities()[outerLoop], bilBlock.getIndex2Entities()[innerLoop]);
        } else if (block instanceof UnilateralBlock) {
            UnilateralBlock uniBlock = (UnilateralBlock) block;
            innerLoop++;
            if (innerLimit < innerLoop) {
                outerLoop++;
                if (outerLimit < outerLoop) {
                    Log.error("All comparisons were already executed!");
                    return null;
                }
                innerLoop = outerLoop + 1;
            }

            if (uniBlock.getEntities()[outerLoop] < uniBlock.getEntities()[innerLoop]) {
                return new Comparison(false, uniBlock.getEntities()[outerLoop], uniBlock.getEntities()[innerLoop]);
            } else if (uniBlock.getEntities()[innerLoop] < uniBlock.getEntities()[outerLoop]) {
                return new Comparison(false, uniBlock.getEntities()[innerLoop], uniBlock.getEntities()[outerLoop]);
            }
        } else if (block instanceof DecomposedBlock) {
            DecomposedBlock deBlock = (DecomposedBlock) block;
            outerLoop++;
            Comparison c = null;
            if (deBlock.isCleanCleanER()) {
                c = new Comparison(deBlock.isCleanCleanER(), deBlock.getEntities1()[outerLoop], deBlock.getEntities2()[outerLoop]);
            } else {
                if (deBlock.getEntities1()[outerLoop] < deBlock.getEntities2()[outerLoop]) {
                    c = new Comparison(deBlock.isCleanCleanER(), deBlock.getEntities1()[outerLoop], deBlock.getEntities2()[outerLoop]);
                } else if (deBlock.getEntities2()[outerLoop] < deBlock.getEntities1()[outerLoop]) {
                    c = new Comparison(deBlock.isCleanCleanER(), deBlock.getEntities2()[outerLoop], deBlock.getEntities1()[outerLoop]);
                }
            }
            c.setUtilityMeasure(deBlock.getWeights()[outerLoop] / DISCRETIZATION_FACTOR);
            return c;
        }

        return null;
    }
}
