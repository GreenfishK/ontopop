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
import org.scify.jedai.datamodel.*;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public class ProfileMatcher extends AbstractEntityMatching {

    protected ITextModel[] entityModelsD1;
    protected ITextModel[] entityModelsD2;

    public ProfileMatcher(List<EntityProfile> profiles) {
        this(profiles, null, RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF, SimilarityMetric.COSINE_SIMILARITY);
    }
    
    public ProfileMatcher(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        this(profilesD1, profilesD2, RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF, SimilarityMetric.COSINE_SIMILARITY);
    }
    
    public ProfileMatcher(List<EntityProfile> profiles, RepresentationModel model, SimilarityMetric simMetric) {
        this(profiles, null, model, simMetric);
    }
    
    public ProfileMatcher(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, RepresentationModel model, SimilarityMetric simMetric) {
        super(profilesD1, profilesD2, model, simMetric);

        buildModels();
    }

    @Override
    protected final void buildModels() {
        if (profilesD1 == null) {
            throw new IllegalArgumentException(
                "First list of entity profiles is null. The first argument should always contain entities.");
        }

        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());

        isCleanCleanER = false;
        entityModelsD1 = getModels(DATASET_1, profilesD1);
        if (profilesD2 != null) {
            isCleanCleanER = true;
            entityModelsD2 = getModels(DATASET_2, profilesD2);
        }
    }

    @Override
    public float executeComparison(Comparison comparison) {
        if (isCleanCleanER) {
            return entityModelsD1[comparison.getEntityId1()].getSimilarity(entityModelsD2[comparison.getEntityId2()]);
        }

        return entityModelsD1[comparison.getEntityId1()].getSimilarity(entityModelsD1[comparison.getEntityId2()]);
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
    public String getMethodInfo() {
        return getMethodName() + ": it aggregates all attribute values of each entity profile "
                + "into a representation model and compares them according to the given similarity metric.";
    }

    @Override
    public String getMethodName() {
        return "Profile Matcher";
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + representationModel + "\t"
                + getParameterName(1) + "=" + simMetric;
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    private ITextModel[] getModels(int datasetId, List<EntityProfile> profiles) {
        int counter = 0;
        final ITextModel[] models = new ITextModel[profiles.size()];
        RepresentationModel.resetGlobalValues(datasetId, representationModel);
        for (EntityProfile profile : profiles) {
            models[counter] = RepresentationModel.getModel(datasetId, representationModel, simMetric, profile.getEntityUrl());
            for (Attribute attribute : profile.getAttributes()) {
                models[counter].updateModel(attribute.getValue());
            }
            models[counter].finalizeModel();
            counter++;
        }
        return models;
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "org.scify.jedai.utilities.enumerations.RepresentationModel");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "org.scify.jedai.utilities.enumerations.RepresentationModel.CHARACTER_FOURGRAMS_TF_IDF");
        obj1.put("minValue", "-");
        obj1.put("maxValue", "-");
        obj1.put("stepValue", "-");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "org.scify.jedai.utilities.enumerations.SimilarityMetric");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "org.scify.jedai.utilities.enumerations.SimilarityMetric.COSINE_SIMILARITY");
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
                return "The " + getParameterName(0) + " aggregates the textual values that correspond to every entity.";
            case 1:
                return "The " + getParameterName(1) + " compares the models of two entities, returning a value between 0 (completely dissimlar) and 1 (identical).";
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
}
