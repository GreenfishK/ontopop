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
import org.scify.jedai.blockprocessing.blockcleaning.SizeBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.CenterClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;

import java.io.File;
import java.util.List;

/**
 *
 * @author GAP2
 */
public class StepByStepRandomConfigurationDER {

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

        String mainDir = "data" + File.separator + "dirtyErDatasets" + File.separator;
        final String[] datasets = {"census", "cddb", "cora"};

        for (String dataset : datasets) {
            System.out.println("\n\nCurrent dataset\t:\t" + dataset);

            final EntitySerializationReader inReader = new EntitySerializationReader(mainDir + dataset + "Profiles");
            final List<EntityProfile> profiles = inReader.getEntityProfiles();
            System.out.println("\nNumber of entity profiles\t:\t" + profiles.size());

            final IGroundTruthReader gtReader = new GtSerializationReader(mainDir + dataset + "IdDuplicates");
            final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(profiles));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            final IBlockBuilding bb = new StandardBlocking();
            final IBlockProcessing bp1 = new SizeBasedBlockPurging();
            final IBlockProcessing bp2 = new BlockFiltering();
            final IBlockProcessing cc = new CardinalityNodePruning();
            final IEntityMatching em = new ProfileMatcher(profiles);
            final IEntityClustering ec = new CenterClustering();
//            final IEntityClustering ec = new MergeCenterClustering();
//            final IEntityClustering ec = new ConnectedComponentsClustering();

            final StringBuilder matchingWorkflowName = new StringBuilder();
            matchingWorkflowName.append(bb.getMethodName());
            matchingWorkflowName.append("->").append(bp1.getMethodName());
            matchingWorkflowName.append("->").append(bp2.getMethodName());
            matchingWorkflowName.append("->").append(cc.getMethodName());
            matchingWorkflowName.append("->").append(em.getMethodName());
            matchingWorkflowName.append("->").append(ec.getMethodName());

            // local optimization of Block Building
            float bestA = 0;
            int bestIteration = 0;
            long originalComparisons = profiles.size() * (profiles.size() - 1) / 2;
            for (int j = 0; j < NO_OF_TRIALS; j++) {
                bb.setNextRandomConfiguration();
                final List<AbstractBlock> originalBlocks = bb.getBlocks(profiles);
                if (originalBlocks.isEmpty()) {
                    continue;
                }

                final BlocksPerformance blp = new BlocksPerformance(originalBlocks, duplicatePropagation);
                blp.setStatistics();
                float recall = blp.getPc();
                float rr = 1 - blp.getAggregateCardinality() / originalComparisons;
                float a = rr * recall;
                if (bestA < a) {
                    bestIteration = j;
                    bestA = a;
                }
            }
            System.out.println("\n\nBest iteration\t:\t" + bestIteration);
            System.out.println("Best performance\t:\t" + bestA);

            bb.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> blocks = bb.getBlocks(profiles);
            BlocksPerformance blp = new BlocksPerformance(blocks, duplicatePropagation);
            blp.setStatistics();
            blp.printStatistics(0, bp1.getMethodConfiguration(), bp1.getMethodName());

            // local optimization of Block Purging
            bestA = 0;
            bestIteration = 0;
            originalComparisons = getTotalComparisons(blocks);
            for (int j = 0; j < NO_OF_TRIALS; j++) {
                bp1.setNextRandomConfiguration();
                final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);
                if (purgedBlocks.isEmpty()) {
                    continue;
                }

                blp = new BlocksPerformance(purgedBlocks, duplicatePropagation);
                blp.setStatistics();
                float recall = blp.getPc();
                float rr = 1 - blp.getAggregateCardinality() / originalComparisons;
                float a = rr * recall;
                if (bestA < a) {
                    bestIteration = j;
                    bestA = a;
                }
            }
            System.out.println("\n\nBest iteration\t:\t" + bestIteration);
            System.out.println("Best performance\t:\t" + bestA);

            bp1.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> purgedBlocks = bp1.refineBlocks(blocks);
            blp = new BlocksPerformance(purgedBlocks, duplicatePropagation);
            blp.setStatistics();
            blp.printStatistics(0, bp1.getMethodConfiguration(), bp1.getMethodName());

            // local optimization of Block Filtering
            bestA = 0;
            bestIteration = 0;
            originalComparisons = getTotalComparisons(purgedBlocks);
            for (int j = 0; j < NO_OF_TRIALS; j++) {
                bp2.setNextRandomConfiguration();
                final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
                if (filteredBlocks.isEmpty()) {
                    continue;
                }

                blp = new BlocksPerformance(filteredBlocks, duplicatePropagation);
                blp.setStatistics();
                float recall = blp.getPc();
                float rr = 1 - blp.getAggregateCardinality() / originalComparisons;
                float a = rr * recall;
                if (bestA < a) {
                    bestIteration = j;
                    bestA = a;
                }
            }
            System.out.println("\n\nBest iteration\t:\t" + bestIteration);
            System.out.println("Best performance\t:\t" + bestA);

            bp2.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> filteredBlocks = bp2.refineBlocks(purgedBlocks);
            blp = new BlocksPerformance(filteredBlocks, duplicatePropagation);
            blp.setStatistics();
            blp.printStatistics(0, bp2.getMethodConfiguration(), bp2.getMethodName());

            // local optimization of CNP
            bestA = 0;
            bestIteration = 0;
            originalComparisons = getTotalComparisons(filteredBlocks);
            for (int j = 0; j < NO_OF_TRIALS; j++) {
                cc.setNextRandomConfiguration();
                final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
                if (finalBlocks.isEmpty()) {
                    continue;
                }

                blp = new BlocksPerformance(finalBlocks, duplicatePropagation);
                blp.setStatistics();
                float recall = blp.getPc();
                float rr = 1 - blp.getAggregateCardinality() / originalComparisons;
                float a = rr * recall;
                if (bestA < a) {
                    bestIteration = j;
                    bestA = a;
                }
            }
            System.out.println("\n\nBest iteration\t:\t" + bestIteration);
            System.out.println("Best performance\t:\t" + bestA);

            cc.setNumberedRandomConfiguration(bestIteration);
            final List<AbstractBlock> finalBlocks = cc.refineBlocks(filteredBlocks);
            blp = new BlocksPerformance(finalBlocks, duplicatePropagation);
            blp.setStatistics();
            blp.printStatistics(0, cc.getMethodConfiguration(), cc.getMethodName());

            // local optimization of Matching & Clustering            
            bestIteration = 0;
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
            matchingWorkflowConf.append("\n").append(bp1.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(bp2.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(cc.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(em.getMethodConfiguration());
            matchingWorkflowConf.append("\n").append(ec.getMethodConfiguration());

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(time2 - time1, matchingWorkflowName.toString(), matchingWorkflowConf.toString());
        }
    }
}
