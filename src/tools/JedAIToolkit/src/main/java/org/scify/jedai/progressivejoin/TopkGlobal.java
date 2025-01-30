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

import java.util.*;
import org.scify.jedai.datamodel.Comparison;

/**
 *
 * @author mthanos
 */
public class TopkGlobal {

    private final List<Comparison> results;
    private final JaccardTopK similarity;
    private final LinkedHashMap<Integer, ArrayList<Integer>> records;

    private boolean[] isindataset1;
    private boolean iscleancleanEr;

    /**
     * @param records
     * @param similarity
     * @param results the results from {@link #run()} will be added into this
     * list
     */
    public TopkGlobal(LinkedHashMap<Integer, ArrayList<Integer>> records, JaccardTopK similarity, List<Comparison> results) {
        this.records = records;
        this.similarity = similarity;
        this.results = results;
    }

    public void run() {
        InvertedIndex ind = new InvertedIndex();
//        int candcount = 0;

        // <recordid, position, threshold>
        PriorityQueue<Object[]> events = new PriorityQueue<>((o1, o2) -> {
            if ((Float) o1[2] > (Float) o2[2]) {
                return -1;
            } else if (Math.abs((Float) o1[2] - (Float) o2[2]) < 1E-13 && (int) o1[0] > (int) o2[0]) {
                return -1;
            } else if (Math.abs((Float) o1[2] - (Float) o2[2]) < 1E-13 && (int) o1[0] == (int) o2[0]) {
                return 0;
            } else {
                return 1;
            }
        });
        records.forEach((recordid, tokens) -> events.add(new Object[]{recordid, 0, 1.0}));

        SortedList<Comparison> tmpresults = new SortedList<>((o1, o2) -> {
            if (Math.abs(o1.getUtilityMeasure() - o2.getUtilityMeasure()) < 1E-13) {
                return 0;
            } else if (o1.getUtilityMeasure() > o2.getUtilityMeasure()) {
                return -1;
            } else {
                return 1;
            }
        });

        // <recordid1, recordid2>
        Set<ArrayList<Integer>> testonce = new HashSet<>();

        boolean do_index_insertion = true;

        while (!events.isEmpty()) {
            Object[] curevent = events.poll();
            int recordid = (int) curevent[0];

            float thres = getThres(tmpresults, similarity.k);
            // stop if thresholds in result queue are all bigger than possible thresholds in event queue
            if (thres > (float) curevent[2]) {
                break;
            }

            ArrayList<Integer> record = records.get(recordid);
            int reclen = record.size();
            Integer token = record.get((int) curevent[1]);

            ListHead indheader = ind.getHeader(token);
            List<int[]> indlist = indheader.getInvlist();

            for (ListIterator<int[]> iter = indlist.listIterator(); iter.hasNext();) {
                int pos = iter.nextIndex();
                int[] indrecordpair = iter.next();
                int indrecid = indrecordpair[0];
                if (iscleancleanEr) {
                    if (isindataset1[recordid] == isindataset1[indrecid]) {
                        continue;
                    }
                }
                ArrayList<Integer> indexedrecord = records.get(indrecid);
                int indreclen = indexedrecord.size();

                // list shortening - SimilarityUpperBound-Access
                if (similarity.upperbound_access(reclen, indreclen, (int) curevent[1], indrecordpair[1]) < thres) {
                    indheader.cutoff_from(pos);
                    break;
                }

                // length filter
                if (indreclen >= similarity.minsize_orig(indreclen, thres) && indreclen <= similarity.maxsize_orig(indreclen, thres)) {
                    ArrayList<Integer> testpair = new ArrayList<>(Arrays.asList(Math.min(recordid, indrecid), Math.max(recordid, indrecid)));
                    if (!testonce.contains(testpair)) {

//                        candcount++;
                        float maxrecprobepos = similarity.maxprefix(reclen, thres);
                        float maxindrecprobepos = similarity.maxprefix(indreclen, thres);

                        int minoverlap = similarity.minoverlap(reclen, indreclen, thres);

                        // <sim_overlap, nextposrec, nextposindrec>
                        int[] verified = Verify.verifiy_sim(record, indexedrecord, minoverlap, 1,
                                (int) curevent[1] + 1, indrecordpair[1] + 1);

                        float sim_val = similarity.computesim(reclen, indreclen, verified[0]);

                        if (verified[1] < maxrecprobepos && verified[2] < maxindrecprobepos) {
                            testonce.add(testpair);
                        }

                        Comparison c = new Comparison(iscleancleanEr, recordid, indrecid);
                        c.setUtilityMeasure(sim_val);
                        tmpresults.add(c);
                        thres = getThres(tmpresults, similarity.k);
                    }
                }
            }

            if (tmpresults.size() >= similarity.k) {
                tmpresults.subList(similarity.k, tmpresults.size()).clear();
            }

            // SimilarityUpperBound-Probe
            thres = similarity.upperbound_probe(reclen, (int) curevent[1] + 1);

            if ((int) curevent[1] + 1 < reclen) {
                events.add(new Object[]{recordid, (int) curevent[1] + 1, thres});
            }

            if (do_index_insertion) {
                float sim_upperbound_index = similarity.upperbound_index(reclen, (int) curevent[1]);
                if (sim_upperbound_index > thres) {
                    ind.add(token, new int[]{recordid, (int) curevent[1]});
                } else {
                    do_index_insertion = false;
                }
            }
        }

        results.addAll(tmpresults.subList(0, tmpresults.size() < similarity.k ? tmpresults.size() : similarity.k));
//        System.out.println("SSJ Candidates: " + candcount);
//        System.out.println("SSJ Result Count: " + results.size());
    }

    private float getThres(SortedList<Comparison> tmpresults, int k) {
        if (tmpresults.size() >= k) {
            return tmpresults.get(k - 1).getUtilityMeasure();
        }
        return 0.0f;
    }

    public void setIsindataset1(boolean[] isindataset1) {
        this.isindataset1 = isindataset1;
    }

    public void setIscleancleanEr(boolean iscleancleanEr) {
        this.iscleancleanEr = iscleancleanEr;
    }
}
