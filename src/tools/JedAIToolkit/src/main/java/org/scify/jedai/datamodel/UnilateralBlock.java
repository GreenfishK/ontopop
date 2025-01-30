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
import java.util.Arrays;

/**
 *
 * @author G.A.P. II
 */

public class UnilateralBlock extends AbstractBlock implements Serializable {
    
    private static final long serialVersionUID = 43532585408538695L;
    
    protected final int[] entities;
    
    public UnilateralBlock(int[] entities) {
        this(1.0f, entities);
    }
    
    public UnilateralBlock(float entropy, int[] entities) {
        super(entropy);
        this.entities = entities;
        comparisons = ((long)entities.length)*(entities.length-1)/2;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UnilateralBlock other = (UnilateralBlock) obj;
        return Arrays.equals(this.entities, other.entities);
    }

    public int[] getEntities() {
        return entities;
    }
    
    @Override
    public int getTotalBlockAssignments() {
        return entities.length;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Arrays.hashCode(this.entities);
        return hash;
    }
    
    @Override
    public void setUtilityMeasure() {
        utilityMeasure = 1.0f/entities.length;
    }
    
    @Override
    public String toString() {
        return "block index : " + blockIndex + ", utility measure : " + 
                utilityMeasure + ", #comparisons : " + getNoOfComparisons() + 
                ", entities : " + Arrays.toString(entities);
    }
}