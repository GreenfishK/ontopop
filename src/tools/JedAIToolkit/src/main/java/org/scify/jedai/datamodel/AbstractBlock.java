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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */

public abstract class AbstractBlock implements Serializable {
    
    private static final long serialVersionUID = 7526443743449L;
    
    protected int blockIndex;
    
    protected long comparisons;
    protected float entropy;
    protected float utilityMeasure;
            
    public AbstractBlock(float en) {
        blockIndex = -1;
        entropy = 1.0f;
        utilityMeasure = -1;
    }
    
    public int getBlockIndex() {
        return blockIndex;
    }
    
    public ComparisonIterator getComparisonIterator() {
        return new ComparisonIterator(this);
    }
    
    public float getEntropy() {
        return entropy;
    }

    public long getNoOfComparisons() {
        return comparisons;
    }
    
    public float getUtilityMeasure() {
        return utilityMeasure;
    }
    
    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }
    
    public List<Comparison> getComparisons() {
        final List<Comparison> comparisonsList = new ArrayList<>();

        final ComparisonIterator iterator = getComparisonIterator();
        while (iterator.hasNext()) {
            Comparison comparison = iterator.next();
            comparisonsList.add(comparison);
        }
        
        return comparisonsList;
    }
    
    public abstract int getTotalBlockAssignments();
    public abstract void setUtilityMeasure();
}