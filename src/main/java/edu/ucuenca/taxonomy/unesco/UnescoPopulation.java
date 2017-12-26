/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucuenca.taxonomy.unesco;

import edu.ucuenca.taxonomy.entitymanagement.DBPediaExpansion;
import edu.ucuenca.taxonomy.entitymanagement.SpotlightRecognition;
import edu.ucuenca.taxonomy.entitymanagement.api.EntityExpansion;
import edu.ucuenca.taxonomy.entitymanagement.api.EntityRecognition;
import edu.ucuenca.taxonomy.unesco.dababase.utils.GraphOperations;
import edu.ucuenca.taxonomy.unesco.exceptions.ResourceSizeException;
import java.util.Collections;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class UnescoPopulation {

    Logger log = LoggerFactory.getLogger(UnescoPopulation.class);

    private final GraphTraversalSource g;
    private static final ValueFactory vf = ValueFactoryImpl.getInstance();
    private final EntityRecognition spotlight = SpotlightRecognition.getInstance();
    private final EntityExpansion dbpedia;

    public UnescoPopulation(GraphTraversalSource g) {
        this.g = g;
        this.dbpedia = new DBPediaExpansion(g);
    }

    public void populate() {
        try (UnescoNomeclatureConnection conn = UnescoNomeclatureConnection.getInstance()) {
            UnescoNomeclature unesco = new UnescoNomeclature(conn);
            UnescoPopulation.this.populateNodes(unesco.sixDigitResources(), unesco);
        } catch (Exception ex) {
            log.error("Cannot populate Unesco nomenclature", ex);
        }
    }

    private void populateNodes(List<URI> unescoURIs, UnescoNomeclature unesco) {
        unescoURIs.stream()
                .filter(uri -> !unesco.code(uri).contains("99"))
                .map(uri -> findEntities(uri, unesco))
                .map(uris -> dbpedia.expand(uris));
    }

    private List<URI> findEntities(URI uri, UnescoNomeclature unesco) {
        String label = unesco.label(uri, "en").getLabel();
        List<URI> entities = spotlight.getEntities(label);
        Vertex unescoVertex = GraphOperations.insertIdV(g, uri.stringValue(), "unesco");

        entities.stream().map(entity -> {
            Vertex entityVertex = GraphOperations.insertIdV(g, entity.stringValue(), "node");
            entityVertex.addEdge("sameAs", unescoVertex, "label", label);
            return entity;
        });
        if (entities.isEmpty()) {
            try {
                URI parent = unesco.broad(uri);
                if (parent != null) {
                    UnescoPopulation.this.populateNodes(Collections.singletonList(parent), unesco);
                }
            } catch (ResourceSizeException ex) {
                log.error("{} should return only a parent", uri, ex);
            }
        }
        return entities;
    }

//    public static void main(String[] args) throws Exception {
//        try (Graph graph = StardogConnection.intance().graph()) {
//            UnescoPopulation up = new UnescoPopulation(graph.traversal());
//            up.populate();
//        }
////<editor-fold defaultstate="collapsed" desc="some test for unesco nomenclature">
////        ValueFactory vf = ValueFactoryImpl.getInstance();
////        URI two_digit = vf.createURI("http://skos.um.es/unesco6/12");
////        URI four_digit = vf.createURI("http://skos.um.es/unesco6/1203");
////        URI six_digit = vf.createURI("http://skos.um.es/unesco6/120304");
////        EntityRecognition sp = SpotlightRecognition.getInstance();
////
////        try (UnescoNomeclatureConnection conn = UnescoNomeclatureConnection.getInstance()) {
////            UnescoNomeclature un = new UnescoNomeclature(conn);
////            System.out.println();
////            for (URI field : Arrays.asList(two_digit)) {//un.twoDigitResources()) {
////                String fieldStr = un.label(field, "en").getLabel();
////                System.out.print(field);
////                System.out.println("->" + fieldStr);
////
////                for (URI discipline : un.narrow(field)) {
////                    String disciplineStr = un.label(discipline, "en").getLabel();
////                    System.out.print("\t" + discipline);
////                    System.out.println("->" + disciplineStr);
////
////                    if (String.valueOf(un.code(discipline)).contains("99")) {
////                        continue;
////                    }
////                    String context = disciplineStr;
////                    for (URI entity : sp.getEntities(context, 0.15)) {
////                        System.out.println("\t\t\t" + context + "::" + entity);
////                    }
//////                    for (URI subdiscipline : un.narrow(discipline)) {
//////                        String subdisciplineStr = un.label(subdiscipline, "en").label();
//////                        System.out.print("\t\t" + subdiscipline);
//////                        System.out.println("->" + subdisciplineStr);
//////                        String context = fieldStr + ", " + disciplineStr + ", " + subdisciplineStr;
//////                        for (URI entity : sp.getEntities(context)) {
//////                            System.out.println("\t\t\t" + entity);
//////                        }
//////                    }
////                }
////            }
////        } catch (Exception ex) {
////        }
////</editor-fold>
//
//    }
}
