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
package org.scify.jedai.blockprocessing.blockcleaning;

import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.blockprocessing.AbstractBlockProcessing;
import org.scify.jedai.configuration.gridsearch.DblGridSearchConfiguration;
import org.scify.jedai.configuration.randomsearch.DblRandomSearchConfiguration;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.BilateralBlock;
import org.scify.jedai.datamodel.UnilateralBlock;
import org.scify.jedai.utilities.comparators.IncBlockCardinalityComparator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gap2
 */
public class BlockFiltering extends AbstractBlockProcessing {
    private static final long serialVersionUID = -4121232084289963719L;

    protected float ratio;

    protected int entitiesD1;
    protected int entitiesD2;
    protected int[] counterD1;
    protected int[] counterD2;
    protected int[] limitsD1;
    protected int[] limitsD2;

    protected final DblGridSearchConfiguration gridRatio;
    protected final DblRandomSearchConfiguration randomRatio;

    public BlockFiltering() {
        this(0.8f);
    }

    public BlockFiltering(float r) {
        ratio = r;

        gridRatio = new DblGridSearchConfiguration(1.0f, 0.025f, 0.025f);
        randomRatio = new DblRandomSearchConfiguration(1.0f, 0.01f);
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    protected void countEntities(List<AbstractBlock> blocks) {
        entitiesD1 = Integer.MIN_VALUE;
        entitiesD2 = Integer.MIN_VALUE;
        if (blocks.get(0) instanceof BilateralBlock) {
            blocks.stream().map((block) -> (BilateralBlock) block).map((bilBlock) -> {
                for (int id1 : bilBlock.getIndex1Entities()) {
                    if (entitiesD1 < id1 + 1) {
                        entitiesD1 = id1 + 1;
                    }
                }
                return bilBlock;
            }).forEachOrdered((bilBlock) -> {
                for (int id2 : bilBlock.getIndex2Entities()) {
                    if (entitiesD2 < id2 + 1) {
                        entitiesD2 = id2 + 1;
                    }
                }
            });
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            blocks.stream().map((block) -> (UnilateralBlock) block).forEachOrdered((uniBlock) -> {
                for (int id : uniBlock.getEntities()) {
                    if (entitiesD1 < id + 1) {
                        entitiesD1 = id + 1;
                    }
                }
            });
        }
    }

    protected void getBilateralLimits(List<AbstractBlock> blocks) {
        limitsD1 = new int[entitiesD1];
        limitsD2 = new int[entitiesD2];
        blocks.stream().map((block) -> (BilateralBlock) block).map((bilBlock) -> {
            for (int id1 : bilBlock.getIndex1Entities()) {
                limitsD1[id1]++;
            }
            return bilBlock;
        }).forEachOrdered((bilBlock) -> {
            for (int id2 : bilBlock.getIndex2Entities()) {
                limitsD2[id2]++;
            }
        });

        for (int i = 0; i < limitsD1.length; i++) {
            limitsD1[i] = Math.round(ratio * limitsD1[i]);
        }
        for (int i = 0; i < limitsD2.length; i++) {
            limitsD2[i] = Math.round(ratio * limitsD2[i]);
        }
    }

    protected void getLimits(List<AbstractBlock> blocks) {
        if (blocks.get(0) instanceof BilateralBlock) {
            getBilateralLimits(blocks);
        } else if (blocks.get(0) instanceof UnilateralBlock) {
            getUnilateralLimits(blocks);
        }
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + ratio;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it retains every entity in a subset of its smallest blocks.";
    }

    @Override
    public String getMethodName() {
        return "Block Filtering";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves a single parameter:\n"
                + "1)" + getParameterDescription(0) + ".\n";
    }

    @Override
    public int getNumberOfGridConfigurations() {
        return gridRatio.getNumberOfConfigurations();
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj = new JsonObject();
        obj.put("class", "java.lang.Float");
        obj.put("name", getParameterName(0));
        obj.put("defaultValue", "0.8");
        obj.put("minValue", "0.025");
        obj.put("maxValue", "1.0");
        obj.put("stepValue", "0.025");
        obj.put("description", getParameterDescription(0));

        final JsonArray array = new JsonArray();
        array.add(obj);

        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " specifies the portion of the retained smaller blocks per entity.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Filtering Ratio";
            default:
                return "invalid parameter id";
        }
    }

