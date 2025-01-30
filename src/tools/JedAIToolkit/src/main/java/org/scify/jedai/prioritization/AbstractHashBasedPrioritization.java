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

import java.util.List;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public abstract class AbstractHashBasedPrioritization extends AbstractPrioritization {
    
    protected WeightingScheme wScheme;
    
    protected final IntGridSearchConfiguration gridWScheme;
    protected final IntRandomSearchConfiguration randomWScheme;
    
    public AbstractHashBasedPrioritization (int budget, WeightingScheme wScheme) {
        super(budget);
        this.wScheme = wScheme;
        
        gridWScheme = new IntGridSearchConfiguration(WeightingScheme.values().length - 1, 0, 1);
        randomWScheme = new IntRandomSearchConfiguration(WeightingScheme.values().length, 0);
    }
    
    @Override
    public void developEntityBasedSchedule(List<EntityProfile> profilesD1) {}
    
    @Override
    public void developEntityBasedSchedule(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {}
    
    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + comparisonsBudget + ",\t"
                + getParameterName(1) + "=" + wScheme;
    }
    
    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }
    
    @Override
    public int getNumberOfGridConfigurations() {
        return gridComparisonsBudget.getNumberOfConfigurations() * gridWScheme.getNumberOfConfigurations();
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
        obj2.put("class", "org.scify.jedai.utilities.enumerations.WeightingScheme");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "org.scify.jedai.utilities.enumerations.WeightingScheme.JS");
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
                return "The " + getParameterName(0) + " defines the maximum number of pairwise comparisons that will be executed.";
            case 1:
                return "The " + getParameterName(1) + " determines the function that assigns weights to the non-redundant comparisons within each block.";
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
                return "Weighting Scheme";
            default:
                return "invalid parameter id";
        }
    }
    
    @Override
    public void setNextRandomConfiguration() {
        int schemeId = (Integer) randomWScheme.getNextRandomValue();
        wScheme = WeightingScheme.values()[schemeId];
        
        comparisonsBudget = (Integer) randomComparisonsBudget.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int schemeId = (Integer) gridWScheme.getNumberedValue(iterationNumber);
        wScheme = WeightingScheme.values()[schemeId];
        
        comparisonsBudget = (Integer) gridComparisonsBudget.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        int schemeId = (Integer) randomWScheme.getNumberedRandom(iterationNumber);
        wScheme = WeightingScheme.values()[schemeId];
        
        comparisonsBudget = (Integer) randomComparisonsBudget.getNumberedRandom(iterationNumber);
    }
}
