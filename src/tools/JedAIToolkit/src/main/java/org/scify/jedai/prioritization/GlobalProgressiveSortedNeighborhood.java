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
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.utilities.comparators.DecComparisonWeightComparator;
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;
import org.scify.jedai.utilities.enumerations.ProgressiveWeightingScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * @author gap2
 * @author giovanni
 */
public class GlobalProgressiveSortedNeighborhood extends AbstractSimilarityBasedPrioritization {

    protected int maxWindow;

    protected final Queue<Comparison> topComps;

    protected final IntGridSearchConfiguration gridMaxWindow;
    protected final IntRandomSearchConfiguration randomMaxWindow;
    
    public GlobalProgressiveSortedNeighborhood(int budget, ProgressiveWeightingScheme pwScheme) {
        super(budget, pwScheme);

        topComps = new PriorityQueue<>(2 * comparisonsBudget, new IncComparisonWeightComparator());
        
        gridMaxWindow = new IntGridSearchConfiguration(10, 1, 1);
        randomMaxWindow = new IntRandomSearchConfiguration(10, 1);
    }

    @Override
    public void developEntityBasedSchedule(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        super.developEntityBasedSchedule(profilesD1, profilesD2);
        maxWindow = noOfEntities <= 100 ? 2 : (int) Math.round(Math.pow(2, Math.log10(noOfEntities) + 1)) + 1;
        getComparisons();
    }

    private void getComparisons() { //TODO: comparison propagation
        int limit = isCleanCleanER ? datasetLimit : noOfEntities;
        for (int entityId = 0; entityId < limit; entityId++) {
            final int[] entityPositions = positionIndex.getEntityPositions(entityId);
            for (int currentWindow = 1; currentWindow < maxWindow; currentWindow++) {
                for (int position : entityPositions) {
                    if (position + currentWindow < sortedEntityIds.length) {
                        if (isCleanCleanER && datasetLimit <= sortedEntityIds[position + currentWindow]
                                || !isCleanCleanER && sortedEntityIds[position + currentWindow] < entityId) {
                            updateLocalWeight(currentWindow, entityId, sortedEntityIds[position + currentWindow]);
                        }
                    }

                    if (0 <= position - currentWindow) {
                        if (isCleanCleanER && datasetLimit <= sortedEntityIds[position - currentWindow]
                                || !isCleanCleanER && sortedEntityIds[position - currentWindow] < entityId) {
                            updateLocalWeight(currentWindow, entityId, sortedEntityIds[position - currentWindow]);
                        }
                    }
                }
            }

            float minimumWeight = -1;
            for (TIntIterator iterator = distinctNeighbors.iterator(); iterator.hasNext();) {
                int neighborId = iterator.next();
                flags[neighborId] = -1;

                float weight = getWeight(entityId, neighborId);
                if (weight < minimumWeight) {
                    continue;
                }

                int entityId2 = isCleanCleanER ? neighborId - datasetLimit : neighborId;
                final Comparison c = new Comparison(isCleanCleanER, entityId, entityId2);
                c.setUtilityMeasure(weight);
                topComps.add(c);
                if (comparisonsBudget < topComps.size()) {
                    Comparison lastComparison = topComps.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        }

        final List<Comparison> topComparisons = new ArrayList<>(topComps);
        topComparisons.sort(new DecComparisonWeightComparator());
        compIterator = topComparisons.iterator();
    }

    protected float getWeight(int entityId1, int entityId2) {
        switch (pwScheme) {
            case NCF:
                float denominator = positionIndex.getEntityPositions(entityId1).length + positionIndex.getEntityPositions(entityId2).length - counters[entityId2];
                return counters[entityId2] / denominator;
            default: //ACF, ID
                return counters[entityId2];
        }
    }

    @Override
    public boolean hasNext() {
        return compIterator.hasNext();
    }

    @Override
    public Comparison next() {
        return compIterator.next();
    }

    protected void updateLocalWeight(int currentWindow, int entityId, int neighborId) {
        if (flags[neighborId] != entityId) {
            counters[neighborId] = 0;
            flags[neighborId] = entityId;
        }

        switch (pwScheme) {
            case ID:
                counters[neighborId] += 1.0 / currentWindow;
                break;
            default: // ACF, NCF
                counters[neighborId]++;
                break;
        }
        distinctNeighbors.add(neighborId);
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + comparisonsBudget + ",\t"
                + getParameterName(1) + "=" + maxWindow;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it applies directly to the input entities, sorting them according to schema-agnostic Sorted Neighborhood. "
                + "Then, it slides a window w=1 along the sorted list of entities to compare all profiles in consecutive positions. "
                + "The window size is iteratively incremented until the user-defined maximum size. "
                + "All non-redundant comparisons within these window sizes are ordered in decreasing frequency.";
    }

    @Override
    public String getMethodName() {
        return "Global Progressive Sorted Neighborhood";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridComparisonsBudget.getNumberOfConfigurations() * gridMaxWindow.getNumberOfConfigurations();
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

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Integer");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "5");
        obj1.put("minValue", "1");
        obj1.put("maxValue", "10");
        obj1.put("stepValue", "1");
        obj2.put("description", getParameterDescription(1));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " defines the maximum number of pairwise comparisons that will be executed.";
            case 1:
                return "The " + getParameterName(1) + " determines the maximum size of the window sliding over the sorted list of entities.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Budget";
            case 1:
                return "Maximum window size";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public void setNextRandomConfiguration() {
        comparisonsBudget = (Integer) randomComparisonsBudget.getNextRandomValue();
        maxWindow = (Integer) randomMaxWindow.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        comparisonsBudget = (Integer) gridComparisonsBudget.getNumberedValue(iterationNumber);
        maxWindow = (Integer) gridComparisonsBudget.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        comparisonsBudget = (Integer) randomComparisonsBudget.getNumberedRandom(iterationNumber);
        maxWindow = (Integer) randomMaxWindow.getNumberedRandom(iterationNumber);
    }
}
