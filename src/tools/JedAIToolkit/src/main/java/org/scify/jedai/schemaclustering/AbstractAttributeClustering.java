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
package org.scify.jedai.schemaclustering;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.RepModelSimMetricCombo;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.AttributeClusters;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.graph.ConnectedComponents;
import org.scify.jedai.utilities.graph.UndirectedGraph;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractAttributeClustering implements ISchemaClustering {
    
    protected int attributesDelimiter;
    protected int noOfAttributes;

    protected float a; // minimum portion of max similarity for connecting two attributes
    protected float[] globalMaxSimilarities;

    protected final IntGridSearchConfiguration gridCombo;
    protected final IntRandomSearchConfiguration randomCombo;
    protected ITextModel[][] attributeModels;
    protected final List<RepModelSimMetricCombo> modelMetricCombinations;
    protected Map<String, TIntList> invertedIndex;
    protected RepresentationModel repModel;
    protected SimilarityMetric simMetric;
    protected TObjectIntMap<String> attrNameIndex;

    public AbstractAttributeClustering(float a, RepresentationModel model, SimilarityMetric metric) {
        this.a = a;
        repModel = model;
        simMetric = metric;
        attributeModels = new ITextModel[2][];

        modelMetricCombinations = RepModelSimMetricCombo.getAllValidCombos();
        gridCombo = new IntGridSearchConfiguration(modelMetricCombinations.size() - 1, 0, 1);
        randomCombo = new IntRandomSearchConfiguration(modelMetricCombinations.size(), 0);
    }

    private void buildAttributeModels(int datasetId, List<EntityProfile> profiles) {
        attrNameIndex = new TObjectIntHashMap<>();
        profiles.forEach((profile) -> {
            profile.getAttributes().forEach((attribute) -> {
                attrNameIndex.putIfAbsent(attribute.getName(), attrNameIndex.size() + 1);
            });
        });

        int currentAttributes = attrNameIndex.size();
        attributeModels[datasetId] = new ITextModel[currentAttributes];
        final TObjectIntIterator<String> it = attrNameIndex.iterator();
        while (it.hasNext()) {
            it.advance();
            attributeModels[datasetId][it.value() - 1] = RepresentationModel.getModel(datasetId, repModel, simMetric, it.key());
        }

        profiles.forEach((profile) -> {
            profile.getAttributes().forEach((attribute) -> {
                updateModel(datasetId, attribute);
            });
        });

        for (int i = 0; i < currentAttributes; i++) {
            attributeModels[datasetId][i].finalizeModel();
        }
    }

    private void buildInvertedIndex() {
        invertedIndex = new HashMap<>();
        int indexedDataset = 0 < attributesDelimiter ? DATASET_2 : DATASET_1;
        for (int i = 0; i < attributeModels[indexedDataset].length; i++) {
            final Set<String> signatures = attributeModels[indexedDataset][i].getSignatures();

            for (String signature : signatures) {
                TIntList attributeIds = invertedIndex.get(signature);
                if (attributeIds == null) {
                    attributeIds = new TIntArrayList();
                    invertedIndex.put(signature, attributeIds);
                }
                attributeIds.add(i);
            }
        }
    }

    protected AttributeClusters[] clusterAttributes() {
        final UndirectedGraph similarityGraph = new UndirectedGraph(noOfAttributes);

        final TIntSet coOccurringAttrs = new TIntHashSet();
        int lastId = 0 < attributesDelimiter ? attributesDelimiter : noOfAttributes;
        for (int i = 0; i < lastId; i++) {
            coOccurringAttrs.clear();

            final Set<String> signatures = attributeModels[DATASET_1][i].getSignatures();
            for (String signature : signatures) {
                final TIntList attrIds = invertedIndex.get(signature);
                if (attrIds == null) {
                    continue;
                }
                coOccurringAttrs.addAll(attrIds);
            }

            if (0 < attributesDelimiter) { // Clean-Clean ER
                connectCleanCleanErComparisons(i, coOccurringAttrs, similarityGraph);
            } else { // Dirty ER
                connectDirtyErComparisons(i, coOccurringAttrs, similarityGraph);
            }
        }

        AttributeClusters[] aClusters;
        final ConnectedComponents cc = new ConnectedComponents(similarityGraph);
        if (attributesDelimiter < 0) { // Dirty ER
            aClusters = new AttributeClusters[1];
            aClusters[0] = clusterAttributes(DATASET_1, cc);
        } else { // Clean-Clean ER
            aClusters = new AttributeClusters[2];
            aClusters[0] = clusterAttributes(DATASET_1, cc);
            aClusters[1] = clusterAttributes(DATASET_2, cc);
        }

        return aClusters; 
    }

    protected AttributeClusters clusterAttributes(int datasetId, ConnectedComponents cc) {
        int firstId = datasetId == DATASET_1 ? 0 : attributesDelimiter;
        int lastId = 0 < attributesDelimiter && datasetId == DATASET_1 ? attributesDelimiter : noOfAttributes;

        int glueClusterId = cc.count() + 1;
        int[] clusterFrequency = new int[glueClusterId + 1];
        float[] clusterEntropy = new float[glueClusterId + 1];
        final TObjectIntMap<String> clusters = new TObjectIntHashMap<>();
        for (int i = firstId; i < lastId; i++) {
            int ccId = cc.id(i);
            if (cc.size(i) == 1) { // singleton attribute
                ccId = glueClusterId;
            }
            
            clusterFrequency[ccId]++;
            clusterEntropy[ccId] += attributeModels[datasetId][i].getEntropy(true);
            clusters.put(attributeModels[datasetId][i].getInstanceName(), ccId);
        }
        
        for (int i = 0; i < glueClusterId + 1; i++) {
            clusterEntropy[i] /= clusterFrequency[i];
        }
        
        return new AttributeClusters(clusterEntropy, clusters);
    }

    protected void compareAttributes() {
        globalMaxSimilarities = new float[noOfAttributes];
        final TIntSet coOccurringAttrs = new TIntHashSet();
        int lastId = 0 < attributesDelimiter ? attributesDelimiter : noOfAttributes;
        for (int i = 0; i < lastId; i++) {
            coOccurringAttrs.clear();

            final Set<String> signatures = attributeModels[DATASET_1][i].getSignatures();
            for (String signature : signatures) {
                final TIntList attrIds = invertedIndex.get(signature);
                if (attrIds == null) {
                    continue;
                }
                coOccurringAttrs.addAll(attrIds);
            }

            if (0 < attributesDelimiter) { // Clean-Clean ER
                executeCleanCleanErComparisons(i, coOccurringAttrs);
            } else { // Dirty ER
                executeDirtyErComparisons(i, coOccurringAttrs);
            }
        }
    }

    private void connectCleanCleanErComparisons(int attributeId, TIntSet coOccurringAttrs, UndirectedGraph similarityGraph) {
        for (TIntIterator sigIterator = coOccurringAttrs.iterator(); sigIterator.hasNext();) {
            int neighborId = sigIterator.next();

            int normalizedNeighborId = neighborId + attributesDelimiter;
            float similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_2][neighborId]);
            if (a * globalMaxSimilarities[attributeId] < similarity
                    || a * globalMaxSimilarities[normalizedNeighborId] < similarity) {
                similarityGraph.addEdge(attributeId, normalizedNeighborId);
            }
        }
    }

    private void connectDirtyErComparisons(int attributeId, TIntSet coOccurringAttrs, UndirectedGraph similarityGraph) {
        for (TIntIterator sigIterator = coOccurringAttrs.iterator(); sigIterator.hasNext();) {
            int neighborId = sigIterator.next();
            if (neighborId <= attributeId) { // avoid repeated comparisons & comparison with attributeId
                continue;
            }

            float similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_1][neighborId]);
            if (a * globalMaxSimilarities[attributeId] < similarity
                    || a * globalMaxSimilarities[neighborId] < similarity) {
                similarityGraph.addEdge(attributeId, neighborId);
            }
        }
    }

    private void executeCleanCleanErComparisons(int attributeId, TIntSet coOccurringAttrs) {
        for (TIntIterator sigIterator = coOccurringAttrs.iterator(); sigIterator.hasNext();) {
            int neighborId = sigIterator.next();

            int normalizedNeighborId = neighborId + attributesDelimiter;
            float similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_2][neighborId]);
            if (globalMaxSimilarities[attributeId] < similarity) {
                globalMaxSimilarities[attributeId] = similarity;
            }

            if (globalMaxSimilarities[normalizedNeighborId] < similarity) {
                globalMaxSimilarities[normalizedNeighborId] = similarity;
            }
        }
    }

    private void executeDirtyErComparisons(int attributeId, TIntSet coOccurringAttrs) {
        for (TIntIterator sigIterator = coOccurringAttrs.iterator(); sigIterator.hasNext();) {
            int neighborId = sigIterator.next();
            if (neighborId <= attributeId) { // avoid repeated comparisons & comparison with attributeId
                continue;
            }

            float similarity = attributeModels[DATASET_1][attributeId].getSimilarity(attributeModels[DATASET_1][neighborId]);
            if (globalMaxSimilarities[attributeId] < similarity) {
                globalMaxSimilarities[attributeId] = similarity;
            }

            if (globalMaxSimilarities[neighborId] < similarity) {
                globalMaxSimilarities[neighborId] = similarity;
            }
        }
    }

    @Override
    public AttributeClusters[] getClusters(List<EntityProfile> profiles) {
        return this.getClusters(profiles, null);
    }

    @Override
    public AttributeClusters[] getClusters(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        buildAttributeModels(DATASET_1, profilesD1);
        attributesDelimiter = -1;
        noOfAttributes = attrNameIndex.size();

        if (profilesD2 != null) {
            buildAttributeModels(DATASET_2, profilesD2);
            attributesDelimiter = noOfAttributes;
            noOfAttributes += attrNameIndex.size();
        }
        attrNameIndex = null;

        buildInvertedIndex();
        compareAttributes();

        return clusterAttributes();
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + repModel + "\t"
                + getParameterName(1) + "=" + simMetric;
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridCombo.getNumberOfConfigurations();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "org.scify.jedai.utilities.enumerations.RepresentationModel");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "org.scify.jedai.utilities.enumerations.RepresentationModel.TOKEN_UNIGRAM_GRAPHS");
        obj1.put("minValue", "-");
        obj1.put("maxValue", "-");
        obj1.put("stepValue", "-");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "org.scify.jedai.utilities.enumerations.SimilarityMetric");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "org.scify.jedai.utilities.enumerations.SimilarityMetric.GRAPH_VALUE_SIMILARITY");
        obj2.put("minValue", "-");
        obj2.put("maxValue", "-");
        obj2.put("stepValue", "-");
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
                return "The " + getParameterName(0) + " aggregates the textual items that correspond to every attribute.";
            case 1:
                return "The " + getParameterName(1) + " compares the models of two attributes, returning a value between 0 (completely dissimlar) and 1 (identical).";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Representation Model";
            case 1:
                return "Similarity Measure";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public void setNextRandomConfiguration() {
        int comboId = (Integer) randomCombo.getNextRandomValue();
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int comboId = (Integer) gridCombo.getNumberedValue(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        int comboId = (Integer) randomCombo.getNumberedRandom(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        repModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
    }

    protected abstract void updateModel(int datasetId, Attribute attribute);
}
