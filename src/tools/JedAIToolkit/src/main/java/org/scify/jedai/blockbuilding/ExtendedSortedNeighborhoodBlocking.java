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
package org.scify.jedai.blockbuilding;

import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.UnilateralBlock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gap2
 */
public class ExtendedSortedNeighborhoodBlocking extends SortedNeighborhoodBlocking {
    private static final long serialVersionUID = 752230236150088601L;

    public ExtendedSortedNeighborhoodBlocking() {
        this(2);
    }

     public ExtendedSortedNeighborhoodBlocking(int w) {
        super(w);
        
        gridWindow = new IntGridSearchConfiguration(10, 1, 1);
        randomWindow = new IntRandomSearchConfiguration(10, 1);
    }
     
    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it improves " + super.getMethodName() + " by sliding the window over the sorted list of blocking keys.";
    }

    @Override
    public String getMethodName() {
        return "Extended Sorted Neighborhood Blocking";
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "2");
        obj1.put("minValue", "2");
        obj1.put("maxValue", "10");
        obj1.put("stepValue", "1");
        obj1.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the fixed size of the window that slides over the sorted list of blocking keys.";
            default:
                return "invalid parameter id";
        }
    }
    
    @Override
    protected void parseIndex() {
        final Set<String> blockingKeysSet = invertedIndexD1.keySet();
        final String[] sortedTerms = blockingKeysSet.toArray(new String[0]);
        Arrays.sort(sortedTerms);

        //slide window over the sorted list of blocking keys
        int upperLimit = sortedTerms.length - windowSize;
        for (int i = 0; i <= upperLimit; i++) {
            final TIntSet entityIds = new TIntHashSet();
            for (int j = 0; j < windowSize; j++) {
                entityIds.addAll(invertedIndexD1.get(sortedTerms[i + j]));
            }

            if (1 < entityIds.size()) {
                blocks.add(new UnilateralBlock(entityIds.toArray()));
            }
        }
    }

    @Override
    protected void parseIndices() {
        final Set<String> blockingKeysSet = new HashSet<>();
        blockingKeysSet.addAll(invertedIndexD1.keySet());
        blockingKeysSet.addAll(invertedIndexD2.keySet());
        final String[] sortedTerms = blockingKeysSet.toArray(new String[0]);
        Arrays.sort(sortedTerms);

        //slide window over the sorted list of blocking keys
        int upperLimit = sortedTerms.length - windowSize;
        for (int i = 0; i <= upperLimit; i++) {
            final TIntSet entityIds1 = new TIntHashSet();
            final TIntSet entityIds2 = new TIntHashSet();
            for (int j = 0; j < windowSize; j++) {
                final TIntList d1Entities = invertedIndexD1.get(sortedTerms[i + j]);
                if (d1Entities != null) {
                    entityIds1.addAll(d1Entities);
                }

                final TIntList d2Entities = invertedIndexD2.get(sortedTerms[i + j]);
                if (d2Entities != null) {
                    entityIds2.addAll(d2Entities);
                }
            }

            if (!entityIds1.isEmpty() && !entityIds2.isEmpty()) {
                blocks.add(new BilateralBlock(entityIds1.toArray(), entityIds2.toArray()));
            }
        }
    }
}
