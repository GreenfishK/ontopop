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

import com.esotericsoftware.minlog.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.atlas.json.JsonArray;

/**
 *
 * @author gap2
 */
public class StandardBlocking extends AbstractBlockBuilding {
    private static final long serialVersionUID = 1873235915732803062L;

    public StandardBlocking() {
        super();
    }

    @Override
    protected Set<String> getBlockingKeys(String attributeValue) {
        return new HashSet<>(Arrays.asList(getTokens(attributeValue)));
    }

    @Override
    public String getMethodConfiguration() {
        return PARAMETER_FREE;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates one block for every token in the attribute values of at least two entities.";
    }

    @Override
    public String getMethodName() {
        return "Standard Blocking";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " is a " + PARAMETER_FREE + ", as it uses unsupervised, schema-agnostic blocking keys:\n"
                + "every token is a blocking key.";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return 1; // the default (parameter-free) one
    }
    
    @Override
    public JsonArray getParameterConfiguration() {
        return new JsonArray();
    }

    @Override
    public String getParameterDescription(int parameterId) {
        return PARAMETER_FREE;
    }

    @Override
    public String getParameterName(int parameterId) {
        return PARAMETER_FREE;
    }

    protected String[] getTokens(String attributeValue) {
        return attributeValue.split("[\\W_]");
    }
    
    @Override
    public void setNextRandomConfiguration() {
        Log.warn("Random search is inapplicable! " + getMethodName() + " is a parameter-free method!");
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        Log.warn("Grid search is inapplicable! " + getMethodName() + " is a parameter-free method!");
    }
    
    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        Log.warn("Random search is inapplicable! " + getMethodName() + " is a parameter-free method!");
    }
}
