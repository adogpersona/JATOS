package publix.services.personal_single;

import publix.services.PublixErrorMessages;
import models.workers.PersonalSingleWorker;

import com.google.inject.Singleton;

/**
 * PersonalSinglePublix' implementation of PublixErrorMessages
 * 
 * @author Kristian Lange
 */
@Singleton
public class PersonalSingleErrorMessages extends
		PublixErrorMessages {

	@Override
	public String workerNotCorrectType(Long workerId) {
		return "The worker with ID " + workerId + " isn't a "
				+ PersonalSingleWorker.UI_WORKER_TYPE + " Worker.";
	}

}
