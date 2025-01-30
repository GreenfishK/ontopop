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
package org.scify.jedai.version3.prioritization;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.ComparisonsBasedBlockPurging;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.prioritization.IPrioritization;
import org.scify.jedai.prioritization.ProgressiveGlobalTopComparisons;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.BlockBuildingMethod;
import org.scify.jedai.utilities.enumerations.ComparisonCleaningMethod;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class TestProgressiveRecall {

    private final static int NO_OF_ITERATIONS = 10;

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDir = "C:\\Users\\Georgios\\Documents\\Data\\ER\\ccer\\top10\\";
        String[] datasetsD1 = {"restaurant1Profiles", "abtProfiles", "amazonProfiles", "dblpProfiles", "imdbProfilesNEW", "imdbProfilesNEW", "tmdbProfiles", "walmartProfiles", "dblpProfiles2", "imdbProfiles"};
        String[] datasetsD2 = {"restaurant2Profiles", "buyProfiles", "gpProfiles", "acmProfiles", "tmdbProfiles", "tvdbProfiles", "tvdbProfiles", "amazonProfiles2", "scholarProfiles", "dbpediaProfiles"};
        String[] groundtruthDirs = {"restaurantsIdDuplicates", "abtBuyIdDuplicates", "amazonGpIdDuplicates", "dblpAcmIdDuplicates", "imdbTmdbIdDuplicates", "imdbTvdbIdDuplicates", "tmdbTvdbIdDuplicates", "amazonWalmartIdDuplicates",
            "dblpScholarIdDuplicates", "moviesIdDuplicates"};

        for (int i = 0; i < groundtruthDirs.length; i++) {
            final IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1[i]);
            final List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("\n\n\n\n\nInput Entity Profiles\t:\t" + profiles1.size());

            final IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2[i]);
            final List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            final IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs[i]);
            final Set<IdDuplicates> duplicatePairs = gtReader.getDuplicatePairs(null);
            AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(duplicatePairs);
            System.out.println("Existing Duplicates\t:\t" + duplicatePairs.size());

            long time1 = System.currentTimeMillis();

            final List<BlockBuildingMethod> blockingMethods = new ArrayList<>();
            blockingMethods.add(BlockBuildingMethod.STANDARD_BLOCKING);
            blockingMethods.add(BlockBuildingMethod.Q_GRAMS_BLOCKING);
            blockingMethods.add(BlockBuildingMethod.EXTENDED_Q_GRAMS_BLOCKING);
            blockingMethods.add(BlockBuildingMethod.SUFFIX_ARRAYS);
            blockingMethods.add(BlockBuildingMethod.EXTENDED_SUFFIX_ARRAYS);

            final List<ComparisonCleaningMethod> metablockingMethods = new ArrayList<>();
            metablockingMethods.add(ComparisonCleaningMethod.BLAST);
            metablockingMethods.add(ComparisonCleaningMethod.CARDINALITY_EDGE_PRUNING);
            metablockingMethods.add(ComparisonCleaningMethod.CARDINALITY_NODE_PRUNING);
            metablockingMethods.add(ComparisonCleaningMethod.RECIPROCAL_CARDINALITY_NODE_PRUNING);
            metablockingMethods.add(ComparisonCleaningMethod.RECIPROCAL_WEIGHTING_NODE_PRUNING);
            metablockingMethods.add(ComparisonCleaningMethod.WEIGHTED_EDGE_PRUNING);
            metablockingMethods.add(ComparisonCleaningMethod.WEIGHTED_NODE_PRUNING);

            for (BlockBuildingMethod bbMethod : blockingMethods) {
                System.out.println("\n\n\n\n\nCurrent blocking method\t:\t" + bbMethod);

                final IBlockBuilding blockBuildingMethod = BlockBuildingMethod.getDefaultConfiguration(bbMethod);
                List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);
                System.out.println("Original blocks\t:\t" + blocks.size());

                final IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(1.00f);
                blocks = blockCleaningMethod1.refineBlocks(blocks);

                final IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
                blocks = blockCleaningMethod2.refineBlocks(blocks);

                for (ComparisonCleaningMethod mbMethod : metablockingMethods) {
                    System.out.println("\n\n\n\n\nCurrent meta-blocking method\t:\t" + bbMethod);

                    for (WeightingScheme wScheme : WeightingScheme.values()) {
                        System.out.println("\n\n\n\n\nCurrent weighting scheme\t:\t" + wScheme);

                        final IBlockProcessing comparisonCleaningMethod = ComparisonCleaningMethod.getMetablockingMethod(mbMethod, wScheme);
                        List<AbstractBlock> mbBlocks = comparisonCleaningMethod.refineBlocks(new ArrayList<>(blocks));

                        long time2 = System.currentTimeMillis();

                        BlocksPerformance blStats = new BlocksPerformance(mbBlocks, duplicatePropagation);
                        blStats.setStatistics();
                        blStats.printStatistics(time2 - time1, "", "");

                        double totalComparisons = blStats.getAggregateCardinality();
                        System.out.println("Total comparisons\t:\t" + totalComparisons);

                        for (WeightingScheme prScheme : WeightingScheme.values()) {
                            System.out.println("\n\n\n\n\nCurrent prioritization weighting scheme\t:\t" + wScheme);

                            double averageAUC = 0;
                            double averageEmitTime = 0;
                            double averageInitTime = 0;
                            for (int iteration = 0; iteration < NO_OF_ITERATIONS; iteration++) {
                                long time3 = System.currentTimeMillis();

//            final IPrioritization prioritization = new ProgressiveBlockScheduling((int) totalComparisons, WeightingScheme.ARCS);
//            final IPrioritization prioritization = new ProgressiveEntityScheduling((int) totalComparisons, WeightingScheme.ARCS);
//            final IPrioritization prioritization = new ProgressiveLocalTopComparisons((int) totalComparisons, WeightingScheme.ARCS);
                                final IPrioritization prioritization = new ProgressiveGlobalTopComparisons((int) totalComparisons, prScheme);
//            final IPrioritization prioritization = new ProgressiveGlobalRandomComparisons((int) totalComparisons);
                                prioritization.developBlockBasedSchedule(mbBlocks);

                                long time4 = System.currentTimeMillis();

                                int counter = 0;
                                int detectedMatches = 0;
                                double progressiveRecall = 0;
                                while (prioritization.hasNext()) {
                                    counter++;
                                    Comparison c = prioritization.next();
                                    if (duplicatePairs.contains(new IdDuplicates(c.getEntityId1(), c.getEntityId2()))) {
                                        detectedMatches++;
                                    }
                                    progressiveRecall += detectedMatches;
                                }

                                long time5 = System.currentTimeMillis();
                                averageAUC += progressiveRecall / duplicatePairs.size() / (counter + 1.0);
                                averageEmitTime += time5 - time4;
                                averageInitTime += time4 - time3;
                            }
                            System.out.println("AUC (Progressive Recall)\t:\t" + averageAUC / NO_OF_ITERATIONS);
                            System.out.println("Average Initialization Time\t:\t" + averageInitTime / NO_OF_ITERATIONS);
                            System.out.println("Average Emission Time\t:\t" + averageEmitTime / NO_OF_ITERATIONS);
                        }
                        
//                        //Baseline method
//                        List<Comparison> allComparisons = new ArrayList<>();
//                        for (AbstractBlock block : mbBlocks) {
//                            final ComparisonIterator cIterator = block.getComparisonIterator();
//                            while (cIterator.hasNext()) {
//                                allComparisons.add(cIterator.next());
//                            }
//                        }
//                        System.out.println("Total comparisons\t:\t" + allComparisons.size());
//
//                        double averageAUC = 0;
//                        for (int iteration = 0; iteration < NO_OF_ITERATIONS; iteration++) {
//                            Collections.shuffle(allComparisons);
//
//                            int counter = 0;
//                            double detectedMatches = 0;
//                            double progressiveRecall = 0;
//                            for (Comparison c : allComparisons) {
//                                counter++;
//                                if (duplicatePairs.contains(new IdDuplicates(c.getEntityId1(), c.getEntityId2()))) {
//                                    detectedMatches++;
//                                }
//                                progressiveRecall += detectedMatches;
//                            }
//                            averageAUC += progressiveRecall / duplicatePairs.size() / (counter + 1.0);
//                        }
//                        System.out.println("Baseline AUC (Progressive Recall)\t:\t" + averageAUC / NO_OF_ITERATIONS);
                    }
                }
            }
        }
    }
}
