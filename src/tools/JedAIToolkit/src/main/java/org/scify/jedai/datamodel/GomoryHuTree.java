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

import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Map;

import org.jgrapht.Graphs;
import org.jgrapht.alg.flow.EdmondsKarpMFImpl;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 *
 * @author manos
 * @param <V>
 * @param <E>
 */

public class GomoryHuTree<V, E> {

    protected final SimpleWeightedGraph<V, E> graph;

    public GomoryHuTree(SimpleWeightedGraph<V, E> graph) {
        this.graph = graph;
    }

    private DefaultDirectedWeightedGraph<V, DefaultWeightedEdge> makeDirectedCopy(SimpleWeightedGraph<V, E> graph) {
        final DefaultDirectedWeightedGraph<V, DefaultWeightedEdge> copy = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addAllVertices(copy, graph.vertexSet());
        graph.edgeSet().forEach((e) -> {
            V v1 = graph.getEdgeSource(e);
            V v2 = graph.getEdgeTarget(e);
            Graphs.addEdge(copy, v1, v2, graph.getEdgeWeight(e));
            Graphs.addEdge(copy, v2, v1, graph.getEdgeWeight(e));
        });
        
        return copy;
    }

    public SimpleGraph<Integer, DefaultEdge> MinCutTree() {
        final DefaultDirectedWeightedGraph<V, DefaultWeightedEdge> directedGraph = makeDirectedCopy(graph);
        
        final Map<V, V> predecessors = new HashMap<>();
        final Set<V> vertexSet = directedGraph.vertexSet();
        final Iterator<V> it = vertexSet.iterator();
        V start = it.next();
        predecessors.put(start, start);
        while (it.hasNext()) {
            V vertex = it.next();
            predecessors.put(vertex, start);
        }

        final DefaultDirectedWeightedGraph<V, DefaultWeightedEdge> returnGraphClone = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        final SimpleGraph<Integer, DefaultEdge> returnGraph = new SimpleGraph<>(DefaultEdge.class);
        final EdmondsKarpMFImpl<V, DefaultWeightedEdge> minSourceSinkCut = new EdmondsKarpMFImpl<>(directedGraph);
        
        final Iterator<V> itVertices = directedGraph.vertexSet().iterator();
        itVertices.next();
        while (itVertices.hasNext()) {
            V vertex = itVertices.next();
            V predecessor = predecessors.get(vertex);
            float flowValue = (float) minSourceSinkCut.calculateMinCut(vertex, predecessor); // TODO: is this right?

            returnGraphClone.addVertex(vertex);
            returnGraphClone.addVertex(predecessor);
            
            returnGraph.addVertex(Integer.parseInt(vertex + ""));
            returnGraph.addVertex(Integer.parseInt(predecessor + ""));
            
            final Set<V> sourcePartition = minSourceSinkCut.getSourcePartition();
//            float flowValue = minSourceSinkCut.getCutWeight();
            DefaultWeightedEdge e = returnGraphClone.addEdge(vertex, predecessor);
            returnGraph.addEdge(Integer.parseInt(vertex + ""), Integer.parseInt(predecessor + ""));
            returnGraphClone.setEdgeWeight(e, flowValue);

            graph.vertexSet().stream().filter((sourceVertex) -> (predecessors.get(sourceVertex).equals(predecessor)
                    && sourcePartition.contains(sourceVertex))).forEachOrdered((sourceVertex) -> {
                        predecessors.put(sourceVertex, vertex);
            });
        }
        
        return returnGraph;
    }
}
