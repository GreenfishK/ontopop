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
import java.util.Collections;
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
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entitymatching.IEntityMatching;
import org.scify.jedai.entitymatching.ProfileMatcher;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class TestDirtyERBaseline {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        float[] bestThresholds = {0.75f, 0.45f};
        RepresentationModel[] bestModels = {RepresentationModel.CHARACTER_BIGRAM_GRAPHS, RepresentationModel.CHARACTER_BIGRAMS_TF_IDF};
        SimilarityMetric[] bestMetrics = {SimilarityMetric.GRAPH_OVERALL_SIMILARITY, SimilarityMetric.GENERALIZED_JACCARD_SIMILARITY};

        String mainDir = "/home/gap2/data/JedAIdata/datasets/dirtyErDatasets/";
        String[] profilesFile = {"cddbProfiles", "coraProfiles"};
        String[] groundtruthFile = {"cddbIdDuplicates", "coraIdDuplicates"};

        for (int i = 0; i < groundtruthFile.length; i++) {
            IEntityReader eReader = new EntitySerializationReader(mainDir + profilesFile[i]);
            List<EntityProfile> profiles = eReader.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthFile[i]);
            final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            IBlockBuilding blockBuildingMethod = new StandardBlocking();
            List<AbstractBlock> blocks = blockBuildingMethod.getBlocks(profiles);
            System.out.println("Original blocks\t:\t" + blocks.size());

            IBlockProcessing blockCleaningMethod1 = new ComparisonsBasedBlockPurging(false);
            blocks = blockCleaningMethod1.refineBlocks(blocks);

            IBlockProcessing blockCleaningMethod2 = new BlockFiltering();
            blocks = blockCleaningMethod2.refineBlocks(blocks);

            IBlockProcessing comparisonCleaningMethod = new CardinalityNodePruning(WeightingScheme.ARCS);
            List<AbstractBlock> cnpBlocks = comparisonCleaningMethod.refineBlocks(blocks);

            List<Comparison> allComparisons = new ArrayList<>();
            for (AbstractBlock block : cnpBlocks) {
                final ComparisonIterator cIterator = block.getComparisonIterator();
                while (cIterator.hasNext()) {
                    allComparisons.add(cIterator.next());
                }
            }
            System.out.println("Total comparisons\t:\t" + allComparisons.size());

            final IEntityMatching emOr = new ProfileMatcher(profiles, bestModels[i], bestMetrics[i]);
            SimilarityPairs originalSims = emOr.executeComparisons(cnpBlocks);
            System.out.println("Executed comparisons\t:\t" + originalSims.getNoOfComparisons());

//            Map<Comparison, Double> comparisonWeight = new HashMap<>();
//            PairIterator pi = originalSims.getPairIterator();
//            while (pi.hasNext()) {
//                Comparison c = pi.next();
//                comparisonWeight.put(c, c.getUtilityMeasure());
//            }
            IEntityClustering ec = new ConnectedComponentsClustering(bestThresholds[i]);
            EquivalenceCluster[] clusters = ec.getDuplicates(originalSims);

            ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(0, "", "");
            float originalRecall = clp.getRecall();

            final IEntityMatching em = new ProfileMatcher(profiles, bestModels[i], bestMetrics[i]);
            SimilarityPairs sims = new SimilarityPairs(false, allComparisons.size());

            int counter = 0;
//            int missingComparisons = 0;
            Collections.shuffle(allComparisons);
            for (Comparison comparison : allComparisons) {
                counter++;

                float similarity = em.executeComparison(comparison);
                comparison.setUtilityMeasure(similarity);
//                Double originalSim = comparisonWeight.get(comparison);
//                if (originalSim == null) {
//                    missingComparisons++;
//                } else {
//                    if (!originalSim.equals(similarity)) {
//                        System.out.println(similarity + "\t" + originalSim);
//                    }
//                }
                sims.addComparison(comparison);
//            }
//            System.out.println("Misssing comparisons\t:\t" + missingComparisons);

                ec = new ConnectedComponentsClustering(bestThresholds[i]);
                clusters = ec.getDuplicates(sims);

                clp = new ClustersPerformance(clusters, duplicatePropagation);
                clp.setStatistics();
                float currentRecall = clp.getRecall();
                System.out.println("Current recall\t:\t" + counter + "\t" + currentRecall);
                if (originalRecall <= currentRecall) {
                    clp.printStatistics(0, "", "");
                    break;
                }
            }
        }
    }
}
