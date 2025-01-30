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
package org.scify.jedai.configuration;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

import java.io.File;
import java.util.List;

/**
 *
 * @author GAP2
 */
public class OptimizeDirtyMoviesDataset {

    private final static int NO_OF_TRIALS = 100;

    static long getTotalComparisons(List<AbstractBlock> blocks) {
        long originalComparisons = 0;
        for (AbstractBlock block : blocks) {
            originalComparisons += block.getNoOfComparisons();
        }
        System.out.println("Original comparisons\t:\t" + originalComparisons);
        return originalComparisons;
    }

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();

        String entitiesFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "moviesProfiles";
        String groundTruthFilePath = "data" + File.separator + "dirtyErDatasets" + File.separator + "moviesIdDuplicates";

        final IEntityReader eReader1 = new EntitySerializationReader(entitiesFilePath);
        final List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

        final IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFilePath);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        final IBlockBuilding bb = new StandardBlocking();
        final IBlockProcessing bp = new BlockFiltering();
        final IBlockProcessing cc = new CardinalityNodePruning(WeightingScheme.ECBS);
        final IEntityMatching em = new ProfileMatcher(profiles1);
        final IEntityClustering ec = new UniqueMappingClustering();

        final StringBuilder matchingWorkflowName = new StringBuilder();
        matchingWorkflowName.append(bb.getMethodName());
        matchingWorkflowName.append("->").append(bp.getMethodName());
        matchingWorkflowName.append("->").append(cc.getMethodName());
        matchingWorkflowName.append("->").append(em.getMethodName());
        matchingWorkflowName.append("->").append(ec.getMethodName());

        final List<AbstractBlock> originalBlocks = bb.getBlocks(profiles1);
        BlocksPerformance blp = new BlocksPerformance(originalBlocks, duplicatePropagation);
        blp.setStatistics();
        blp.printStatistics(0, bb.getMethodConfiguration(), bb.getMethodName());

        final List<AbstractBlock> filteredBlocks = bp.refineBlocks(originalBlocks);
        blp = new BlocksPerformance(filteredBlocks, duplicatePropagation);
        blp.setStatistics();
        blp.printStatistics(0, bp.getMethodConfiguration(), bp.getMethodName());

        final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
        blp = new BlocksPerformance(finalBlocks, duplicatePropagation);
        blp.setStatistics();
        blp.printStatistics(0, cc.getMethodConfiguration(), cc.getMethodName());

//        // local optimization of Matching & Clustering            
        int bestIteration = 0;
        float bestFMeasure = 0;
        for (int j = 0; j < NO_OF_TRIALS; j++) {
            em.setNextRandomConfiguration();
            final SimilarityPairs sims = em.executeComparisons(finalBlocks);

            ec.setNextRandomConfiguration();
            final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            float fMeasure = clp.getFMeasure();
            if (bestFMeasure < fMeasure) {
                bestIteration = j;
                bestFMeasure = fMeasure;
            }
        }
        System.out.println("\nBest Iteration\t:\t" + bestIteration);
        System.out.println("Best FMeasure\t:\t" + bestFMeasure);

        long time1 = System.currentTimeMillis();

        em.setNumberedRandomConfiguration(bestIteration);
        final SimilarityPairs sims = em.executeComparisons(finalBlocks);

        ec.setNumberedRandomConfiguration(bestIteration);
        final EquivalenceCluster[] clusters = ec.getDuplicates(sims);

        long time2 = System.currentTimeMillis();

        final StringBuilder matchingWorkflowConf = new StringBuilder();
        matchingWorkflowConf.append(bb.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(bp.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(cc.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(em.getMethodConfiguration());
        matchingWorkflowConf.append("\n").append(ec.getMethodConfiguration());

        final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
        clp.setStatistics();
        clp.printStatistics(time2 - time1, matchingWorkflowName.toString(), matchingWorkflowConf.toString());
    }
}
