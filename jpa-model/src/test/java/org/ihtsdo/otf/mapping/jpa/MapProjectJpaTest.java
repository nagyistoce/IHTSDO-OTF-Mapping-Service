package org.ihtsdo.otf.mapping.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.CacheMode;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * The Class MapProjectJpaTest.
 * 
 * Provides test cases
 * 1. confirm MapProject data load returns expected data
 * 2. confirms indexed fields are indexed
 * 3. confirms MapProject is audited and changes are logged in audit table
 */
public class MapProjectJpaTest {

	/** The manager. */
	private static EntityManager manager;

	/** The factory. */
	private static EntityManagerFactory factory;

	/** The full text entity manager. */
	private static FullTextEntityManager fullTextEntityManager;
	
	/** The audit history reader. */
	private static AuditReader reader;
	
	private static MapProjectJpa mapProject;

	/** The test ref set id. */
	private static Long testRefSetId = new Long("123456789");

	/** The test name. */
	private static String testName = "SNOMEDCT to ICD10CM Mapping";

	/** The updated test name. */
	private static String testName2 = "Updated SNOMEDCT to ICD10CM Mapping";
	
	/** The test source terminology. */
	private static String testSourceTerminology = "SNOMEDCT";

	/** The test source terminology version. */
	private static String testSourceTerminologyVersion = "20130731";

	/** The test destination terminology. */
	private static String testDestinationTerminology = "ICD10CM";

	/** The test destination terminology version. */
	private static String testDestinationTerminologyVersion = "2010";

	/** The test block structure. */
	private static boolean testBlockStructure = true;

	/** The test group structure. */
	private static boolean testGroupStructure = false;

	/** The test published. */
	private static boolean testPublished = false;

	/**
	 * Creates db tables, load test objects and create indexes to prepare for test
	 * cases.
	 */
	@BeforeClass
	public static void init() {

		// create Entitymanager
		factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		fullTextEntityManager = Search.getFullTextEntityManager(manager);

		// load test objects
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			loadMapProjects();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to load mapProject records.");
		}

		// create indexes
		try {
			FullTextEntityManager fullTextEntityManager =
					Search.getFullTextEntityManager(manager);
			fullTextEntityManager.purgeAll(ConceptJpa.class);
			fullTextEntityManager.flushToIndexes();
			fullTextEntityManager.createIndexer(ConceptJpa.class)
					.batchSizeToLoadObjects(25).cacheMode(CacheMode.NORMAL)
					.threadsToLoadObjects(5).threadsForSubsequentFetching(20)
					.startAndWait();
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Indexing failed.");
		}
		
