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
package org.scify.jedai.generalexamples;

import java.io.File;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.UnilateralDuplicatePropagation;
import java.util.List;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author G.A.P. II
 */
public class DirtyErDatasetStatistics {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String mainFolder = "data" + File.separator + "dirtyErDatasets" + File.separator;
        String[] entitiesFiles = {mainFolder + "restaurantProfiles",
            mainFolder + "censusProfiles",
            mainFolder + "coraProfiles",
            mainFolder + "cddbProfiles",
            mainFolder + "newDBPedia1",
            mainFolder + "newDBPedia2"
        };
        String[] groundTruthFiles = {mainFolder + "restaurantIdDuplicates",
            mainFolder + "censusIdDuplicates",
            mainFolder + "coraIdDuplicates",
            mainFolder + "cddbIdDuplicates",
            mainFolder + "newDBPedia1",
        };

        for (int i = 0; i < entitiesFiles.length; i++) {
            System.out.println("\n\n\n\n\nCurrent dataset\t:\t" + entitiesFiles[i]);
            
            IEntityReader eReader = new EntitySerializationReader(entitiesFiles[i]);
            List<EntityProfile> profiles = eReader.getEntityProfiles();
            System.out.println("Input Entity Profiles\t:\t" + profiles.size());
            System.out.println("Brute-force Comparisons\t:\t" + (((double)profiles.size())*(profiles.size()-1)/2.0));
            double nameValuePairs = 0;
            for (EntityProfile profile : profiles) {
                nameValuePairs += profile.getProfileSize();
            }
            System.out.println("Total Name-Value Pairs\t:\t" + nameValuePairs);
            System.out.println("Average Name-Value Pairs\t:\t" + nameValuePairs/profiles.size());
            
            IGroundTruthReader gtReader = new GtSerializationReader(groundTruthFiles[i]);
            final AbstractDuplicatePropagation duplicatePropagation = new UnilateralDuplicatePropagation(gtReader.getDuplicatePairs(eReader.getEntityProfiles()));
            System.out.println("Existing Duplicates\t:\t" + duplicatePropagation.getDuplicates().size());       
        }
    }

}
