# Neo4j Graph Database Learning Guide

This repository demonstrates Neo4j graph database concepts through a practical example of managing developers, defects, skills, and teams.

## Technologies Used
- **Java 21**
- **Neo4j Database** (local instance)
- **Neo4j Java Driver** (bolt protocol)
- **JUnit 5** for testing
- **Maven** for build management

## Project Overview

A simple graph database application that models software development team dynamics:
- Developers belong to Teams
- Developers have Skills
- Defects are assigned to Developers

This creates a rich graph structure perfect for learning complex queries and traversals.

---

## Graph Data Model

### Nodes (Entities)
- **Developer**: `{id, name, teamId}`
- **Defect**: `{id, title, severity, status}`
- **Skill**: `{id, name, level}`
- **Team**: `{id, name, location}`

### Relationships
```
(Developer)-[:HAS_SKILL]->(Skill)
(Developer)-[:ASSIGNED_TO]->(Defect)
(Developer)-[:MEMBER_OF]->(Team)
```

### Visual Representation
```
     ┌──────┐
     │ Team │
     └──────┘
        ↑
        │ MEMBER_OF
        │
   ┌──────────┐     HAS_SKILL     ┌───────┐
   │Developer │──────────────────→│ Skill │
   └──────────┘                    └───────┘
        │
        │ ASSIGNED_TO
        ↓
   ┌────────┐
   │ Defect │
   └────────┘
```

---

## Neo4j Learning Examples

### 1. Creating Nodes (Basic)

**Concept**: Creating individual nodes with properties

```cypher
// Create a Developer node
CREATE (d:Developer {id: 'dev1', name: 'Alice', teamId: 'team1'})

// Create a Skill node
CREATE (s:Skill {id: 'skill1', name: 'Java', level: 'EXPERT'})
```

**Java Example**: See `GraphRepository.java:32` - `createDeveloper()`

---

### 2. Creating Relationships (Basic)

**Concept**: Connecting nodes with typed relationships

```cypher
// Connect Alice to Java skill
MATCH (d:Developer {id: 'dev1'})
MATCH (s:Skill {id: 'skill1'})
CREATE (d)-[:HAS_SKILL]->(s)
```

**Java Example**: See `GraphRepository.java:83` - `assignSkillToDeveloper()`

**Key Learning**:
- Relationships are directional and typed
- Use MATCH to find existing nodes before creating relationships

---

### 3. Simple Pattern Matching (Intermediate)

**Concept**: Finding nodes by traversing relationships

```cypher
// Find all developers in Backend Team
MATCH (d:Developer)-[:MEMBER_OF]->(t:Team {name: 'Backend Team'})
RETURN d.name
```

**Java Example**: See `GraphRepository.java:124` - `findDevelopersByTeam()`

**Test**: `GraphTraversalTest.java:109` - `testFindDevelopersByTeam()`

**Key Learning**:
- MATCH finds patterns in the graph
- Arrow syntax `->` shows relationship direction
- Filter with `{property: value}` syntax

---

### 4. Reverse Pattern Matching (Intermediate)

**Concept**: Traversing relationships in reverse

```cypher
// Find all defects assigned to Alice
MATCH (d:Developer {name: 'Alice'})-[:ASSIGNED_TO]->(def:Defect)
RETURN def.title, def.severity
```

**Java Example**: See `GraphRepository.java:149` - `findDefectsAssignedToDeveloper()`

**Test**: `GraphTraversalTest.java:132` - `testFindDefectsForDeveloper()`

---

### 5. Aggregation (Intermediate)

**Concept**: Counting and grouping results

```cypher
// Count defects per developer and rank them
MATCH (d:Developer)-[:ASSIGNED_TO]->(def:Defect)
RETURN d.name AS developerName, count(def) AS defectCount
ORDER BY defectCount DESC
```

**Java Example**: See `GraphRepository.java:175` - `findDevelopersWithMostDefects()`

**Test**: `GraphTraversalTest.java:155` - `testFindDevelopersWithMostDefects()`

**Key Learning**:
- `count()` aggregates results
- `ORDER BY` sorts results
- `AS` creates column aliases

---

### 6. Multi-Hop Traversal (Advanced)

**Concept**: Following multiple relationship types in one query

```cypher
// Given a defect, find all skills of developers working on it
MATCH (d:Developer)-[:ASSIGNED_TO]->(def:Defect {id: 'defect1'})
MATCH (d)-[:HAS_SKILL]->(s:Skill)
RETURN DISTINCT s.name
ORDER BY s.name
```

**Java Example**: See `GraphRepository.java:199` - `findSkillsOfDevelopersWorkingOnDefect()`

**Test**: `GraphTraversalTest.java:173` - `testMultiHopTraversal()`

**Key Learning**:
- Multiple MATCH clauses traverse different paths
- Reuse variables (d) to connect patterns
- `DISTINCT` removes duplicates

---

### 7. Complex Multi-Hop with Team (Advanced)

**Concept**: Chaining multiple relationships

```cypher
// Find all unique skills in a team
MATCH (d:Developer)-[:MEMBER_OF]->(t:Team {name: 'Backend Team'})
MATCH (d)-[:HAS_SKILL]->(s:Skill)
RETURN DISTINCT s.name
ORDER BY s.name
```

