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


import org.scify.jedai.blockbuilding.SortedNeighborhoodBlocking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gap2
 */
public class SortedEntities extends SortedNeighborhoodBlocking {
    
    private int[] sortedEntityIds;
    
    public SortedEntities() {
        super();
    }
    
    public int[] getSortedEntityIds() {
        return sortedEntityIds;
    }
    
    @Override
    protected void parseIndex() {
        final Set<String> blockingKeysSet = invertedIndexD1.keySet();
        final String[] sortedTerms = blockingKeysSet.toArray(new String[0]);
        Arrays.sort(sortedTerms);

        sortedEntityIds = getSortedEntities(sortedTerms);
    }
    
    @Override
    protected void parseIndices() {
        final Set<String> blockingKeysSet = new HashSet<>();
        blockingKeysSet.addAll(invertedIndexD1.keySet());
        blockingKeysSet.addAll(invertedIndexD2.keySet());
        
        final String[] sortedTerms = blockingKeysSet.toArray(new String[0]);
        Arrays.sort(sortedTerms);

        sortedEntityIds = getMixedSortedEntities(sortedTerms);
    }
}
