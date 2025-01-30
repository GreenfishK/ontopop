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
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockbuilding.IBlockBuilding;
import org.scify.jedai.blockbuilding.StandardBlocking;
import org.scify.jedai.blockprocessing.IBlockProcessing;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.ComparisonsBasedBlockPurging;
import org.scify.jedai.blockprocessing.comparisoncleaning.CardinalityNodePruning;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
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
import org.scify.jedai.prioritization.IPrioritization;
import org.scify.jedai.prioritization.ProgressiveGlobalTopComparisons;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class TestProgressiveCleanCleanER {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        float[] clusteringThreshold = {0.90f, 0.30f, 0.05f, 0.55f, 0.60f, 0.45f, 0.10f};
        RepresentationModel[] repModel = {RepresentationModel.CHARACTER_BIGRAMS, RepresentationModel.CHARACTER_BIGRAMS_TF_IDF, RepresentationModel.TOKEN_BIGRAMS_TF_IDF, RepresentationModel.TOKEN_UNIGRAMS_TF_IDF, RepresentationModel.TOKEN_UNIGRAMS_TF_IDF, RepresentationModel.CHARACTER_TRIGRAMS_TF_IDF, RepresentationModel.TOKEN_UNIGRAMS_TF_IDF};
        SimilarityMetric[] simMetric = {SimilarityMetric.COSINE_SIMILARITY, SimilarityMetric.COSINE_SIMILARITY, SimilarityMetric.COSINE_SIMILARITY, SimilarityMetric.SIGMA_SIMILARITY, SimilarityMetric.COSINE_SIMILARITY, SimilarityMetric.SIGMA_SIMILARITY, SimilarityMetric.COSINE_SIMILARITY};
        
        String mainDir = "/home/gap2/data/JedAIdata/datasets/cleanCleanErDatasets/";
        String[] datasetsD1 = {"restaurant1Profiles", "abtProfiles", "amazonProfiles", "dblpProfiles", "walmartProfiles", "dblpProfiles2", "imdbProfiles"};
        String[] datasetsD2 = {"restaurant2Profiles", "buyProfiles", "gpProfiles", "acmProfiles", "amazonProfiles2", "scholarProfiles", "dbpediaProfiles"};
        String[] groundtruthDirs = {"restaurantsIdDuplicates", "abtBuyIdDuplicates", "amazonGpIdDuplicates", "dblpAcmIdDuplicates", "amazonWalmartIdDuplicates",
            "dblpScholarIdDuplicates", "moviesIdDuplicates"};

        for (int i = 3; i < 4/*groundtruthDirs.length*/; i++) {
            IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1[i]);
            List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles1.size());

            IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2[i]);
            List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles2.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs[i]);
            AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

//            Set<IdDuplicates> pairs = duplicatePropagation.getDuplicates();
//            for (IdDuplicates pair : pairs) {
//                System.out.println("Duplicates\t:\tId1-" + pair.getEntityId1() + "\tId2-" + pair.getEntityId2());
//            }
            IBlockBuilding blockBuildingMethod = new StandardBlocking();
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles1, profiles2);
            System.out.println("Original blocks\t:\t" + blocks.size());

            IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(1.00f);
            blocks = blockCleaningMethod1.refineBlocks(blocks);

            IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
            blocks = blockCleaningMethod2.refineBlocks(blocks);

            IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.JS);
            List<AbstractBlock> cnpBlocks = comparisonCleaningMethod.refineBlocks(new ArrayList<>(blocks));
//            final Set<Comparison> distinctComparisons = new HashSet<>();
            float totalComparisons = 0;
            for (AbstractBlock block : cnpBlocks) {
                totalComparisons += block.getNoOfComparisons();
//                ComparisonIterator ci = block.getComparisonIterator();
//                while (ci.hasNext()) {
//                    distinctComparisons.add(ci.next());
//                }
            }
            System.out.println("Total comparisons\t:\t" + totalComparisons);
//            System.out.println("Distinct comparisons\t:\t" + distinctComparisons.size() + "\t" + totalComparisons);

            final IEntityMatching emOr = new ProfileMatcher(profiles1, profiles2, repModel[i], simMetric[i]);
            SimilarityPairs originalSims = emOr.executeComparisons(cnpBlocks);

            IEntityClustering ec = new UniqueMappingClustering(clusteringThreshold[i]);
            EquivalenceCluster[] clusters = ec.getDuplicates(originalSims);

            ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(0, "", "");
            float originalRecall = clp.getRecall();

//            final IPrioritization prioritization = new ProgressiveBlockScheduling((int) totalComparisons, WeightingScheme.ARCS);
//            final IPrioritization prioritization = new ProgressiveEntityScheduling((int) totalComparisons, WeightingScheme.ARCS);
//            final IPrioritization prioritization = new ProgressiveLocalTopComparisons((int) totalComparisons, WeightingScheme.ARCS);
            final IPrioritization prioritization = new ProgressiveGlobalTopComparisons((int) totalComparisons, WeightingScheme.JS);
//            final IPrioritization prioritization = new ProgressiveGlobalRandomComparisons((int) totalComparisons);
            prioritization.developBlockBasedSchedule(cnpBlocks);

//            final IPrioritization prioritization = new LocalProgressiveSortedNeighborhood(profiles1.size() * profiles2.size(), ProgressiveWeightingScheme.ACF);
//            final IPrioritization prioritization = new GlobalProgressiveSortedNeighborhood(profiles1.size() * profiles2.size(), ProgressiveWeightingScheme.ACF);
//            prioritization.developEntityBasedSchedule(profiles1, profiles2);
//            int counter = 0;
//            Set<Comparison> comparisons = new HashSet<>();
            final IEntityMatching em = new ProfileMatcher(profiles1, profiles2, repModel[i], simMetric[i]);
//            final List<Comparison> emittedComparisons = new ArrayList<>();    
            SimilarityPairs sims = new SimilarityPairs(true, (int) totalComparisons);
            while (prioritization.hasNext()) {
                Comparison c1 = prioritization.next();
//                System.out.println(c1);
//                comparisons.add(c1);
//            }
//            List<Comparison> randomComparisons = new ArrayList<>(comparisons);
//            Collections.shuffle(randomComparisons);
//            
//            for (Comparison c1 : randomComparisons) {
                float similarity = em.executeComparison(c1);
//                if (0 < similarity) {
//                    counter++;
                c1.setUtilityMeasure(similarity);
                sims.addComparison(c1);
//            }
//            }
//                ec = new UniqueMappingClustering(clusteringThreshold[i]);
                clusters = ec.getDuplicates(sims);

                clp = new ClustersPerformance(clusters, duplicatePropagation);
                clp.setStatistics();
                float currentRecall = clp.getRecall();
                System.out.println("Current recall\t:\t" + currentRecall);
                if (originalRecall <= currentRecall) {
                    clp.printStatistics(0, "", "");
                    break;
                }
            }
//            System.out.println("Distinct comparisons\t:\t" + counter + "\t" + comparisons.size());
//
//            distinctComparisons.retainAll(comparisons);
//            System.out.println("Common comparisons\t:\t" + distinctComparisons.size());
        }
    }
}
