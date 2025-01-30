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
package org.scify.jedai.entityclustering;

import java.io.File;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.enumerations.EntityClusteringDerMethod;
import org.scify.jedai.utilities.enumerations.EntityMatchingMethod;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

public class TestCorrelationClustering {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String entitiesFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "coraProfiles";
        String groundTruthFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "coraIdDuplicates";

        IEntityReader eReader = new EntitySerializationReader(entitiesFilePath);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles.size());

        IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        long time1 = System.currentTimeMillis();

        IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(BlockBuildingMethod.STANDARD_BLOCKING);
        List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles, null);
        System.out.println("Original blocks\t:\t" + blocks.size());

        StringBuilder blockingWorkflowConf = new StringBuilder();
        StringBuilder blockingWorkflowName = new StringBuilder();
        blockingWorkflowConf.append(blockBuildingMethod.getMethodConfiguration());
        blockingWorkflowName.append(blockBuildingMethod.getMethodName());

        IBlockProcessing blockCleaningMethod = BlockBuildingMethod.getDefaultBlockCleaning(BlockBuildingMethod.STANDARD_BLOCKING);
        if (blockCleaningMethod != null) {
            blocks = blockCleaningMethod.refineBlocks(blocks);
            blockingWorkflowConf.append("\n").append(blockCleaningMethod.getMethodConfiguration());
            blockingWorkflowName.append("->").append(blockCleaningMethod.getMethodName());
        }

        IBlockProcessing comparisonCleaningMethod = BlockBuildingMethod.getDefaultComparisonCleaning(BlockBuildingMethod.STANDARD_BLOCKING);
        if (comparisonCleaningMethod != null) {
            blocks = comparisonCleaningMethod.refineBlocks(blocks);
            blockingWorkflowConf.append("\n").append(comparisonCleaningMethod.getMethodConfiguration());
            blockingWorkflowName.append("->").append(comparisonCleaningMethod.getMethodName());
        }

        long time2 = System.currentTimeMillis();

        BlocksPerformance blp = new BlocksPerformance(blocks, duplicatePropagation);
//        blp.printFalseNegatives(profiles, null, "data" + File.separator + "falseNegatives.csv");
//        blp.printDetailedResults(profiles, null);
        blp.setStatistics();
        blp.printStatistics(time2 - time1, blockingWorkflowConf.toString(), blockingWorkflowName.toString());

        EntityMatchingMethod emMethod = EntityMatchingMethod.PROFILE_MATCHER;

        long time3 = System.currentTimeMillis();

        IEntityMatching em = EntityMatchingMethod.getDefaultConfiguration(profiles, null, emMethod);
        SimilarityPairs simPairs = em.executeComparisons(blocks);

        long time4 = System.currentTimeMillis();

        EntityClusteringDerMethod ecMethod =EntityClusteringDerMethod.CORRELATION_CLUSTERING;
        long time5 = System.currentTimeMillis();

        IEntityClustering ec = EntityClusteringDerMethod.getDefaultConfiguration(ecMethod);
        //ec.setSimilarityThreshold(0.1);
        EquivalenceCluster[] entityClusters = ec.getDuplicates(simPairs);

        long time6 = System.currentTimeMillis();

        StringBuilder matchingWorkflowConf = new StringBuilder();
        StringBuilder matchingWorkflowName = new StringBuilder();
        matchingWorkflowConf.append(em.getMethodConfiguration());
        matchingWorkflowName.append(em.getMethodName());
        matchingWorkflowConf.append("\n").append(ec.getMethodConfiguration());
        matchingWorkflowName.append("->").append(ec.getMethodName());

        ClustersPerformance clp = new ClustersPerformance(entityClusters, duplicatePropagation);
//        clp.printDetailedResults(profiles, null, "D:\\temp.csv");
        clp.setStatistics();
        clp.printStatistics(time6 - time5 + time4 - time3, matchingWorkflowName.toString(), matchingWorkflowConf.toString());
    
}
}
