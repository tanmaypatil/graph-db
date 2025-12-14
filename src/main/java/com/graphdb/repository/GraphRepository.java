package com.graphdb.repository;

import com.graphdb.connection.Neo4jConnection;
import com.graphdb.model.Defect;
import com.graphdb.model.Developer;
import com.graphdb.model.Skill;
import com.graphdb.model.Team;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.neo4j.driver.Values.parameters;

public class GraphRepository {
    private static final Logger logger = LoggerFactory.getLogger(GraphRepository.class);
    private final Driver driver;

    public GraphRepository() {
        this.driver = Neo4jConnection.getInstance().getDriver();
    }

    // ==================== CREATE OPERATIONS ====================

    public void createDeveloper(Developer developer) {
        String cypher = "CREATE (d:Developer {id: $id, name: $name, teamId: $teamId})";
        try (Session session = driver.session()) {
            session.run(cypher, parameters(
                    "id", developer.getId(),
                    "name", developer.getName(),
                    "teamId", developer.getTeamId()
            ));
            logger.info("Created developer: {}", developer.getName());
        }
    }

    public void createDefect(Defect defect) {
        String cypher = "CREATE (d:Defect {id: $id, title: $title, severity: $severity, status: $status})";
        try (Session session = driver.session()) {
            session.run(cypher, parameters(
                    "id", defect.getId(),
                    "title", defect.getTitle(),
                    "severity", defect.getSeverity(),
                    "status", defect.getStatus()
            ));
            logger.info("Created defect: {}", defect.getTitle());
        }
    }

    public void createSkill(Skill skill) {
        String cypher = "CREATE (s:Skill {id: $id, name: $name, level: $level})";
        try (Session session = driver.session()) {
            session.run(cypher, parameters(
                    "id", skill.getId(),
                    "name", skill.getName(),
                    "level", skill.getLevel()
            ));
            logger.info("Created skill: {}", skill.getName());
        }
    }

    public void createTeam(Team team) {
        String cypher = "CREATE (t:Team {id: $id, name: $name, location: $location})";
        try (Session session = driver.session()) {
            session.run(cypher, parameters(
                    "id", team.getId(),
                    "name", team.getName(),
                    "location", team.getLocation()
            ));
            logger.info("Created team: {}", team.getName());
        }
    }

    // ==================== RELATIONSHIP OPERATIONS ====================

    public void assignSkillToDeveloper(String developerId, String skillId) {
        String cypher = """
                MATCH (d:Developer {id: $developerId})
                MATCH (s:Skill {id: $skillId})
                CREATE (d)-[:HAS_SKILL]->(s)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, parameters("developerId", developerId, "skillId", skillId));
            logger.info("Assigned skill {} to developer {}", skillId, developerId);
        }
    }

    public void assignDefectToDeveloper(String defectId, String developerId) {
        String cypher = """
                MATCH (d:Developer {id: $developerId})
                MATCH (def:Defect {id: $defectId})
                CREATE (d)-[:ASSIGNED_TO]->(def)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, parameters("developerId", developerId, "defectId", defectId));
            logger.info("Assigned defect {} to developer {}", defectId, developerId);
        }
    }

    public void assignDeveloperToTeam(String developerId, String teamId) {
        String cypher = """
                MATCH (d:Developer {id: $developerId})
                MATCH (t:Team {id: $teamId})
                CREATE (d)-[:MEMBER_OF]->(t)
                """;
        try (Session session = driver.session()) {
            session.run(cypher, parameters("developerId", developerId, "teamId", teamId));
            logger.info("Assigned developer {} to team {}", developerId, teamId);
        }
    }

    // ==================== QUERY OPERATIONS ====================

    /**
     * Find all developers in a specific team
     */
    public List<Developer> findDevelopersByTeam(String teamName) {
        String cypher = """
                MATCH (d:Developer)-[:MEMBER_OF]->(t:Team {name: $teamName})
                RETURN d.id AS id, d.name AS name, d.teamId AS teamId
                """;
        List<Developer> developers = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, parameters("teamName", teamName));
            while (result.hasNext()) {
                Record record = result.next();
                Developer dev = new Developer(
                        record.get("id").asString(),
                        record.get("name").asString(),
                        record.get("teamId").asString()
                );
                developers.add(dev);
            }
        }
        logger.info("Found {} developers in team {}", developers.size(), teamName);
        return developers;
    }

    /**
     * Find all defects assigned to a specific developer
     */
    public List<Defect> findDefectsAssignedToDeveloper(String developerName) {
        String cypher = """
                MATCH (d:Developer {name: $developerName})-[:ASSIGNED_TO]->(def:Defect)
                RETURN def.id AS id, def.title AS title, def.severity AS severity, def.status AS status
                """;
        List<Defect> defects = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, parameters("developerName", developerName));
            while (result.hasNext()) {
                Record record = result.next();
                Defect defect = new Defect(
                        record.get("id").asString(),
                        record.get("title").asString(),
                        record.get("severity").asString(),
                        record.get("status").asString()
                );
                defects.add(defect);
            }
        }
        logger.info("Found {} defects for developer {}", defects.size(), developerName);
        return defects;
    }

    /**
     * Find developers with most defects (ranked)
     */
    public Map<String, Integer> findDevelopersWithMostDefects() {
        String cypher = """
                MATCH (d:Developer)-[:ASSIGNED_TO]->(def:Defect)
                RETURN d.name AS developerName, count(def) AS defectCount
                ORDER BY defectCount DESC
                """;
        Map<String, Integer> developerDefectCount = new HashMap<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher);
            while (result.hasNext()) {
                Record record = result.next();
                developerDefectCount.put(
                        record.get("developerName").asString(),
                        record.get("defectCount").asInt()
                );
            }
        }
        logger.info("Ranked {} developers by defect count", developerDefectCount.size());
        return developerDefectCount;
    }

    /**
     * Multi-hop traversal: Given a defect, find all skills of developers working on it
     */
    public List<String> findSkillsOfDevelopersWorkingOnDefect(String defectId) {
        String cypher = """
                MATCH (d:Developer)-[:ASSIGNED_TO]->(def:Defect {id: $defectId})
                MATCH (d)-[:HAS_SKILL]->(s:Skill)
                RETURN DISTINCT s.name AS skillName
                ORDER BY skillName
                """;
        List<String> skills = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, parameters("defectId", defectId));
            while (result.hasNext()) {
                Record record = result.next();
                skills.add(record.get("skillName").asString());
            }
        }
        logger.info("Found {} skills for defect {}", skills.size(), defectId);
        return skills;
    }

    /**
     * Multi-hop traversal: Find all unique skills within a team
     */
    public List<String> findAllSkillsInTeam(String teamName) {
        String cypher = """
                MATCH (d:Developer)-[:MEMBER_OF]->(t:Team {name: $teamName})
                MATCH (d)-[:HAS_SKILL]->(s:Skill)
                RETURN DISTINCT s.name AS skillName
                ORDER BY skillName
                """;
        List<String> skills = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, parameters("teamName", teamName));
            while (result.hasNext()) {
                Record record = result.next();
                skills.add(record.get("skillName").asString());
            }
        }
        logger.info("Found {} skills in team {}", skills.size(), teamName);
        return skills;
    }

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Clear entire database (use with caution)
     */
    public void clearDatabase() {
        String cypher = "MATCH (n) DETACH DELETE n";
        try (Session session = driver.session()) {
            session.run(cypher);
            logger.info("Database cleared");
        }
    }
}
