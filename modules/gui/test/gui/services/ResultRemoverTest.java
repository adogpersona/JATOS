package gui.services;

import static org.fest.assertions.Assertions.assertThat;
import general.common.MessagesStrings;
import gui.AbstractTest;
import gui.GuiTestGlobal;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import models.common.ComponentResult;
import models.common.Study;
import models.common.StudyResult;
import models.common.User;

import org.fest.assertions.Fail;
import org.junit.Test;

import services.gui.ResultRemover;
import services.gui.ResultService;
import daos.common.StudyResultDao;
import exceptions.gui.BadRequestException;
import exceptions.gui.ForbiddenException;
import exceptions.gui.NotFoundException;

/**
 * Tests ResultRemover
 * 
 * @author Kristian Lange
 */
public class ResultRemoverTest extends AbstractTest {

	private ResultService resultService;
	private ResultRemover resultRemover;
	private StudyResultDao studyResultDao;

	@Override
	public void before() throws Exception {
		resultService = GuiTestGlobal.INJECTOR.getInstance(ResultService.class);
		resultRemover = GuiTestGlobal.INJECTOR.getInstance(ResultRemover.class);
		studyResultDao = GuiTestGlobal.INJECTOR.getInstance(StudyResultDao.class);
	}

	@Override
	public void after() throws Exception {
	}

	@Test
	public void simpleCheck() {
		int a = 1 + 1;
		assertThat(a).isEqualTo(2);
	}

	@Test
	public void checkRemoveComponentResults() throws NoSuchAlgorithmException,
			IOException, BadRequestException, NotFoundException,
			ForbiddenException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoComponentResults(study);

		// Check that we have 2 results
		List<Long> idList = resultService.extractResultIds("1, 2");
		List<ComponentResult> componentResultList = resultService
				.getComponentResults(idList);
		assertThat(componentResultList.size()).isEqualTo(2);

		// Now remove them
		entityManager.getTransaction().begin();
		resultRemover.removeComponentResults("1, 2", admin);
		entityManager.getTransaction().commit();

		// Check that the results are removed
		try {
			idList = resultService.extractResultIds("1, 2");
			componentResultList = resultService.getComponentResults(idList);
			Fail.fail();
		} catch (NotFoundException e) {
			assertThat(e.getMessage()).isEqualTo(
					MessagesStrings.componentResultNotExist(1l));
		}

