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
package org.scify.jedai.entityclustering;

import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;

import com.esotericsoftware.minlog.Log;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.DblGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.DblRandomSearchConfiguration;
import org.scify.jedai.utilities.graph.ConnectedComponents;

import org.scify.jedai.utilities.graph.UndirectedGraph;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractEntityClustering implements IEntityClustering {
    private static final long serialVersionUID = 8778225257965382090L;

    protected boolean isCleanCleanER;

    protected float threshold;

    protected int noOfEntities;
    protected int datasetLimit;

    protected final DblGridSearchConfiguration gridThreshold;
    protected final DblRandomSearchConfiguration randomThreshold;
    protected UndirectedGraph similarityGraph;

    public AbstractEntityClustering(float simTh) {
        threshold = simTh;
        
        gridThreshold = new DblGridSearchConfiguration(1.00f, 0.05f, 0.05f);
        randomThreshold = new DblRandomSearchConfiguration(0.99f, 0.01f);
    }

    protected EquivalenceCluster[] getConnectedComponents() {
        final ConnectedComponents cc = new ConnectedComponents(similarityGraph);
        final EquivalenceCluster[] connectedComponents = new EquivalenceCluster[cc.count()];
        for (int i = 0; i < cc.count(); i++) {
            connectedComponents[i] = new EquivalenceCluster();
        }
        
        if (isCleanCleanER) {
            for (int i = 0; i < datasetLimit; i++) {
                int ccId = cc.id(i);
                connectedComponents[ccId].addEntityIdD1(i);
            }
            for (int i = datasetLimit; i < noOfEntities; i++) {
                int ccId = cc.id(i);
                connectedComponents[ccId].addEntityIdD2(i-datasetLimit);
            }
        } else {
            for (int i = 0; i < noOfEntities; i++) {
                int ccId = cc.id(i);
                connectedComponents[ccId].addEntityIdD1(i);
            }
        }

        return connectedComponents;
    }

    protected int getMaxEntityId(int[] entityIds) {
        int maxId = Integer.MIN_VALUE;
        for (int entityId : entityIds) {
            if (maxId < entityId) {
                maxId = entityId;
            }
        }
        return maxId;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + threshold;
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridThreshold.getNumberOfConfigurations();
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Float");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "0.1");
        obj1.put("minValue", "0.1");
        obj1.put("maxValue", "0.95");
        obj1.put("stepValue", "0.05");
        obj1.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the cut-off similarity threshold for connecting two entities with an edge in the (initial) similarity graph.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Similarity Threshold";
            default:
                return "invalid parameter id";
        }
    }

    protected void initializeData(SimilarityPairs simPairs) {
        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());
        
//        simPairs.normalizeSimilarities();
        isCleanCleanER = simPairs.isCleanCleanER();
        
        int maxEntity1 = getMaxEntityId(simPairs.getEntityIds1());
        int maxEntity2 = getMaxEntityId(simPairs.getEntityIds2());
        if (simPairs.isCleanCleanER()) {
            datasetLimit = maxEntity1 + 1;
            noOfEntities = maxEntity1 + maxEntity2 + 2;
        } else {
            datasetLimit = 0;
            noOfEntities = Math.max(maxEntity1, maxEntity2) + 1;
        }

        similarityGraph = new UndirectedGraph(noOfEntities);
    }

    @Override
    public void setNextRandomConfiguration() {
        threshold = (Float) randomThreshold.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        threshold = (Float) gridThreshold.getNumberedValue(iterationNumber);
    }
    
    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        threshold = (Float) randomThreshold.getNumberedRandom(iterationNumber);
    }
    
    @Override
    public void setSimilarityThreshold(float th) {
        threshold = th;
        Log.info("Similarity threshold : " + threshold);
    }
}
