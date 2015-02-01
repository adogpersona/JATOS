package services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.StudyModel;
import models.results.StudyResult;
import models.workers.Worker;
import play.mvc.Controller;
import play.mvc.Http;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import exceptions.JatosGuiException;

/**
 * Utility class for all JATOS Controllers (not Publix).
 * 
 * @author Kristian Lange
 */
@Singleton
public class WorkerService extends Controller {

	private final JatosGuiExceptionThrower jatosGuiExceptionThrower;

	@Inject
	public WorkerService(JatosGuiExceptionThrower jatosGuiExceptionThrower) {
		this.jatosGuiExceptionThrower = jatosGuiExceptionThrower;
	}

	/**
	 * Throws a JatosGuiException in case the worker doesn't exist.
	 * Distinguishes between normal and Ajax request.
	 */
	public void checkWorker(Worker worker, Long workerId)
			throws JatosGuiException {
		if (worker == null) {
			String errorMsg = ErrorMessages.workerNotExist(workerId);
			jatosGuiExceptionThrower.throwHome(errorMsg,
					Http.Status.BAD_REQUEST);
		}
	}

	/**
	 * Retrieve all workersProvider that did this study.
	 */
	public Set<Worker> retrieveWorkers(StudyModel study) {
		List<StudyResult> studyResultList = StudyResult.findAllByStudy(study);
		Set<Worker> workerSet = new HashSet<>();
		for (StudyResult studyResult : studyResultList) {
			workerSet.add(studyResult.getWorker());
		}
		return workerSet;
	}

}
