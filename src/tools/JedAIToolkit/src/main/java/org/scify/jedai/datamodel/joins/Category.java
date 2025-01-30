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
package org.scify.jedai.datamodel.joins;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mthanos
 */
public class Category {

    public static int N;
    public static int METHOD;
    public static float THRESHOLD;

    // [s_len, e_len]
    public int s_len;
    public int e_len;

    public int N1, N2;
    public int K, K2;

    public int[][] range_start;
    public int[][] range_end;

    public int sig_len;
    public List<TIntList> subs;
    public HashMap<Integer, TIntList>[] sig_map;

    public Category(int len, float threshold, int categoryN) {
        Category.THRESHOLD = threshold;
        Category.N = categoryN;
        s_len = len;
        e_len = (int) (s_len / THRESHOLD);
        K = (int) (2 * (1 - THRESHOLD) / (1 + THRESHOLD) * e_len);
        N1 = K + 1;
        N2 = 2;

        K2 = (K + 1) / N1 - 1;

        // important fix
        if ((K + 1) % N1 != 0) {
            K2++;
        }

        if (N1 > K + 1 || N1 * N2 <= K + 1) {
            return;
        }

        subs = new ArrayList<>();
        int n = N2;
        int k = N2 - K2;
        TIntList sub = new TIntArrayList();
        int s;
        for (s = 0; s < k; s++) {
            sub.add(s);
        }
        subs.add(sub);

        while (sub.get(0) < n - k) {
            for (s = 0; s < k; s++) {
                if (sub.get(k - s - 1) < n - s - 1) {
                    break;
                }
            }
            s = k - s - 1;
            sub.set(s, sub.get(s) + 1);
            s++;
            for (; s < k; s++) {
                sub.set(s, sub.get(s - 1) + 1);
            }
            subs.add(sub);
        }

        sig_len = N1 * subs.size();
        //System.out.println("ss "+sig_len);
        sig_map = new HashMap[sig_len];
        for (int is = 0; is < sig_len; is++) {
            sig_map[is] = new HashMap<>();
        }

        int[] t = new int[N1 * N2];
        range_start = new int[N1][N2];
        for (int kk = 0; kk < N1; kk++) {
            range_start[kk][0] = t[kk * N2];
        }

        t = new int[N1 * N2];
        range_end = new int[N1][N2];
        for (int kk = 0; kk < N1; kk++) {
            range_end[kk][0] = t[kk * N2];
        }

        //System.out.println("n1 n2 "+N+" "+N1+" "+N2);
        for (int i = 0; i < N1; i++) {
            for (int j = 0; j < N2; j++) {
                range_start[i][j] = N * (N2 * i + j) / N1 / N2;
                range_end[i][j] = N * (N2 * i + j + 1) / N1 / N2;
                /*System.out.println("rs "+range_start[i][j]);
                System.out.println("re "+range_end[i][j]);*/
            }
        }
    }
}