		// create audit reader for history records
		reader = AuditReaderFactory.get( manager );
	}

	/**
	 * Test map project load.
	 */
	@Test
	public void testMapProjectLoad() {

		EntityTransaction tx = manager.getTransaction();
		try {
			System.out.println("Testing load of MapProjects...");

			tx.begin();
			confirmLoad();
			tx.commit();

		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to confirm load of mapProject records.");
		}

	}

	/**
	 * Load map projects.
	 * 
	 * @throws Exception the exception
	 */
	private static void loadMapProjects() throws Exception {

		mapProject = new MapProjectJpa();

		mapProject.setName(testName);
		mapProject.setRefSetId(testRefSetId);
		mapProject.setSourceTerminology(testSourceTerminology);
		mapProject.setSourceTerminologyVersion(testSourceTerminologyVersion);
		mapProject.setDestinationTerminology(testDestinationTerminology);
		mapProject
				.setDestinationTerminologyVersion(testDestinationTerminologyVersion);
		mapProject.setBlockStructure(testBlockStructure);
		mapProject.setGroupStructure(testGroupStructure);
		mapProject.setPublished(testPublished);
		manager.persist(mapProject);

		MapLeadJpa mapLeadBrian = new MapLeadJpa();
		mapLeadBrian.setName("Brian");
		mapLeadBrian.setUserName("bcarlsen");
		mapLeadBrian.setEmail("bcarlsen@westcoastinformatics.com");
		manager.persist(mapLeadBrian);
		mapProject.addMapLead(mapLeadBrian);

		MapLeadJpa mapLeadRory = new MapLeadJpa();
		mapLeadRory.setName("Rory");
		mapLeadRory.setUserName("rda");
		mapLeadRory.setEmail("rda@ihtsdo.org");
		manager.persist(mapLeadRory);
		mapProject.addMapLead(mapLeadRory);

		MapSpecialistJpa mapSpecialistDeborah = new MapSpecialistJpa();
		mapSpecialistDeborah.setName("Deborah");
		mapSpecialistDeborah.setUserName("dshapiro");
		mapSpecialistDeborah.setEmail("dshapiro@westcoastinformatics.com");
		manager.persist(mapSpecialistDeborah);
		mapProject.addMapSpecialist(mapSpecialistDeborah);

		

	}

	/**
	 * Confirm load.
	 */
	private void confirmLoad() {
		javax.persistence.Query query =
				manager
						.createQuery("select m from MapProjectJpa m where refSetId = :refSetId");

		// Try to retrieve the single expected result
		// If zero or more than one result are returned, log error and set result to
		// null

		try {
			query.setParameter("refSetId", testRefSetId);

			MapProject mapProject = (MapProject) query.getSingleResult();
			assertEquals(mapProject.getRefSetId(), testRefSetId);

		} catch (NoResultException e) {
			fail("Concept query for refSetId = " + testRefSetId
					+ " returned no results!");
		} catch (NonUniqueResultException e) {
			fail("Concept query for refSetId = " + testRefSetId
					+ " returned multiple results!");
		}
	}

	/**
	 * Test map project indexes.
	 */
	@Test
	public void testMapProjectIndex() {

		System.out.println("Testing indexes...");

		try {
			SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

			QueryParser queryParser =
					new QueryParser(Version.LUCENE_36, "summary",
							searchFactory.getAnalyzer(ConceptJpa.class));

			// test index on refSetId
			Query luceneQuery = queryParser.parse("refSetId:" + testRefSetId);
			FullTextQuery fullTextQuery =
					fullTextEntityManager.createFullTextQuery(luceneQuery);
			List<MapProject> results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getName(), testName);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

			// test index on name
			luceneQuery = queryParser.parse("name:" + testName);
			fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
			results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getRefSetId(), testRefSetId);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

			// test index on source terminology version
			luceneQuery =
					queryParser.parse("sourceTerminologyVersion:"
							+ testSourceTerminologyVersion);
			fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
			results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getRefSetId(), testRefSetId);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

			// test index on destination terminology version
			luceneQuery =
					queryParser.parse("destinationTerminologyVersion:"
							+ testDestinationTerminologyVersion);
			fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
			results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getRefSetId(), testRefSetId);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

			// test index on destination terminology
			luceneQuery =
					queryParser.parse("destinationTerminology:"
							+ testDestinationTerminology);
			fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
			results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getRefSetId(), testRefSetId);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

			// test index on source terminology
			luceneQuery =
					queryParser.parse("sourceTerminology:" + testSourceTerminology);
			fullTextQuery = fullTextEntityManager.createFullTextQuery(luceneQuery);
			results = fullTextQuery.getResultList();
			for (MapProject mapProject : results) {
				assertEquals(mapProject.getRefSetId(), testRefSetId);
			}
			assertTrue("results.size() " + results.size(), results.size() > 0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test map project audit reader history.
	 */
	@Test
	public void testMapProjectAuditReader() {
		// report initial number of revisions on MapProject object
		List<Number> revNumbers = reader.getRevisions(MapProjectJpa.class, 1L);
		assertTrue(revNumbers.size() == 1);
		System.out.println("MapProject: " + 1L + " - Versions: " + revNumbers.toString());
		
		// make a change to MapProject
		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			MapSpecialistJpa mapSpecialistPatrick = new MapSpecialistJpa();
			mapSpecialistPatrick.setName("Patrick");
			mapSpecialistPatrick.setUserName("pgranvold");
			mapSpecialistPatrick.setEmail("pgranvold@westcoastinformatics.com");
			manager.persist(mapSpecialistPatrick);
			mapProject.setName(testName2);
			manager.persist(mapProject);
			mapProject.addMapSpecialist(mapSpecialistPatrick);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to modify mapProject records.");
		}
		
		// report incremented number of revisions on MapProject object
		revNumbers = reader.getRevisions(MapProjectJpa.class, 1L);
		assertTrue(revNumbers.size() == 2);
		System.out.println("MapProject: " + 1L + " - Versions: " + revNumbers.toString());
	
		// revert change to MapProject
		tx = manager.getTransaction();
		try {
			tx.begin();			
			mapProject.setName(testName);
			manager.persist(mapProject);
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to revert mapProject records.");
		}
	}

	/**
	 * Clean up.
	 */
	@AfterClass
	public static void cleanUp() {	
		EntityTransaction tx = manager.getTransaction();
	
		// truncate tables
		try {
			tx.begin();	
			javax.persistence.Query query = manager.createNativeQuery("DELETE FROM map_projects_map_advices");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_advices");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_leads");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_leads");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_specialists");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_advices_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_advices_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_leads_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_leads_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_map_specialists_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_specialists_aud");
			query.executeUpdate();
			query = manager.createNativeQuery("DELETE FROM map_projects_aud");
			query.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			fail("Failure to detach mapProject.");
		}
		manager.close();
		factory.close();
	}

}