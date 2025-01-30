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
package org.scify.jedai.datamodel;

import gnu.trove.map.TIntFloatMap;

/**
 *
 * @author G.A.P. II
 */
public class VertexWeight {

    private final float weight;
    private final int noOfAdj;
    private final int pos; //id
    private final TIntFloatMap Connections;

    public VertexWeight(int pos, float weight, int noOfAdj, TIntFloatMap Connections) {
        this.pos = pos;
        this.weight = weight;
        this.noOfAdj = noOfAdj;
        this.Connections = Connections;
    }

    public int getPos() {
        return this.pos;
    }

    public float getWeight() {
        return this.weight;
    }

    public int getNoOfAdj() {
        return this.noOfAdj;
    }

    public TIntFloatMap Connections() {
        return this.Connections;
    }

    @Override
    public String toString() {
        return "pos : " + pos + ", weight : " + weight + ", noOfAdj : " + noOfAdj;
    }
}
