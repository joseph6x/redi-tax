/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package na;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author cedia
 */
public class DBD {

    private final static List<URI> properties = new ArrayList<>();
    private final static String DBPEDIA_CONTEXT = "https://dbpedia.org/sparql";
    private final static Map<URI, Double> m = new HashMap<>();

    static {
        properties.add(new URIImpl("http://purl.org/dc/terms/subject"));
        properties.add(new URIImpl("http://www.w3.org/2004/02/skos/core#broader"));

        m.put(new URIImpl("http://purl.org/dc/terms/subject"), 1.0);
        m.put(new URIImpl("http://www.w3.org/2004/02/skos/core#broader"), 1.0);
        m.put(new URIImpl("http://dbpedia.org/ontology/genre"), 0.5);
        m.put(new URIImpl("http://dbpedia.org/ontology/wikiPageRedirects"), 0.8);
        m.put(new URIImpl("http://www.w3.org/2004/02/skos/core#related"), 1.0);
        m.put(new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 0.7);
        m.put(new URIImpl("http://www.w3.org/2002/07/owl#Thing"), -0.5);
        m.put(new URIImpl("http://www.w3.org/2000/01/rdf-schema#label"), 1.0);
    }
    SPARQLRepository repository = null;

    public void run(List<URI> uris, int lvl) {

        URIImpl uriImpl = new URIImpl("http://dbpedia.org/resource/Computer_science");
        expand(uriImpl, uriImpl, 3, 0, new HashSet<String>());

    }

    public void expand(URI a, URI b, int level, double w, Set<String> ls) {
        if (level < 1) {
            return;
        }
        if (ls.contains(w + b.toString())) {
            return;
        }
        ls.add(w + b.toString());
        String query = String.format("DESCRIBE <%s> ", b.stringValue());
        RepositoryConnection connection = repository.getConnection();
        GraphQueryResult result = connection.prepareGraphQuery(QueryLanguage.SPARQL, query, DBPEDIA_CONTEXT).evaluate();
        while (result.hasNext()) {
            Statement stmt = result.next();
            Double get = m.get(stmt.getPredicate());
            Value hop = null;
            if (stmt.getObject().equals(b)) {
                hop = stmt.getSubject();
            } else {
                hop = stmt.getObject();
            }
            double ww = w;
            if (get != null) {
                ww += get;
                System.out.println(a.toString() + " " + hop.toString() + " " + ww);
            }
            if (hop.equals(b)) {
                continue;
            }
            if (properties.contains(stmt.getPredicate()) && hop instanceof URI) {
                expand(a, (URI) hop, level - 1, ww, ls);
            }
        }
        result.close();
        connection.close();
    }

    public void Init() {
        Map<String, String> additionalHttpHeaders = new HashMap<>();
        additionalHttpHeaders.put("Accept", "application/ld+json");
        repository = new SPARQLRepository(DBPEDIA_CONTEXT);
        repository.setAdditionalHttpHeaders(additionalHttpHeaders);
        repository.initialize();
    }

}
