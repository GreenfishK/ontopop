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

import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.util.List;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.datamodel.RepModelSimMetricCombo;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractEntityMatching implements IEntityMatching {

    protected boolean isCleanCleanER;

    protected final IntGridSearchConfiguration gridCombo;
    protected final IntRandomSearchConfiguration randomCombo;
    protected final List<EntityProfile> profilesD1;
    protected final List<EntityProfile> profilesD2;
    protected final List<RepModelSimMetricCombo> modelMetricCombinations;
    protected RepresentationModel representationModel;
    protected SimilarityMetric simMetric;

    public AbstractEntityMatching(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, RepresentationModel model, SimilarityMetric sMetric) {
        representationModel = model;
        simMetric = sMetric;

        this.profilesD1 = profilesD1;
        this.profilesD2 = profilesD2;
        modelMetricCombinations = RepModelSimMetricCombo.getAllValidCombos();
        gridCombo = new IntGridSearchConfiguration(modelMetricCombinations.size()-1, 0, 1);
        randomCombo = new IntRandomSearchConfiguration(modelMetricCombinations.size(), 0);
    }
    
    protected abstract void buildModels();

    @Override
    public int getNumberOfGridConfigurations() {
        return gridCombo.getNumberOfConfigurations();
    }

    @Override
    public void setNextRandomConfiguration() {
        int comboId = (Integer) randomCombo.getNextRandomValue();
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        representationModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
        buildModels();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int comboId = (Integer) gridCombo.getNumberedValue(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        representationModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
        buildModels();
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        int comboId = (Integer) randomCombo.getNumberedRandom(iterationNumber);
        final RepModelSimMetricCombo selectedCombo = modelMetricCombinations.get(comboId);
        representationModel = selectedCombo.getRepModel();
        simMetric = selectedCombo.getSimMetric();
        buildModels();
    }
}
