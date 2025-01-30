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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
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
import java.util.Random;
import java.util.Set;

/**
 *
 * @author gap2
 */
public class SortedNeighborhoodBlocking extends StandardBlocking {
    private static final long serialVersionUID = -7777048266041316326L;

    protected int windowSize;
    
    protected IntGridSearchConfiguration gridWindow;
    protected IntRandomSearchConfiguration randomWindow;
    protected final Random random;
    
    public SortedNeighborhoodBlocking() {
        this(4);
    }
    
     public SortedNeighborhoodBlocking(int w) {
        super();
        windowSize = w;
        
        random = new Random();
        gridWindow = new IntGridSearchConfiguration(100, 2, 1);
        randomWindow = new IntRandomSearchConfiguration(100, 1);
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + windowSize;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates blocks based on the similarity of the blocking keys of Standard Blocking:\n"
                + "it sorts the keys in alphabetical order, it sorts the entities accordingly and then, it slides a window over the sorted list of entities;\n"
                + "the entities that co-occur inside the window in every iteration form a block and are compared with each other.";
    }

    @Override
    public String getMethodName() {
        return "Sorted Neighborhood Blocking";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }
    
    protected int[] getMixedSortedEntities(String[] sortedTerms) {
        int datasetLimit = entityProfilesD1.size();
        final TIntList sortedEntityIds = new TIntArrayList();

        for (String blockingKey : sortedTerms) {
            final TIntList sortedIds = new TIntArrayList();
            final TIntList d1EntityIds = invertedIndexD1.get(blockingKey);
            if (d1EntityIds != null) {
                sortedIds.addAll(d1EntityIds);
            }

            final TIntList d2EntityIds = invertedIndexD2.get(blockingKey);
            if (d2EntityIds != null) {
                for (TIntIterator iterator = d2EntityIds.iterator(); iterator.hasNext();) {
                    sortedIds.add(datasetLimit + iterator.next());
                }
            }

            sortedIds.shuffle(random);
            sortedEntityIds.addAll(sortedIds);
        }

        return sortedEntityIds.toArray();
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridWindow.getNumberOfConfigurations();
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "4");
        obj1.put("minValue", "2");
        obj1.put("maxValue", "100");
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
                return "The " + getParameterName(0) + " determines the fixed size of the window that slides over the sorted list of entities.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Window Size";
            default:
                return "invalid parameter id";
        }
    }
    
    protected int[] getSortedEntities(String[] sortedTerms) {
        final TIntList sortedEntityIds = new TIntArrayList();

        for (String blockingKey : sortedTerms) {
            final TIntList sortedIds = invertedIndexD1.get(blockingKey);
            sortedIds.shuffle(random);
            sortedEntityIds.addAll(sortedIds);
        }

        return sortedEntityIds.toArray();
    }
    
    @Override
    protected void parseIndex() {
        final Set<String> blockingKeysSet = invertedIndexD1.keySet();
        final String[] sortedTerms = blockingKeysSet.toArray(new String[0]);
        Arrays.sort(sortedTerms);

        final int[] allEntityIds = getSortedEntities(sortedTerms);

        //slide window over the sorted list of entity ids
        int upperLimit = allEntityIds.length - windowSize;
        for (int i = 0; i <= upperLimit; i++) {
            final TIntSet entityIds = new TIntHashSet();
            for (int j = 0; j < windowSize; j++) {
                entityIds.add(allEntityIds[i + j]);
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

        final int[] allEntityIds = getMixedSortedEntities(sortedTerms);

        int datasetLimit = entityProfilesD1.size();
        //slide window over the sorted list of entity ids
        int upperLimit = allEntityIds.length - windowSize;
        for (int i = 0; i <= upperLimit; i++) {
            final TIntSet entityIds1 = new TIntHashSet();
            final TIntSet entityIds2 = new TIntHashSet();
            for (int j = 0; j < windowSize; j++) {
                if (allEntityIds[i + j] < datasetLimit) {
                    entityIds1.add(allEntityIds[i + j]);
                } else {
                    entityIds2.add(allEntityIds[i + j] - datasetLimit);
                }
            }

            if (!entityIds1.isEmpty() && !entityIds2.isEmpty()) {
                blocks.add(new BilateralBlock(entityIds1.toArray(), entityIds2.toArray()));
            }
        }
    }
    
    @Override
    public void setNextRandomConfiguration() {
        windowSize = (Integer) randomWindow.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        windowSize = (Integer) gridWindow.getNumberedValue(iterationNumber);
    }
    
    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        windowSize = (Integer) randomWindow.getNumberedRandom(iterationNumber);
    }
}
