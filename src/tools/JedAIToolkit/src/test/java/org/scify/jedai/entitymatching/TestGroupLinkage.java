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

import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author G.A.P. II
 */
public class TestGroupLinkage {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String entitiesFilePath1 = "data\\dirtyErDatasets\\dblpAcmProfiles";
        String groundTruthFilePath = "data\\dirtyErDatasets\\dblpAcmIdDuplicates";

        IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath1);
        List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader1.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        for (BlockBuildingMethod blbuMethod : BlockBuildingMethod.values()) {
            System.out.println("\n\nCurrent blocking metohd\t:\t" + blbuMethod);
            IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(blbuMethod);
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1);//
            System.out.println("Original blocks\t:\t" + blocks.size());

            IBlockProcessing blockCleaningMethod = BlockBuildingMethod.getDefaultBlockCleaning(blbuMethod);
            if (blockCleaningMethod != null) {
                blocks = blockCleaningMethod.refineBlocks(blocks);
            }

            IBlockProcessing comparisonCleaningMethod = BlockBuildingMethod.getDefaultComparisonCleaning(blbuMethod);
            if (comparisonCleaningMethod != null) {
                blocks = comparisonCleaningMethod.refineBlocks(blocks);
            }

            long start = System.nanoTime();
            for (RepresentationModel model : RepresentationModel.values()) {
                if (model.equals(RepresentationModel.PRETRAINED_WORD_VECTORS)) {
                    GroupLinkage gp = new GroupLinkage(0.1f, profiles1, model, SimilarityMetric.getModelDefaultSimMetric(model));
                    gp.setSimilarityThreshold(0.1f);
                    SimilarityPairs simPairs = gp.executeComparisons(blocks);

                    for (int i = 0; i < simPairs.getNoOfComparisons(); i++) {
                        System.out.println(simPairs.getEntityIds1()[i] + "\t\t" + simPairs.getEntityIds2()[i] + "\t\t" + simPairs.getSimilarities()[i]);
                    }
                }

                long elapsedTime = System.nanoTime() - start;
                System.out.println("time=" + elapsedTime / 1000000000.0);
            }

        }
    }
}
