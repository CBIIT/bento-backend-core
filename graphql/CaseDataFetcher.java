package gov.nih.nci.bento.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hand-written DataFetcher for the {case(case_id)} root query.
 *
 * neo4j-graphql-java derives Cypher node variable names directly from the GraphQL
 * type name. {type case} produces {case} as the variable, which is a
 * reserved keyword in Cypher and causes a syntax error. This fetcher executes
 * hand-written Cypher using the safe alias {c} and returns a fully pre-fetched
 * Map so that nested {@relation} sub-fields are resolved by graphql-java's
 * default PropertyDataFetcher without triggering any further library-generated Cypher.
 *
 * Remove this class if neo4j-graphql-java is upgraded to a version that backtick-
 * escapes reserved keywords used as Cypher variable names.
 */
public class CaseDataFetcher implements DataFetcher<Object> {

    private static final String CYPHER = """
            MATCH (c:case {case_id: $case_id})
            OPTIONAL MATCH (c)-[:member_of]->(co:cohort)
            OPTIONAL MATCH (c)-[:member_of]->(s:study)
            OPTIONAL MATCH (c)<-[:of_case]-(e:enrollment)
            OPTIONAL MATCH (c)<-[:of_case]-(demo:demographic)
            OPTIONAL MATCH (c)-[:of_study_arm]->(sa:study_arm)
            OPTIONAL MATCH (c)-[:had_adverse_event]->(hadae:adverse_event)
            OPTIONAL MATCH (c)-[:went_off_study]->(os:off_study)
            OPTIONAL MATCH (c)-[:went_off_treatment]->(ot:off_treatment)
            OPTIONAL MATCH (c)-[:represents]->(ci:canine_individual)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci
            OPTIONAL MATCH (c)<-[:of_case]-(diag:diagnosis)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, collect(DISTINCT diag) AS diagnoses
            OPTIONAL MATCH (c)<-[:of_case]-(cy:cycle)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, collect(DISTINCT cy) AS cycles
            OPTIONAL MATCH (c)<-[:of_case]-(fu:follow_up)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, collect(DISTINCT fu) AS follow_ups
            OPTIONAL MATCH (c)<-[:of_case]-(sp:sample)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, follow_ups, collect(DISTINCT sp) AS samples
            OPTIONAL MATCH (c)<-[:of_case]-(fl:file)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, follow_ups, samples, collect(DISTINCT fl) AS files
            OPTIONAL MATCH (c)<-[:of_case]-(vi:visit)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, follow_ups, samples, files, collect(DISTINCT vi) AS visits
            OPTIONAL MATCH (c)<-[:of_case]-(ae:adverse_event)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, follow_ups, samples, files, visits, collect(DISTINCT ae) AS adverse_events
            OPTIONAL MATCH (c)<-[:of_case]-(rg:registration)
            WITH c, co, s, e, demo, sa, hadae, os, ot, ci, diagnoses, cycles, follow_ups, samples, files, visits, adverse_events, collect(DISTINCT rg) AS registrations
            RETURN c {
              .*,
              cohort: co { .* },
              study: s { .* },
              enrollment: e { .* },
              demographic: demo { .* },
              diagnoses: [x IN diagnoses | x { .* }],
              cycles: [x IN cycles | x { .* }],
              follow_ups: [x IN follow_ups | x { .* }],
              samples: [x IN samples | x { .* }],
              files: [x IN files | x { .* }],
              visits: [x IN visits | x { .* }],
              adverse_events: [x IN adverse_events | x { .* }],
              registrations: [x IN registrations | x { .* }],
              study_arm: sa { .* },
              adverse_event: hadae { .* },
              off_study: os { .* },
              off_treatment: ot { .* },
              canine_individual: ci { .* }
            } AS c
            """;

    private final Driver driver;

    public CaseDataFetcher(Driver driver) {
        this.driver = driver;
    }

    @Override
    public Object get(DataFetchingEnvironment env) {
        String caseId = env.getArgument("case_id");
        try (Session session = driver.session()) {
            Result result = session.run(CYPHER, Map.of("case_id", caseId));
            List<Object> list = new ArrayList<>();
            if (result.hasNext()) {
                Record rec = result.next();
                list.add(rec.get("c").asObject());
            }
            return list;
        }
    }
}
