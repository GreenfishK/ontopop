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
import org.scify.jedai.datamodel.*;

import java.util.*;

/**
 *
 * @author gap2
 */
public abstract class AbstractBlockBuilding implements IBlockBuilding {
    private static final long serialVersionUID = -1009041578742039220L;

    protected boolean isUsingEntropy;

    protected int noOfEntitiesD1;
    protected int noOfEntitiesD2;

    protected List<AbstractBlock> blocks;
    protected List<EntityProfile> entityProfilesD1;
    protected List<EntityProfile> entityProfilesD2;
    protected Map<String, TIntList> invertedIndexD1;
    protected Map<String, TIntList> invertedIndexD2;
    protected AttributeClusters[] schemaClusters;

    public AbstractBlockBuilding() {
        isUsingEntropy = false;
    }

    protected void buildBlocks() {
        if (schemaClusters == null) {
            indexEntities(invertedIndexD1, entityProfilesD1);
        } else {
            indexEntities(invertedIndexD1, entityProfilesD1, schemaClusters[0]);
        }

        if (invertedIndexD2 != null) {
            if (schemaClusters == null) {
                indexEntities(invertedIndexD2, entityProfilesD2);
            } else {
                indexEntities(invertedIndexD2, entityProfilesD2, schemaClusters[1]);
            }
        }
    }

    protected abstract Set<String> getBlockingKeys(String attributeValue);

    @Override
    public List<AbstractBlock> getBlocks(List<EntityProfile> profiles) {
        return this.getBlocks(profiles, null);
    }

    @Override
    public List<AbstractBlock> getBlocks(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2) {
        return this.getBlocks(profilesD1, profilesD2, null);
    }

    @Override
    public List<AbstractBlock> getBlocks(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, AttributeClusters[] sClusters) {
        Log.info("Applying " + getMethodName() + " with the following configuration : " + getMethodConfiguration());

        if (profilesD1 == null) {
            Log.error("First list of entity profiles is null! The first argument should always contain entities.");
            return null;
        }

        blocks = new ArrayList<>();
        schemaClusters = sClusters;
        invertedIndexD1 = new HashMap<>();
        entityProfilesD1 = profilesD1;
        noOfEntitiesD1 = entityProfilesD1.size();
        if (profilesD2 != null) {
            invertedIndexD2 = new HashMap<>();
            entityProfilesD2 = profilesD2;
            noOfEntitiesD2 = entityProfilesD2.size();
        }

        buildBlocks();
        return readBlocks();
    }

    public long getBruteForceComparisons() {
        if (entityProfilesD2 == null) {
            return noOfEntitiesD1 * (noOfEntitiesD1 - 1) / 2;
        }
        return noOfEntitiesD1 * noOfEntitiesD2;
    }

    public int getTotalNoOfEntities() {
        if (entityProfilesD2 == null) {
            return noOfEntitiesD1;
        }
        return Math.addExact(noOfEntitiesD1, noOfEntitiesD2);
    }

    protected void indexEntities(Map<String, TIntList> index, List<EntityProfile> entities) {
        int counter = 0;
        for (EntityProfile profile : entities) {
            final Set<String> allKeys = new HashSet<>();
            profile.getAttributes().forEach((attribute) -> {
                getBlockingKeys(attribute.getValue().toLowerCase()).stream().map((key) -> key.trim()).filter((normalizedKey) -> (0 < normalizedKey.length())).forEachOrdered((normalizedKey) -> {
                    allKeys.add(normalizedKey);
                });
            });

            for (String key : allKeys) {
                TIntList entityList = index.get(key);
                if (entityList == null) {
                    entityList = new TIntArrayList();
                    index.put(key, entityList);
                }
                entityList.add(counter);
            }
            counter++;
        }
    }

    protected void indexEntities(Map<String, TIntList> index, List<EntityProfile> entities, AttributeClusters schemaClusters) {
        isUsingEntropy = true;

        int counter = 0;
        for (EntityProfile profile : entities) {
            final Set<String> allKeys = new HashSet<>();
            profile.getAttributes().forEach((attribute) -> {
                int clusterId = schemaClusters.getClusterId(attribute.getName());
                getBlockingKeys(attribute.getValue().toLowerCase()).stream().map((key) -> key.trim()).filter((normalizedKey) -> (0 < normalizedKey.length())).forEachOrdered((normalizedKey) -> {
                    allKeys.add(normalizedKey + CLUSTER_PREFIX + clusterId + CLUSTER_PREFIX + schemaClusters.getClusterEntropy(clusterId));
                });
            });

            for (String key : allKeys) {
                TIntList entityList = index.get(key);
                if (entityList == null) {
                    entityList = new TIntArrayList();
                    index.put(key, entityList);
                }
                entityList.add(counter);
            }
            counter++;
        }
    }

    protected void parseIndex() {
        if (!isUsingEntropy) {
            invertedIndexD1.values().stream().filter((entityList) -> (1 < entityList.size())).forEachOrdered((entityList) -> blocks.add(new UnilateralBlock(entityList.toArray())));
        } else {
            invertedIndexD1.forEach((key, value) -> {
                final String[] entropyString = key.split(CLUSTER_SUFFIX);
                float entropyValue = Float.parseFloat(entropyString[1]);
                blocks.add(new UnilateralBlock(entropyValue, value.toArray()));
            });
        }
    }

    protected void parseIndices() {
        if (!isUsingEntropy) {
            invertedIndexD1.forEach((key, value) -> {
                final TIntList entityIdsD2 = invertedIndexD2.get(key);
                if (entityIdsD2 != null && !entityIdsD2.isEmpty()) {
                    blocks.add(new BilateralBlock(value.toArray(), entityIdsD2.toArray()));
                }
            });
        } else {
            invertedIndexD1.forEach((key, value) -> {
                final TIntList entityIdsD2 = invertedIndexD2.get(key);
                if (entityIdsD2 != null && !entityIdsD2.isEmpty()) {
                    final String[] entropyString = key.split(CLUSTER_SUFFIX);
                    float entropyValue = Float.parseFloat(entropyString[1]);
                    blocks.add(new BilateralBlock(entropyValue, value.toArray(), entityIdsD2.toArray()));
                }
            });
        }
    }

    //read blocks from the inverted index
    public List<AbstractBlock> readBlocks() {
        if (entityProfilesD2 == null) { //Dirty ER
            parseIndex();
        } else { // Clean-Clean ER
            parseIndices();
        }

        return blocks;
    }
}