**Java Example**: See `GraphRepository.java:221` - `findAllSkillsInTeam()`

**Test**: `GraphTraversalTest.java:198` - `testFindAllSkillsInTeam()`

**Graph Path**:
```
Team ← MEMBER_OF ← Developer → HAS_SKILL → Skill
```

---

### 8. Filtering & Recommendations (Advanced)

**Concept**: Complex filtering with negative patterns and aggregation

```cypher
// Recommend developers for a defect based on required skills
// Exclude developers already assigned
MATCH (def:Defect {id: 'defect1'})
MATCH (d:Developer)-[:HAS_SKILL]->(s:Skill)
WHERE s.name IN ['Java', 'Python']
  AND NOT (d)-[:ASSIGNED_TO]->(def)
WITH d, count(DISTINCT s) AS matchingSkills
OPTIONAL MATCH (d)-[:ASSIGNED_TO]->(otherDefects:Defect)
WITH d.name AS developerName,
     matchingSkills,
     count(otherDefects) AS currentWorkload
RETURN developerName, matchingSkills, currentWorkload
ORDER BY matchingSkills DESC, currentWorkload ASC
```

**Java Example**: See `GraphRepository.java:251` - `recommendDevelopersForDefect()`

**Test**: `GraphTraversalTest.java:226` - `testSkillBasedRecommendation()`

**Key Learning**:
- `NOT (pattern)` - negative pattern matching (exclusion)
- `WITH` - intermediate result transformation
- `OPTIONAL MATCH` - like SQL LEFT JOIN (won't exclude if no matches)
- `IN` - list membership check
- Multiple ORDER BY criteria

**Real-World Use Case**:
Given a defect requiring Java and Neo4j skills, find developers who:
1. Have at least one of those skills
2. Are NOT already working on this defect
3. Ranked by: most matching skills first, then least busy

---

## Running the Examples

### Prerequisites
```bash
# Ensure Neo4j is running locally
# Default: bolt://localhost:7687
```

### Update Configuration
Edit `src/main/resources/application.properties` with your Neo4j credentials:
```properties
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=your_password
```

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test
```bash
mvn test -Dtest=GraphTraversalTest#testSkillBasedRecommendation
```

---

## Neo4j Concepts Demonstrated

| Concept | Example in Code | Test Location |
|---------|----------------|---------------|
| Node Creation | `createDeveloper()` | Setup in all tests |
| Relationship Creation | `assignSkillToDeveloper()` | Setup in all tests |
| Pattern Matching | `findDevelopersByTeam()` | `testFindDevelopersByTeam()` |
| Aggregation | `findDevelopersWithMostDefects()` | `testFindDevelopersWithMostDefects()` |
| Multi-hop Traversal | `findSkillsOfDevelopersWorkingOnDefect()` | `testMultiHopTraversal()` |
| Negative Patterns | `recommendDevelopersForDefect()` | `testSkillBasedRecommendation()` |
| Optional Matching | `recommendDevelopersWithDetails()` | `testDetailedSkillBasedRecommendation()` |

---

## Project Structure
```
src/
├── main/java/com/graphdb/
│   ├── model/              # POJOs (Developer, Defect, Skill, Team)
│   ├── connection/         # Neo4j driver connection management
│   └── repository/         # All Cypher queries and graph operations
├── main/resources/
│   └── application.properties
└── test/java/com/graphdb/
    └── GraphTraversalTest.java  # All test scenarios
```

---

## Sample Test Data

Each test creates this dataset:

**Teams**:
- Backend Team (San Francisco) - Alice, Bob
- Frontend Team (New York) - Carol, Dave

**Developers & Skills**:
- Alice: Java, Python (3 defects)
- Bob: Java, Neo4j (1 defect)
- Carol: React (1 defect)
- Dave: React, Python (0 defects)

**Defects**:
- Login API fails (HIGH) → Alice
- Database timeout (CRITICAL) → Alice
- UI button misaligned (LOW) → Carol
- Performance issue (MEDIUM) → Alice
- Memory leak (HIGH) → Bob

---

## Key Takeaways

1. **Graph databases excel at relationships** - Queries that would require complex JOINs in SQL are natural in Neo4j
2. **Pattern matching is intuitive** - The ASCII-art syntax `(a)-[:REL]->(b)` mirrors how you think about connections
3. **Multi-hop is efficient** - Traversing relationships is O(1) per hop due to index-free adjacency
4. **Cypher is declarative** - Describe WHAT pattern you want, not HOW to find it
5. **Real-world modeling** - This example shows how teams, skills, and work assignments naturally form a graph

---

## Learning Path

1. Start with basic tests: `testFindDevelopersByTeam()`
2. Progress to aggregation: `testFindDevelopersWithMostDefects()`
3. Explore multi-hop: `testMultiHopTraversal()`
4. Master advanced: `testSkillBasedRecommendation()`

Each test is self-contained and demonstrates specific Neo4j capabilities.

---

## Further Exploration Ideas

Add these queries to deepen your learning:
- Shortest path between two developers
- Skill gap analysis (skills needed but missing)
- Defect clustering by required skills
- Developer collaboration networks
- Team expertise heatmaps

See `GraphRepository.java` for implementation patterns.

