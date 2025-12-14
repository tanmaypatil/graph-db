package com.graphdb;

import com.graphdb.model.Defect;
import com.graphdb.model.Developer;
import com.graphdb.model.Skill;
import com.graphdb.model.Team;
import com.graphdb.repository.GraphRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit test class demonstrating Neo4j graph traversal capabilities.
 * Tests cover team-based queries, defect assignment, and multi-hop traversals.
 */
public class GraphTraversalTest {
    private static final Logger logger = LoggerFactory.getLogger(GraphTraversalTest.class);
    private GraphRepository repository;

    @BeforeEach
    public void setUp() {
        logger.info("Setting up test data...");
        repository = new GraphRepository();

        // Clear database before each test
        repository.clearDatabase();

        // Create Teams
        Team backendTeam = new Team("team1", "Backend Team", "San Francisco");
        Team frontendTeam = new Team("team2", "Frontend Team", "New York");
        repository.createTeam(backendTeam);
        repository.createTeam(frontendTeam);

        // Create Developers
        Developer alice = new Developer("dev1", "Alice", "team1");
        Developer bob = new Developer("dev2", "Bob", "team1");
        Developer carol = new Developer("dev3", "Carol", "team2");
        Developer dave = new Developer("dev4", "Dave", "team2");
        repository.createDeveloper(alice);
        repository.createDeveloper(bob);
        repository.createDeveloper(carol);
        repository.createDeveloper(dave);

        // Create Skills
        Skill java = new Skill("skill1", "Java", "EXPERT");
        Skill python = new Skill("skill2", "Python", "INTERMEDIATE");
        Skill react = new Skill("skill3", "React", "EXPERT");
        Skill neo4j = new Skill("skill4", "Neo4j", "BEGINNER");
        repository.createSkill(java);
        repository.createSkill(python);
        repository.createSkill(react);
        repository.createSkill(neo4j);

        // Create Defects
        Defect defect1 = new Defect("defect1", "Login API fails", "HIGH", "OPEN");
        Defect defect2 = new Defect("defect2", "Database connection timeout", "CRITICAL", "OPEN");
        Defect defect3 = new Defect("defect3", "UI button misaligned", "LOW", "CLOSED");
        Defect defect4 = new Defect("defect4", "Performance issue in query", "MEDIUM", "IN_PROGRESS");
        Defect defect5 = new Defect("defect5", "Memory leak in service", "HIGH", "OPEN");
        repository.createDefect(defect1);
        repository.createDefect(defect2);
        repository.createDefect(defect3);
        repository.createDefect(defect4);
        repository.createDefect(defect5);

        // Assign Skills to Developers
        repository.assignSkillToDeveloper("dev1", "skill1"); // Alice -> Java
        repository.assignSkillToDeveloper("dev1", "skill2"); // Alice -> Python
        repository.assignSkillToDeveloper("dev2", "skill1"); // Bob -> Java
        repository.assignSkillToDeveloper("dev2", "skill4"); // Bob -> Neo4j
        repository.assignSkillToDeveloper("dev3", "skill3"); // Carol -> React
        repository.assignSkillToDeveloper("dev4", "skill3"); // Dave -> React
        repository.assignSkillToDeveloper("dev4", "skill2"); // Dave -> Python

        // Assign Defects to Developers
        repository.assignDefectToDeveloper("defect1", "dev1"); // Alice
        repository.assignDefectToDeveloper("defect2", "dev1"); // Alice
        repository.assignDefectToDeveloper("defect3", "dev3"); // Carol
        repository.assignDefectToDeveloper("defect4", "dev1"); // Alice
        repository.assignDefectToDeveloper("defect5", "dev2"); // Bob

        // Assign Developers to Teams
        repository.assignDeveloperToTeam("dev1", "team1"); // Alice -> Backend Team
        repository.assignDeveloperToTeam("dev2", "team1"); // Bob -> Backend Team
        repository.assignDeveloperToTeam("dev3", "team2"); // Carol -> Frontend Team
        repository.assignDeveloperToTeam("dev4", "team2"); // Dave -> Frontend Team

        logger.info("Test data setup complete");
    }

    @AfterEach
    public void tearDown() {
        logger.info("Cleaning up test data...");
        repository.clearDatabase();
    }

    /**
     * Test: Find all developers in a specific team
     * Graph traversal: (Developer)-[:MEMBER_OF]->(Team)
     */
    @Test
    public void testFindDevelopersByTeam() {
        logger.info("Running test: Find developers by team");

        List<Developer> backendDevelopers = repository.findDevelopersByTeam("Backend Team");

        assertEquals(2, backendDevelopers.size(), "Backend Team should have 2 developers");
        assertTrue(backendDevelopers.stream().anyMatch(d -> d.getName().equals("Alice")));
        assertTrue(backendDevelopers.stream().anyMatch(d -> d.getName().equals("Bob")));

        List<Developer> frontendDevelopers = repository.findDevelopersByTeam("Frontend Team");

        assertEquals(2, frontendDevelopers.size(), "Frontend Team should have 2 developers");
        assertTrue(frontendDevelopers.stream().anyMatch(d -> d.getName().equals("Carol")));
        assertTrue(frontendDevelopers.stream().anyMatch(d -> d.getName().equals("Dave")));

        logger.info("Test passed: Found correct developers for each team");
    }

