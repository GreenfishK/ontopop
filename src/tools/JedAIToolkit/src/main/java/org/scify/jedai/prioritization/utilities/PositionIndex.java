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

import java.io.Serializable;

/**
 * @author gap2
 */
public class PositionIndex implements Serializable {

    private static final long serialVersionUID = 13483254243447435L;

    private final int noOfEntities;
    private final int[][] entityPositions;

    public PositionIndex(int entities, int[] sortedEntities) {
        noOfEntities = entities;

        int[] counters = new int[noOfEntities];
        for (int entityId : sortedEntities) {
            counters[entityId]++;
        }

        //initialize inverted index
        entityPositions = new int[noOfEntities][];
        for (int i = 0; i < noOfEntities; i++) {
            entityPositions[i] = new int[counters[i]];
            counters[i] = 0;
        }

        //build inverted index
        for (int i = 0; i < sortedEntities.length; i++) {
            int entityId = sortedEntities[i];
            entityPositions[entityId][counters[entityId]++] = i;
        }
    }

    public int[] getEntityPositions(int entityId) {
        return entityPositions[entityId];
    }
    
}
