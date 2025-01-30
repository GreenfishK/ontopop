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

/**
 *
 * @author G.A.P. II
 */

public class DecomposedBlock extends AbstractBlock {
    // A type of block that comprises blocks of minimum size in the form of 2
    // int[] for higher efficiency. Only comparisons between entities1 and entities2
    // and for the same index are allowed, i.e., entities1[i] is exclusively comparable 
    // with entities2[i]. No redundant comparisons should be present.
    private final boolean cleanCleanER;
    
    private int[] blockIndices;
    private final int[] entities1;
    private final int[] entities2;
    private final int[] weights;
    
    public DecomposedBlock(boolean ccER, int[] entities1, int[] entities2, int[] weights) {
        super(1.0f);
        if (entities1.length != entities2.length) {
            System.err.println("\n\nCreating imbalanced decomposed block!!!!");
            System.err.println("Entities 1\t:\t" + entities1.length);
            System.err.println("Entities 2\t:\t" + entities2.length);
        }
        cleanCleanER = ccER;
        this.entities1 = entities1;
        this.entities2 = entities2;
        this.weights = weights;
        blockIndices = null;
    }
    
    public int[] getBlockIndices() {
        return blockIndices;
    }
    
    public int[] getEntities1() {
        return entities1;
    }

    public int[] getEntities2() {
        return entities2;
    }
    
    @Override
    public long getNoOfComparisons() {
        return entities1.length;
    }

    @Override
    public int getTotalBlockAssignments() {
        return 2*entities1.length;
    }
    
    public int[] getWeights() {
        return weights;
    }
    
    public boolean isCleanCleanER() {
        return cleanCleanER;
    }
    
    @Override
    public void setBlockIndex(int startingIndex) {
        blockIndex = startingIndex;
        blockIndices = new int[entities1.length];
        for (int i = 0; i < entities1.length; i++) {
            blockIndices[i] = startingIndex+i;
        }
    }

    @Override
    public void setUtilityMeasure() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}