package org.scify.jedai.generalexamples;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.entityreader.IEntityReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;

/**
 *
 * @author Georgios
 */
public class PrintDirtyERDatasets {

    private final static String DELIMITER = "|";
    
    private static void printDataset(String inputPath, String outputPath) throws FileNotFoundException {
        final IEntityReader eReader = new EntitySerializationReader(inputPath);
        List<EntityProfile> profiles = eReader.getEntityProfiles();
        System.out.println("\n\n\n\n\nDataset\t:\t" + inputPath);
        System.out.println("Number of Entity Profiles\t:\t" + profiles.size());

        //order attributes alphabetically
        final Set<String> attributes = new HashSet<>();
        for (EntityProfile profile : profiles) {
            for (Attribute attribute : profile.getAttributes()) {
                attributes.add(attribute.getName().toLowerCase().trim());
            }
        }
        String[] orderedAttributes = new String[attributes.size()];
        attributes.toArray(orderedAttributes);
        Arrays.sort(orderedAttributes);

        //print header with attribute names
        PrintWriter writer = new PrintWriter(outputPath);
        writer.write("Entity Id|");
        for (String attribute : orderedAttributes) {
            writer.write(attribute + DELIMITER);
        }
        writer.write("\n");

        int counter = 0;
        for (EntityProfile profile : profiles) {
            writer.write(counter + DELIMITER);
            for (int attributeId = 0; attributeId < orderedAttributes.length; attributeId++) {
                for (Attribute attribute : profile.getAttributes()) {
                    String currentAttribute = attribute.getName().toLowerCase().trim();
                    if (currentAttribute.equals(orderedAttributes[attributeId])) {
                        writer.write(attribute.getValue());
                    }
                }
                writer.write(DELIMITER);
            }
            writer.write("\n");
            counter++;
        }
        writer.close();
    }

    private static void printGroundtruth(String inputPath, String outputPath) throws FileNotFoundException {
        IGroundTruthReader gtReader = new GtSerializationReader(inputPath);
        Set<IdDuplicates> duplicates = gtReader.getDuplicatePairs(null);
        System.out.println("\n\n\n\n\nDataset groundtruth\t:\t" + inputPath);

        PrintWriter writer = new PrintWriter(outputPath);
        for (IdDuplicates pair : duplicates) {
            if (pair.getEntityId1() < pair.getEntityId2()) {
                writer.write(pair.getEntityId1() + DELIMITER + pair.getEntityId2() + "\n");
            } else {
                writer.write(pair.getEntityId2() + DELIMITER + pair.getEntityId1() + "\n");
            }
        }
        writer.close();
    }

    public static void main(String[] args) throws FileNotFoundException {
        BasicConfigurator.configure();

        String[] entitiesFilePath = {"data" + File.separator + "dirtyErDatasets" + File.separator + "cddbProfiles",
            "data" + File.separator + "dirtyErDatasets" + File.separator + "censusProfiles",
            "data" + File.separator + "dirtyErDatasets" + File.separator + "coraProfiles"
        };
        String[] groundTruthFilePath = {"data" + File.separator + "dirtyErDatasets" + File.separator + "cddbIdDuplicates",
            "data" + File.separator + "dirtyErDatasets" + File.separator + "censusIdDuplicates",
            "data" + File.separator + "dirtyErDatasets" + File.separator + "coraIdDuplicates"
        };

        for (int i = 0; i < groundTruthFilePath.length; i++) {
            System.out.println("\n\nCurrent dataset\t:\t" + groundTruthFilePath[i]);

            printDataset(entitiesFilePath[i], entitiesFilePath[i] + ".csv");
            printGroundtruth(groundTruthFilePath[i], groundTruthFilePath[i] + ".csv");
        }
    }
}
