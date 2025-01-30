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
package org.scify.jedai.version3;

import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.similarityjoins.tokenbased.*;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;

/**
 *
 * @author gap2
 */
public class BestConfigurationJoinBasedWorkflowDer {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDir = "/home/gap2/data/JedAIdata/datasets/dirtyErDatasets/";
        String[] profilesFile = {"coraProfiles", "cddbProfiles"};
        String[] attributeNames = {"title", "track08"};
        String[] groundtruthFile = {"coraIdDuplicates", "cddbIdDuplicates"};

//        int datasetId = Integer.parseInt(args[0]);
        float[] simThreshold = {0.70f, 0.80f};

        for (int datasetId = 0; datasetId < simThreshold.length; datasetId++) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + groundtruthFile[datasetId]);

            final IEntityReader eReader1 = new EntitySerializationReader(mainDir + profilesFile[datasetId]);
            final List<EntityProfile> profiles = eReader1.getEntityProfiles();
            System.out.println("\n\nInput Entity Profiles\t:\t" + profiles.size());

            IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthFile[datasetId]);
            final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

            long time1 = System.currentTimeMillis();
//            AllPairs join = new AllPairs(simThreshold[datasetId]);
            PPJoin join = new PPJoin(simThreshold[datasetId]);
            SimilarityPairs simPairs = join.executeFiltering(attributeNames[datasetId], profiles);
            System.out.println(simPairs.getNoOfComparisons());

            final IEntityClustering ec = new ConnectedComponentsClustering(simThreshold[datasetId]);
            final EquivalenceCluster[] clusters = ec.getDuplicates(simPairs);

            long time2 = System.currentTimeMillis();

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(0, "", "");

            System.out.println("Running time\t:\t" + (time2 - time1));
        }
    }
}
