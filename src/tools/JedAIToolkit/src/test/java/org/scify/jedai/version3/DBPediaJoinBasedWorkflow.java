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
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.UniqueMappingClustering;
import org.scify.jedai.similarityjoins.tokenbased.*;
import org.scify.jedai.utilities.ClustersPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

/**
 *
 * @author gap2
 */
public class DBPediaJoinBasedWorkflow {

    public static void main(String[] args) {
        BasicConfigurator.configure();

//        String mainDir = "/home/gpapadakis/data/ccerDatasets/allDatasets/";
        String mainDir = "/home/gpapadakis/data/datasets/";
        String datasetsD1 = "cleanDBPedia1";
        String datasetsD2 = "cleanDBPedia2";
        String groundtruthDirs = "newDBPediaMatches";

        final IEntityReader eReader1 = new EntitySerializationReader(mainDir + datasetsD1);
        final List<EntityProfile> profiles1 = eReader1.getEntityProfiles();
        System.out.println("\n\nInput Entity Profiles D1\t:\t" + profiles1.size());

        final IEntityReader eReader2 = new EntitySerializationReader(mainDir + datasetsD2);
        final List<EntityProfile> profiles2 = eReader2.getEntityProfiles();
        System.out.println("\n\nInput Entity Profiles D2\t:\t" + profiles2.size());

        IGroundTruthReader gtReader = new GtSerializationReader(mainDir + groundtruthDirs);
        final AbstractDuplicatePropagation duplicatePropagation = new BilateralDuplicatePropagation(gtReader.getDuplicatePairs(null));
        System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());

        for (float simThreshold = 0.7f; simThreshold < 0.99; simThreshold += 0.1) {
            long time1 = System.currentTimeMillis();
//            AllPairs join = new AllPairs(simThreshold[datasetId]);
            PPJoin join = new PPJoin(simThreshold);
            SimilarityPairs simPairs = join.executeFiltering("name", "name", profiles1, profiles2);
            System.out.println(simPairs.getNoOfComparisons());

            final IEntityClustering ec = new UniqueMappingClustering(simThreshold);
            final EquivalenceCluster[] clusters = ec.getDuplicates(simPairs);

            long time2 = System.currentTimeMillis();

            final ClustersPerformance clp = new ClustersPerformance(clusters, duplicatePropagation);
            clp.setStatistics();
            clp.printStatistics(0, "", "");

            System.out.println("Running time\t:\t" + (time2 - time1));
        }
    }
}