    protected void getUnilateralLimits(List<AbstractBlock> blocks) {
        limitsD1 = new int[entitiesD1];
        limitsD2 = null;
        blocks.stream().map((block) -> (UnilateralBlock) block).forEachOrdered((uniBlock) -> {
            for (int id : uniBlock.getEntities()) {
                limitsD1[id]++;
            }
        });

        for (int i = 0; i < limitsD1.length; i++) {
            limitsD1[i] = Math.round(ratio * limitsD1[i]);
        }
    }

    protected void initializeCounters() {
        counterD1 = new int[entitiesD1];
        counterD2 = null;
        if (0 < entitiesD2) {
            counterD2 = new int[entitiesD2];
        }
    }

    @Override
    public List<AbstractBlock> refineBlocks(List<AbstractBlock> blocks) {
        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());

        if (blocks.isEmpty()) {
            Log.warn("Empty set of blocks was given as input!");
            return blocks;
        }
        
        printOriginalStatistics(blocks);
        countEntities(blocks);
        sortBlocks(blocks);
        getLimits(blocks);
        initializeCounters();
        return restructureBlocks(blocks);
    }

    protected List<AbstractBlock> restructureBilateraBlocks(List<AbstractBlock> blocks) {
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        blocks.stream().map((block) -> (BilateralBlock) block).forEachOrdered((oldBlock) -> {
            final TIntList retainedEntitiesD1 = new TIntArrayList();
            for (int entityId : oldBlock.getIndex1Entities()) {
                if (counterD1[entityId] < limitsD1[entityId]) {
                    retainedEntitiesD1.add(entityId);
                }
            }
            final TIntList retainedEntitiesD2 = new TIntArrayList();
            for (int entityId : oldBlock.getIndex2Entities()) {
                if (counterD2[entityId] < limitsD2[entityId]) {
                    retainedEntitiesD2.add(entityId);
                }
            }
            if (!retainedEntitiesD1.isEmpty() && !retainedEntitiesD2.isEmpty()) {
                for (TIntIterator iterator1 = retainedEntitiesD1.iterator(); iterator1.hasNext();) {
                    counterD1[iterator1.next()]++;
                }

                for (TIntIterator iterator2 = retainedEntitiesD2.iterator(); iterator2.hasNext();) {
                    counterD2[iterator2.next()]++;
                }
                newBlocks.add(new BilateralBlock(oldBlock.getEntropy(), retainedEntitiesD1.toArray(), retainedEntitiesD2.toArray()));
            }
        });

        return newBlocks;
    }

    protected List<AbstractBlock> restructureBlocks(List<AbstractBlock> blocks) {
        if (blocks.get(0) instanceof BilateralBlock) {
            return restructureBilateraBlocks(blocks);
        }

        return restructureUnilateraBlocks(blocks);
    }

    protected List<AbstractBlock> restructureUnilateraBlocks(List<AbstractBlock> blocks) {
        final List<AbstractBlock> newBlocks = new ArrayList<>();
        blocks.stream().map((block) -> (UnilateralBlock) block).forEachOrdered((oldBlock) -> {
            final TIntList retainedEntities = new TIntArrayList();
            for (int entityId : oldBlock.getEntities()) {
                if (counterD1[entityId] < limitsD1[entityId]) {
                    retainedEntities.add(entityId);
                }
            }
            if (1 < retainedEntities.size()) {
                for (TIntIterator iterator = retainedEntities.iterator(); iterator.hasNext();) {
                    counterD1[iterator.next()]++;
                }
                newBlocks.add(new UnilateralBlock(oldBlock.getEntropy(), retainedEntities.toArray()));
            }
        });

        return newBlocks;
    }

    @Override
    public void setNextRandomConfiguration() {
        ratio = (Float) randomRatio.getNextRandomValue();
    }

    @Override
    public void setNumberedGridConfiguration(int iterationNumber) {
        ratio = (Float) gridRatio.getNumberedValue(iterationNumber);
    }

    @Override
    public void setNumberedRandomConfiguration(int iterationNumber) {
        ratio = (Float) randomRatio.getNumberedRandom(iterationNumber);
    }

    protected void sortBlocks(List<AbstractBlock> blocks) {
        blocks.sort(new IncBlockCardinalityComparator());
    }
}
