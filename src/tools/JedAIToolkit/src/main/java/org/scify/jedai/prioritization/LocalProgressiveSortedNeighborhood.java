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

import gnu.trove.iterator.TIntIterator;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.ProgressiveWeightingScheme;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gap2
 */
public class LocalProgressiveSortedNeighborhood extends AbstractSimilarityBasedPrioritization {

    protected int currentWindow;

    public LocalProgressiveSortedNeighborhood(int budget, ProgressiveWeightingScheme pwScheme) {
        super(budget, pwScheme);

        currentWindow = 0;
    }

    protected void getWindowComparisons() {
        currentWindow++;
        int limit = isCleanCleanER ? datasetLimit : noOfEntities;
        final List<Comparison> windowComparisons = new ArrayList<>();
        for (int entityId = 0; entityId < limit; entityId++) {
            distinctNeighbors.clear();

            for (int position : positionIndex.getEntityPositions(entityId)) {
                if (position + currentWindow < sortedEntityIds.length) {
                    if (isCleanCleanER && datasetLimit <= sortedEntityIds[position + currentWindow]
                            || !isCleanCleanER && sortedEntityIds[position + currentWindow] < entityId) {
                        updateCounters(entityId, sortedEntityIds[position + currentWindow]);
                    }
                }

                if (0 <= position - currentWindow) {
                    if (isCleanCleanER && datasetLimit <= sortedEntityIds[position - currentWindow]
                            || !isCleanCleanER && sortedEntityIds[position - currentWindow] < entityId) {
                        updateCounters(entityId, sortedEntityIds[position - currentWindow]);
                    }
                }
            }

            for (TIntIterator iterator = distinctNeighbors.iterator(); iterator.hasNext();) {
                int neighborId = iterator.next();
                flags[neighborId] = -1;

                int entityId2 = isCleanCleanER ? neighborId - datasetLimit : neighborId;
                final Comparison c = new Comparison(isCleanCleanER, entityId, entityId2);
                c.setUtilityMeasure(getWeight(entityId, neighborId));
                windowComparisons.add(c);
            }
        }

        windowComparisons.sort(new DecComparisonWeightComparator());
        compIterator = windowComparisons.iterator();
    }

    protected float getWeight(int entityId1, int entityId2) {
        switch (pwScheme) {
            case ACF:
                return counters[entityId2];
            case NCF:
                float denominator = positionIndex.getEntityPositions(entityId1).length + positionIndex.getEntityPositions(entityId2).length - counters[entityId2];
                return counters[entityId2] / denominator;
        }
        return -1;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + comparisonsBudget;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it applies directly to the input entities, sorting them according to schema-agnostic Sorted Neighborhood. "
                + "Then, it slides a window w=1 along the sorted list of entities to compare all profiles in consecutive positions. "
                + "The window size is iteratively incremented until reaching the user-defined budget. "
                + "In each window size, the initialization phase orders non-redundant comparisons in decreasing frequency.";
    }

    @Override
    public String getMethodName() {
        return "Local Progressive Sorted Neighborhood";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridComparisonsBudget.getNumberOfConfigurations();
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "10000");
        obj1.put("minValue", "1000");
        obj1.put("maxValue", "1000000");
        obj1.put("stepValue", "1000");
        obj1.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " defines the maximum number of pairwise comparisons that will be executed.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Budget";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public boolean hasNext() {
        return emittedComparisons < comparisonsBudget && currentWindow < sortedEntityIds.length;
    }

    @Override
    public Comparison next() {
        if (compIterator == null || !compIterator.hasNext()) {
            getWindowComparisons();
        }

        emittedComparisons++;
        return compIterator.next();
    }

    @Override
    public void setNextRandomConfiguration() {
        comparisonsBudget = (Integer) randomComparisonsBudget.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        comparisonsBudget = (Integer) gridComparisonsBudget.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        comparisonsBudget = (Integer) randomComparisonsBudget.getNumberedRandom(iterationNumber);
    }
    
    private void updateCounters(int entityId, int neighborId) {
        if (flags[neighborId] != entityId) {
            counters[neighborId] = 0;
            flags[neighborId] = entityId;
        }

        counters[neighborId]++;
        distinctNeighbors.add(neighborId);
    }
}