    /**
     * Test: Find all defects assigned to a specific developer
     * Graph traversal: (Developer)-[:ASSIGNED_TO]->(Defect)
     */
    @Test
    public void testFindDefectsForDeveloper() {
        logger.info("Running test: Find defects for developer");

        List<Defect> aliceDefects = repository.findDefectsAssignedToDeveloper("Alice");

        assertEquals(3, aliceDefects.size(), "Alice should have 3 defects assigned");
        assertTrue(aliceDefects.stream().anyMatch(d -> d.getTitle().equals("Login API fails")));
        assertTrue(aliceDefects.stream().anyMatch(d -> d.getTitle().equals("Database connection timeout")));
        assertTrue(aliceDefects.stream().anyMatch(d -> d.getTitle().equals("Performance issue in query")));

        List<Defect> bobDefects = repository.findDefectsAssignedToDeveloper("Bob");

        assertEquals(1, bobDefects.size(), "Bob should have 1 defect assigned");
        assertEquals("Memory leak in service", bobDefects.get(0).getTitle());

        logger.info("Test passed: Found correct defects for each developer");
    }

    /**
     * Test: Find developers with most defects (ranked by defect count)
     * Graph traversal: (Developer)-[:ASSIGNED_TO]->(Defect) with aggregation
     */
    @Test
    public void testFindDevelopersWithMostDefects() {
        logger.info("Running test: Find developers with most defects");

        Map<String, Integer> defectCounts = repository.findDevelopersWithMostDefects();

        assertEquals(3, defectCounts.size(), "Should have defect counts for 3 developers");
        assertEquals(3, defectCounts.get("Alice"), "Alice should have 3 defects");
        assertEquals(1, defectCounts.get("Bob"), "Bob should have 1 defect");
        assertEquals(1, defectCounts.get("Carol"), "Carol should have 1 defect");

        logger.info("Test passed: Developer defect counts are correct");
    }

    /**
     * Test: Multi-hop traversal - Given a defect, find all skills of developers working on it
     * Graph traversal: (Developer)-[:ASSIGNED_TO]->(Defect) AND (Developer)-[:HAS_SKILL]->(Skill)
     */
    @Test
    public void testMultiHopTraversal() {
        logger.info("Running test: Multi-hop traversal for defect skills");

        // defect1 is assigned to Alice, who has Java and Python skills
        List<String> skillsForDefect1 = repository.findSkillsOfDevelopersWorkingOnDefect("defect1");

        assertEquals(2, skillsForDefect1.size(), "Should find 2 skills for defect1");
        assertTrue(skillsForDefect1.contains("Java"));
        assertTrue(skillsForDefect1.contains("Python"));

        // defect5 is assigned to Bob, who has Java and Neo4j skills
        List<String> skillsForDefect5 = repository.findSkillsOfDevelopersWorkingOnDefect("defect5");

        assertEquals(2, skillsForDefect5.size(), "Should find 2 skills for defect5");
        assertTrue(skillsForDefect5.contains("Java"));
        assertTrue(skillsForDefect5.contains("Neo4j"));

        logger.info("Test passed: Multi-hop traversal found correct skills");
    }

    /**
     * Test: Multi-hop traversal - Find all unique skills within a team
     * Graph traversal: (Developer)-[:MEMBER_OF]->(Team) AND (Developer)-[:HAS_SKILL]->(Skill)
     */
    @Test
    public void testFindAllSkillsInTeam() {
        logger.info("Running test: Find all skills in team");

        // Backend Team has Alice (Java, Python) and Bob (Java, Neo4j)
        List<String> backendSkills = repository.findAllSkillsInTeam("Backend Team");

        assertEquals(3, backendSkills.size(), "Backend Team should have 3 unique skills");
        assertTrue(backendSkills.contains("Java"));
        assertTrue(backendSkills.contains("Neo4j"));
        assertTrue(backendSkills.contains("Python"));

        // Frontend Team has Carol (React) and Dave (React, Python)
        List<String> frontendSkills = repository.findAllSkillsInTeam("Frontend Team");

        assertEquals(2, frontendSkills.size(), "Frontend Team should have 2 unique skills");
        assertTrue(frontendSkills.contains("Python"));
        assertTrue(frontendSkills.contains("React"));

        logger.info("Test passed: Found all unique skills in each team");
    }
}
