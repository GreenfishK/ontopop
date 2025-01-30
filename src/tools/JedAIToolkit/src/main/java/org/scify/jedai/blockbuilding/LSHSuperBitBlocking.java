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
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import info.debatty.java.lsh.SuperBit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.configuration.gridsearch.IntGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.IntRandomSearchConfiguration;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.textmodels.SuperBitUnigrams;

/**
 *
 * @author GAP2
 */
public class LSHSuperBitBlocking extends AbstractBlockBuilding {
    private static final long serialVersionUID = 357704140172547029L;

    protected boolean d1Indexed;

    protected int bandSize;
    protected int bandsNumber;

    protected final IntGridSearchConfiguration gridBndNumber;
    protected final IntGridSearchConfiguration gridBndSize;
    protected final IntRandomSearchConfiguration randomBndNumber;
    protected final IntRandomSearchConfiguration randomBndSize;

    protected SuperBit superbit;
    protected ITextModel[][] models;

    public LSHSuperBitBlocking() {
        this(5, 5);
    }

    public LSHSuperBitBlocking(int bSize, int bandsNo) {
        super();

        bandSize = bSize;
        bandsNumber = bandsNo;

        gridBndNumber = new IntGridSearchConfiguration(100, 20, 10);
        gridBndSize = new IntGridSearchConfiguration(10, 3, 1);
        randomBndNumber = new IntRandomSearchConfiguration(100, 20);
        randomBndSize = new IntRandomSearchConfiguration(10, 2);
    }

    public void setBandSize(int bandSize) {
        this.bandSize = bandSize;
    }

    public void setBandsNumber(int bandsNumber) {
        this.bandsNumber = bandsNumber;
    }

    protected ITextModel[] buildModels(List<EntityProfile> profiles) {
        int counter = 0;
        final ITextModel[] currentModels = new ITextModel[profiles.size()];
        for (EntityProfile profile : profiles) {
            currentModels[counter] = getModel(profile.getEntityUrl());
            for (Attribute attribute : profile.getAttributes()) {
                currentModels[counter].updateModel(attribute.getValue());
            }
            currentModels[counter].finalizeModel();
            counter++;
        }
        return currentModels;
    }

    @Override
    public List<AbstractBlock> getBlocks(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        resetModel();
        models = new ITextModel[2][];
        models[DATASET_1] = buildModels(profilesD1);
        if (profilesD2 != null) {
            models[DATASET_2] = buildModels(profilesD2);
        }

        d1Indexed = false;
        initializeLshFunctions();
        return super.getBlocks(profilesD1, profilesD2);
    }

    @Override
    protected Set<String> getBlockingKeys(String attributeValue) {
        throw new UnsupportedOperationException("Not supported by LSH, because it uses global information, not local (i.e., not a mere attribute value).");
    }

    protected Set<String> getBlockingKeys(int datasetId, int profileId) {
        final SuperBitUnigrams model = (SuperBitUnigrams) models[datasetId][profileId];
        boolean[] signatures = superbit.signature(model.getVector());

        final Set<String> allKeys = new HashSet<>();
        for (int i = 0; i < bandsNumber; i++) {
            final StringBuilder band = new StringBuilder();
            for (int j = 0; j < bandSize; j++) {
                band.append("-");
                if (signatures[i * bandSize + j]) {
                    band.append("T");
                } else {
                    band.append("F");
                }
            }
            band.append("BND").append(i);
            allKeys.add(band.toString());
        }
        return allKeys;
    }

    protected ITextModel getModel(String instanceName) {
        return new SuperBitUnigrams(instanceName);
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridBndSize.getNumberOfConfigurations() * gridBndNumber.getNumberOfConfigurations();
    }

    @Override
    protected void indexEntities(Map<String, TIntList> index, List<EntityProfile> entities) {
        int datasetId = d1Indexed ? DATASET_2 : DATASET_1;
        d1Indexed = true;

        for (int profileId = 0; profileId < entities.size(); profileId++) {
            for (String key : getBlockingKeys(datasetId, profileId)) {
                TIntList entityList = index.get(key);
                if (entityList == null) {
                    entityList = new TIntArrayList();
                    index.put(key, entityList);
                }
                entityList.add(profileId);
            }
        }
    }

    protected void initializeLshFunctions() {
        Log.info("Dimensionality\t:\t" + SuperBitUnigrams.getCorpusDimensionality());
        superbit = new SuperBit(SuperBitUnigrams.getCorpusDimensionality(), bandsNumber, bandSize);
    }

    protected void resetModel() {
        SuperBitUnigrams.resetGlobalValues(DATASET_1);
    }

    @Override
    public void setNextRandomConfiguration() {
        bandSize = (Integer) randomBndSize.getNextRandomValue();
        bandsNumber = (Integer) randomBndNumber.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        int bandSizeIteration = iterationNumber / gridBndNumber.getNumberOfConfigurations();
        bandSize = (Integer) gridBndSize.getNumberedValue(bandSizeIteration);

        int msLengthIteration = iterationNumber % gridBndNumber.getNumberOfConfigurations();
        bandsNumber = (Integer) gridBndNumber.getNumberedValue(msLengthIteration);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        bandSize = (Integer) randomBndSize.getNumberedRandom(iterationNumber);
        bandsNumber = (Integer) randomBndNumber.getNumberedRandom(iterationNumber);
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + bandSize + ",\t"
                + getParameterName(1) + "=" + bandsNumber;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it creates one block for every band that appears in the signatures of at least two entities.";
    }

    @Override
    public String getMethodName() {
        return "LSH SuperBit Blocking";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Integer");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "5");
        obj1.put("minValue", "3");
        obj1.put("maxValue", "10");
        obj1.put("stepValue", "1");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Integer");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "30");
        obj2.put("minValue", "20");
        obj2.put("maxValue", "100");
        obj2.put("stepValue", "10");
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
                return "The " + getParameterName(0) + " determines the number of hash functions comprising every band.";
            case 1:
                return "The " + getParameterName(1) + " determines the number of bands, i.e., blocking keys, per entity.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Band size";
            case 1:
                return "Number of bands";
            default:
                return "invalid parameter id";
        }
    }
}
