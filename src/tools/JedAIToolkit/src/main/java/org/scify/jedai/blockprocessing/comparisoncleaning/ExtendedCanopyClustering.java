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
package org.scify.jedai.blockprocessing.comparisoncleaning;

import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.utilities.enumerations.WeightingScheme;


import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.HashSet;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.utilities.comparators.IncComparisonWeightComparator;

/**
 *
 * @author gap2
 */
public class ExtendedCanopyClustering extends CardinalityNodePruning {
    private static final long serialVersionUID = -1774443473844341504L;
    protected int inclusiveThreshold;
    protected int exclusiveThreshold;
    
    protected TIntSet excludedEntities;
    
    public ExtendedCanopyClustering() {
        this(10, 1, WeightingScheme.ARCS);
    }
    
    public ExtendedCanopyClustering(int inThr, int outThr) {
        this(inThr, outThr, WeightingScheme.ARCS);
    }

    public ExtendedCanopyClustering(int inThr, int outThr, WeightingScheme scheme) {
        super(scheme);
        nodeCentric = true;
        exclusiveThreshold = outThr;
        inclusiveThreshold = inThr;
        if (inclusiveThreshold < exclusiveThreshold) {
            throw new IllegalStateException("The Exclusive Threshold cannot be larger than the Inclusive one.");
        }
    }

    public void setInclusiveThreshold(int inclusiveThreshold) {
        this.inclusiveThreshold = inclusiveThreshold;
    }

    public void setExclusiveThreshold(int exclusiveThreshold) {
        this.exclusiveThreshold = exclusiveThreshold;
    }

    public void setWeightingScheme(WeightingScheme weightingScheme) {
        this.weightingScheme = weightingScheme;
    }
    
    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + inclusiveThreshold + ",\t"
                + getParameterName(1) + "=" + exclusiveThreshold;
    }
    
    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains for every entity, "
                + "the comparisons that correspond to its top-InclusiveThreshold weighted edges in the blocking graph. "
                + "Also, the ExclusiveThreshold-most similar entities of each entity are not associated with any other entity. ";
    }

    @Override
    public String getMethodName() {
        return "Extended Canopy Clustering";
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "10");
        obj1.put("minValue", "1");
        obj1.put("maxValue", "20");
        obj1.put("stepValue", "1");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Integer");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "1");
        obj2.put("minValue", "1");
        obj2.put("maxValue", "10");
        obj2.put("stepValue", "1");
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
                return "The " + getParameterName(0) + " defines the maximum number of retained comparisons/edges per entity/node.";
            case 1:
                return "The " + getParameterName(1) + " defines the maximum number of nodes/entities that are removed from the "
                        + "blocking graph so that they are not considered as candidate matches for any other node.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        final TIntList entityIds = new TIntArrayList();
        for (int i = 0; i < noOfEntities; i++) {
            entityIds.add(i);
        }
        entityIds.shuffle(new Random());
        final TIntIterator iterator = entityIds.iterator();
        
        excludedEntities = new TIntHashSet();
        nearestEntities = new HashSet[noOfEntities];
        topKEdges = new PriorityQueue<>(2 * inclusiveThreshold, new IncComparisonWeightComparator());
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            while (iterator.hasNext()) {
                int currentId = iterator.next();
                processArcsEntity(currentId);
                verifyValidEntities(currentId);
            }
        } else {
            while (iterator.hasNext()) {
                int currentId = iterator.next();
                processEntity(currentId);
                verifyValidEntities(currentId);
            }
        }

        return retainValidComparisons();
    }

    @Override
    protected void setThreshold() { // not needed for this method
    }

    @Override
    protected void verifyValidEntities(int entityId) {
        if (validEntities.isEmpty()) {
            return;
        }

        topKEdges.clear();
        minimumWeight = Float.MIN_VALUE;
        for (TIntIterator iterator = validEntities.iterator(); iterator.hasNext();) {
            int neighborId = iterator.next();
            if (excludedEntities.contains(neighborId)) {
                continue;
            }
            
            float weight = getWeight(entityId, neighborId);
            if (!(weight < minimumWeight)) {
                final Comparison comparison = new Comparison(cleanCleanER, -1, neighborId);
                comparison.setUtilityMeasure(weight);
                topKEdges.add(comparison);
                if (inclusiveThreshold < topKEdges.size()) {
                    Comparison lastComparison = topKEdges.poll();
                    minimumWeight = lastComparison.getUtilityMeasure();
                }
            }
        }

        int counter = 0;
        int freedEntities = inclusiveThreshold - exclusiveThreshold;
        nearestEntities[entityId] = new HashSet<>(topKEdges);
        for (Comparison comparison : topKEdges) {
            counter++;
            if (freedEntities < counter) {
                excludedEntities.add(comparison.getEntityId2());
            }
        }
    }
}