		// Clean-up
		removeStudy(study);
	}

	private void createTwoComponentResults(Study study) {
		entityManager.getTransaction().begin();
		StudyResult studyResult = studyResultDao.create(study,
				admin.getWorker());
		// Have to set worker manually in test - don't know why
		studyResult.setWorker(admin.getWorker());
		// Have to set study manually in test - don't know why
		study.getFirstComponent().setStudy(study);
		// TODO
		// jatosPublixUtils.startComponent(study.getFirstComponent(),
		// studyResult);
		// jatosPublixUtils.startComponent(study.getFirstComponent(),
		// studyResult);
		entityManager.getTransaction().commit();
	}

	@Test
	public void checkRemoveComponentResultsNotFound()
			throws NoSuchAlgorithmException, IOException, BadRequestException,
			ForbiddenException, NotFoundException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoComponentResults(study);

		// Check that we have 2 results
		List<Long> idList = resultService.extractResultIds("1, 2");
		List<ComponentResult> componentResultList = resultService
				.getComponentResults(idList);
		assertThat(componentResultList.size()).isEqualTo(2);

		// Now try to remove the results 1 and 3 (3 doesn't exist)
		try {
			resultRemover.removeComponentResults("1, 3", admin);
			Fail.fail();
		} catch (NotFoundException e) {
			assertThat(e.getMessage()).isEqualTo(
					MessagesStrings.componentResultNotExist(3l));
		}

		// Check that NO result is removed - not even result 1
		idList = resultService.extractResultIds("1, 2");
		componentResultList = resultService.getComponentResults(idList);
		assertThat(componentResultList.size()).isEqualTo(2);

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkRemoveStudyResults() throws NoSuchAlgorithmException,
			IOException, BadRequestException, NotFoundException,
			ForbiddenException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		// Check that we have 2 results
		List<Long> idList = resultService.extractResultIds("1, 2");
		List<StudyResult> studyResultList = resultService
				.getStudyResults(idList);
		assertThat(studyResultList.size()).isEqualTo(2);

		// Now remove them
		entityManager.getTransaction().begin();
		resultRemover.removeStudyResults("1, 2", admin);
		entityManager.getTransaction().commit();

		studyResultList = studyResultDao.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(0);

		// Clean-up
		removeStudy(study);
	}

	private void createTwoStudyResults(Study study) {
		entityManager.getTransaction().begin();
		StudyResult studyResult1 = studyResultDao.create(study,
				admin.getWorker());
		// Have to set worker manually in test - don't know why
		studyResult1.setWorker(admin.getWorker());
		// TODO
		// ComponentResult componentResult11 = jatosPublixUtils.startComponent(
		// study.getFirstComponent(), studyResult1);
		// componentResult11
		// .setData("First ComponentResult's data of the first StudyResult.");
		// ComponentResult componentResult12 = jatosPublixUtils.startComponent(
		// study.getFirstComponent(), studyResult1);
		// componentResult12
		// .setData("Second ComponentResult's data of the first StudyResult.");

		StudyResult studyResult2 = studyResultDao.create(study,
				admin.getWorker());
		// Have to set worker manually in test - don't know why
		// TODO
		studyResult2.setWorker(admin.getWorker());
		// ComponentResult componentResult21 = jatosPublixUtils.startComponent(
		// study.getFirstComponent(), studyResult1);
		// componentResult21
		// .setData("First ComponentResult's data of the second StudyResult.");
		// ComponentResult componentResult22 = jatosPublixUtils.startComponent(
		// study.getFirstComponent(), studyResult1);
		// componentResult22
		// .setData("Second ComponentResult's data of the second StudyResult.");
		entityManager.getTransaction().commit();
	}

	@Test
	public void checkRemoveAllStudyResults() throws NoSuchAlgorithmException,
			IOException, ForbiddenException, BadRequestException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		// Check that we have 2 results
		List<StudyResult> studyResultList = studyResultDao
				.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(2);

		// Remove them
		entityManager.getTransaction().begin();
		resultRemover.removeAllStudyResults(study, admin);
		entityManager.getTransaction().commit();

		// Check that we have no more results
		studyResultList = studyResultDao.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(0);

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkRemoveAllStudyResultsWrongUser() throws IOException,
			NoSuchAlgorithmException, BadRequestException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		// Check that we have 2 results
		List<StudyResult> studyResultList = studyResultDao
				.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(2);

		// And now try to remove them with the wrong user
		User testUser = createAndPersistUser("bla@bla.com", "Bla", "bla");
		try {
			resultRemover.removeAllStudyResults(study, testUser);
			Fail.fail();
		} catch (ForbiddenException e) {
			assertThat(e.getMessage()).isEqualTo(
					MessagesStrings.studyNotUser(testUser.getName(),
							testUser.getEmail(), study.getId(),
							study.getTitle()));
		}

		// Check that we still have 2 results
		studyResultList = studyResultDao.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(2);

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkRemoveAllStudyResultsStudyLocked()
			throws BadRequestException, NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		// Check that we have 2 results
		List<StudyResult> studyResultList = studyResultDao
				.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(2);

		// Lock study
		entityManager.getTransaction().begin();
		study.setLocked(true);
		entityManager.getTransaction().commit();

		try {
			resultRemover.removeAllStudyResults(study, admin);
			Fail.fail();
		} catch (ForbiddenException e) {
			assertThat(e.getMessage()).isEqualTo(
					MessagesStrings.studyLocked(study.getId()));
		}

		// Check that we still have 2 results
		studyResultList = studyResultDao.findAllByStudy(study);
		assertThat(studyResultList.size()).isEqualTo(2);

		// Clean-up
		removeStudy(study);
	}

}
