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
package org.scify.jedai.progressivejoin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.scify.jedai.datamodel.Comparison;

/**
 *
 * @author mthanos
 */
public final class SetSimJoin {

    private LinkedHashMap<String, ArrayList<Integer>> records;
    private ArrayList<Comparison> results;

    private LinkedHashMap<Integer, ArrayList<Integer>> records_internal;
    private HashMap<Integer, String> mapper;

    private boolean isCleanCleanER;
    private int noOfEntities;
    private int datasetDelimiter;
    private boolean[] isindataset1;

    public SetSimJoin() {
        results = new ArrayList<>();
        records_internal = new LinkedHashMap<>();
        mapper = new HashMap<>();
    }

    public SetSimJoin(LinkedHashMap<String, ArrayList<Integer>> records) {
        setRecords(records);
    }

    public void topkGlobal(Integer k) {
        if (records == null) {
            throw new IllegalArgumentException("Records missing");
        }
        if (k == null) {
            throw new IllegalArgumentException("k missing");
        }

        if (isCleanCleanER()) {
            mapRecordsCleanClean();
        } else {
            mapRecords();
        }
        TopkGlobal topk = new TopkGlobal(records_internal, new JaccardTopK(k), results);
        topk.setIscleancleanEr(isCleanCleanER);
        topk.setIsindataset1(isindataset1);

        topk.run();
    }

    public void setRecords(LinkedHashMap<String, ArrayList<Integer>> records) {
        this.records = records;
    }

    public ArrayList<Comparison> getResults() {
        return results;
    }

    private void mapRecords() {
        final int[] internalid = {0};
        records.forEach((key, value) -> {
            records_internal.put(internalid[0], value);
            mapper.put(internalid[0]++, key);
        });
    }

    private void mapRecordsCleanClean() {
        isindataset1 = new boolean[noOfEntities];
        final int[] internalid = {0};
        records.forEach((key, value) -> {
            isindataset1[internalid[0]] = Integer.parseInt(key) < datasetDelimiter;
            records_internal.put(internalid[0], value);
            mapper.put(internalid[0]++, key);
        });
    }

    public boolean isCleanCleanER() {
        return isCleanCleanER;
    }

    public void setCleanCleanER(boolean cleanCleanER) {
        this.isCleanCleanER = cleanCleanER;
    }

    public void setDatasetDelimiter(int datasetDelimiter) {
        this.datasetDelimiter = datasetDelimiter;
    }

    public int getNoOfEntities() {
        return noOfEntities;
    }

    public void setNoOfEntities(int noOfEntities) {
        this.noOfEntities = noOfEntities;
    }

}
