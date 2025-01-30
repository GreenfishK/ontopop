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
package org.scify.jedai.entitymatching;

import com.esotericsoftware.minlog.Log;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.scify.jedai.datamodel.*;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.comparators.DecSimilarityEdgeComparator;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author G.A.P. II
 */
public class GroupLinkage extends AbstractEntityMatching {

    protected float similarityThreshold;
    protected ITextModel[][] entityModelsD1;
    protected ITextModel[][] entityModelsD2;

    public GroupLinkage(List<EntityProfile> profiles) {
        this(0.1f, profiles, null, RepresentationModel.TOKEN_UNIGRAMS, SimilarityMetric.COSINE_SIMILARITY);
    }
    
    public GroupLinkage(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        this(0.1f, profilesD1, profilesD2, RepresentationModel.TOKEN_UNIGRAMS, SimilarityMetric.COSINE_SIMILARITY);
    }

    public GroupLinkage(float simThr, List<EntityProfile> profiles, RepresentationModel model, SimilarityMetric simMetric) {
        this(simThr, profiles, null, model, simMetric);
    }
    
    public GroupLinkage(float simThr, List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, RepresentationModel model, SimilarityMetric simMetric) {
        super(profilesD1, profilesD2, model, simMetric);
        
        similarityThreshold = simThr;
        
        buildModels();
    }

    @Override
    protected final void buildModels() {
        if (profilesD1 == null) {
            throw new IllegalStateException(
                "First list of entity profiles is null! The first argument should always contain entities.");
        }

        if (entityModelsD1 == null) {
            Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());

            isCleanCleanER = false;
            entityModelsD1 = getModels(DATASET_1, profilesD1);
            if (profilesD2 != null) {
                isCleanCleanER = true;
                entityModelsD2 = getModels(DATASET_2, profilesD2);
            }
        }
    }
    
    @Override
    public float executeComparison(Comparison comparison) {
        final Queue<SimilarityEdge> similarityQueue = getSimilarityEdges(comparison);
        final SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> similarityGraph = getSimilarityGraph(similarityQueue);
        int verticesNum = entityModelsD1[comparison.getEntityId1()].length;
        if (isCleanCleanER) {
            verticesNum += entityModelsD2[comparison.getEntityId2()].length;
        } else {
            verticesNum += entityModelsD1[comparison.getEntityId2()].length;
        }

        return getSimilarity(similarityGraph, verticesNum);
    }

    @Override
    public SimilarityPairs executeComparisons(List<AbstractBlock> blocks) {
        final SimilarityPairs simPairs = new SimilarityPairs(profilesD2 != null, blocks);
        blocks.stream().map(AbstractBlock::getComparisonIterator).forEachOrdered((iterator) -> {
            while (iterator.hasNext()) {
                final Comparison currentComparison = iterator.next();
                float similarity = executeComparison(currentComparison);
                if (0 < similarity) {
                    currentComparison.setUtilityMeasure(similarity);
                    simPairs.addComparison(currentComparison);
                }
            }
        });

        return simPairs;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + representationModel + "\t"
                + getParameterName(1) + "=" + simMetric + "\t"
                + getParameterName(2) + "=" + similarityThreshold;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it implements the group linkage algorithm for schema-agnostic comparison of the attribute values of two entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "Group Linkage";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves three parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".\n"
                + "3)" + getParameterDescription(2) + ".";
    }

    //Every element of the getModels list is an ITextModel[] array, corresponding to 
    //a profile. Every element of these arrays is a text-model corresponding to an attribute.
    private ITextModel[][] getModels(int datasetId, List<EntityProfile> profiles) {
        int entityCounter = 0;
        final ITextModel[][] ModelsList = new ITextModel[profiles.size()][];
        RepresentationModel.resetGlobalValues(datasetId, representationModel);
        for (EntityProfile profile : profiles) {
            int validAttributes = profile.getAttributes().stream().filter((attribute) -> (!attribute.getValue().isEmpty())).mapToInt(_item -> 1).sum();

            int counter = 0;
            ModelsList[entityCounter] = new ITextModel[validAttributes];
            for (Attribute attribute : profile.getAttributes()) {
                if (!attribute.getValue().isEmpty()) {
                    ModelsList[entityCounter][counter] = RepresentationModel.getModel(datasetId, representationModel, simMetric, attribute.getName());
                    ModelsList[entityCounter][counter].updateModel(attribute.getValue());
                    ModelsList[entityCounter][counter].finalizeModel();
                    counter++;
                }
            }
            entityCounter++;
        }

        return ModelsList;
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

        final JsonObject obj3 = new JsonObject();
        obj3.put("class", "java.lang.Float");
        obj3.put("name", getParameterName(2));
        obj3.put("defaultValue", "0.5");
        obj3.put("minValue", "0.1");
        obj3.put("maxValue", "0.95");
        obj3.put("stepValue", "0.05");
        obj3.put("description", getParameterDescription(2));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        array.add(obj3);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " builds the modules that form the model of individual attribute values.";
            case 1:
                return "The " + getParameterName(1) + " compares the models of two attribute values, returning a value between 0 (completely dissimlar) and 1 (identical).";
            case 2:
                return "The " + getParameterName(2) + " determines the similarity value over which two compared attribute values are connected with an edge in the bipartite graph.";
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
            case 2:
                return "Similarity Threshold";
            default:
                return "invalid parameter id";
        }
    }

    private float getSimilarity(SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> simGraph, int verticesNum) {
        float numerator = 0;
        float denominator = verticesNum; //m1+m2
        for (DefaultWeightedEdge e : simGraph.edgeSet()) {
            numerator += simGraph.getEdgeWeight(e);
            denominator -= 1.0;
        }
        return numerator / denominator;
    }

    private Queue<SimilarityEdge> getSimilarityEdges(Comparison comparison) {
        final ITextModel[] model1 = entityModelsD1[comparison.getEntityId1()];
        ITextModel[] model2;
        if (isCleanCleanER) {
            model2 = entityModelsD2[comparison.getEntityId2()];
        } else {
            model2 = entityModelsD1[comparison.getEntityId2()];
        }

        int s1 = model1.length;
        int s2 = model2.length;
        final Queue<SimilarityEdge> SEqueue = new PriorityQueue<>(s1 * s2, new DecSimilarityEdgeComparator());
        for (int i = 0; i < s1; i++) {
            for (int j = 0; j < s2; j++) {
                float sim = model1[i].getSimilarity(model2[j]);
                if (similarityThreshold < sim) {
                    SEqueue.add(new SimilarityEdge(i, j, sim));
                }
            }
        }

        return SEqueue;
    }

    private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> getSimilarityGraph(Queue<SimilarityEdge> seQueue) {
        final SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        while (seQueue.size() > 0) {
            SimilarityEdge se = seQueue.remove();
            int i = se.getModel1Pos();
            int j = se.getModel2Pos();
            String label1 = "a" + i;
            String label2 = "b" + j;
            if (!(graph.containsVertex(label1) || graph.containsVertex(label2))) {//only if both vertices don't exist
                graph.addVertex(label1);
                graph.addVertex(label2);
                DefaultWeightedEdge e = graph.addEdge(label1, label2);
                graph.setEdgeWeight(e, se.getSimilarity());
            }
        }

        return graph;
    }

    public void setSimilarityThreshold(float p) {
        this.similarityThreshold = p;
    }    
}
