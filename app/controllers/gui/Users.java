package controllers.gui;

import java.util.List;

import models.StudyModel;
import models.UserModel;
import persistance.StudyDao;
import persistance.UserDao;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.With;
import services.gui.Breadcrumbs;
import services.gui.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import exceptions.gui.JatosGuiException;

/**
 * Controller with actions concerning users
 * 
 * @author Kristian Lange
 */
@With(JatosGuiAction.class)
@Singleton
public class Users extends Controller {

	private static final String CLASS_NAME = Users.class.getSimpleName();

	public static final String SESSION_EMAIL = "email";

	private final UserService userService;
	private final UserDao userDao;
	private final StudyDao studyDao;

	@Inject
	Users(UserDao userDao, UserService userService, StudyDao studyDao) {
		this.userDao = userDao;
		this.userService = userService;
		this.studyDao = studyDao;
	}

	/**
	 * Shows the profile view of a user
	 */
	@Transactional
	public Result profile(String email) throws JatosGuiException {
		Logger.info(CLASS_NAME + ".profile: " + "email " + email + ", "
				+ "logged-in user's email " + session(Users.SESSION_EMAIL));
		UserModel user = userService.retrieveUser(email);
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		userService.checkUserLoggedIn(user, loggedInUser);

		String breadcrumbs = Breadcrumbs.generateForUser(user);
		return ok(views.html.gui.user.profile.render(studyList, loggedInUser,
				breadcrumbs, user));
	}

	/**
	 * Shows a view with a form to create a new user.
	 */
	@Transactional
	public Result create() throws JatosGuiException {
		Logger.info(CLASS_NAME + ".create: " + "logged-in user's email "
				+ session(Users.SESSION_EMAIL));
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		String breadcrumbs = Breadcrumbs.generateForHome(Breadcrumbs.NEW_USER);
		return ok(views.html.gui.user.create.render(studyList, loggedInUser,
				breadcrumbs, Form.form(UserModel.class)));
	}

	/**
	 * Handles post request of user create form.
	 */
	@Transactional
	public Result submit() throws Exception {
		Logger.info(CLASS_NAME + ".submit: " + "logged-in user's email "
				+ session(Users.SESSION_EMAIL));
		Form<UserModel> form = Form.form(UserModel.class).bindFromRequest();
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());

		if (form.hasErrors()) {
			return showCreateUserAfterError(studyList, loggedInUser, form,
					null, Http.Status.BAD_REQUEST);
		}

		UserModel newUser = form.get();
		DynamicForm requestData = Form.form().bindFromRequest();
		String password = requestData.get(UserModel.PASSWORD);
		String passwordRepeat = requestData.get(UserModel.PASSWORD_REPEAT);
		List<ValidationError> errorList = userService.validateNewUser(newUser,
				password, passwordRepeat);
		if (!errorList.isEmpty()) {
			return showCreateUserAfterError(studyList, loggedInUser, form,
					errorList, Http.Status.BAD_REQUEST);
		}

		String passwordHash = userService.getHashMDFive(password);
		newUser.setPasswordHash(passwordHash);
		userDao.create(newUser);
		return redirect(controllers.gui.routes.Home.home());
	}

	private Result showCreateUserAfterError(List<StudyModel> studyList,
			UserModel loggedInUser, Form<UserModel> form,
			List<ValidationError> errorList, int httpStatus) {
		if (ControllerUtils.isAjax()) {
			return status(httpStatus);
		} else {
			if (errorList != null) {
				for (ValidationError error : errorList) {
					form.reject(error);
				}
			}
			String breadcrumbs = Breadcrumbs.generateForHome("New User");
			return status(httpStatus, views.html.gui.user.create.render(
					studyList, loggedInUser, breadcrumbs, form));
		}
	}

	private Result showEditUserAfterError(List<StudyModel> studyList,
			UserModel loggedInUser, Form<UserModel> form, UserModel user,
			int httpStatus) {
		if (ControllerUtils.isAjax()) {
			return status(httpStatus);
		} else {
			String breadcrumbs = Breadcrumbs.generateForUser(user,
					"Edit Profile");
			return status(httpStatus, views.html.gui.user.editProfile.render(
					studyList, loggedInUser, breadcrumbs, user, form));
		}
	}

	private Result showChangePasswordAfterError(List<StudyModel> studyList,
			UserModel loggedInUser, Form<UserModel> form,
			List<ValidationError> errorList, int httpStatus, UserModel user) {
		if (ControllerUtils.isAjax()) {
			return status(httpStatus);
		} else {
			if (errorList != null) {
				for (ValidationError error : errorList) {
					form.reject(error);
				}
			}
			String breadcrumbs = Breadcrumbs.generateForUser(user,
					"Change Password");
			return status(httpStatus,
					views.html.gui.user.changePassword.render(studyList,
							loggedInUser, breadcrumbs, form));
		}
	}

	/**
	 * Shows view with form to edit a user profile.
	 */
	@Transactional
	public Result editProfile(String email) throws JatosGuiException {
		Logger.info(CLASS_NAME + ".editProfile: " + "email " + email + ", "
				+ "logged-in user's email " + session(Users.SESSION_EMAIL));
		UserModel user = userService.retrieveUser(email);
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		userService.checkUserLoggedIn(user, loggedInUser);

		Form<UserModel> form = Form.form(UserModel.class).fill(user);
		String breadcrumbs = Breadcrumbs.generateForUser(user,
				Breadcrumbs.EDIT_PROFILE);
		return ok(views.html.gui.user.editProfile.render(studyList,
				loggedInUser, breadcrumbs, user, form));
	}

	/**
	 * Handles post request of user edit profile form.
	 */
	@Transactional
	public Result submitEditedProfile(String email) throws JatosGuiException {
		Logger.info(CLASS_NAME + ".submitEditedProfile: " + "email " + email
				+ ", " + "logged-in user's email "
				+ session(Users.SESSION_EMAIL));
		UserModel user = userService.retrieveUser(email);
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		userService.checkUserLoggedIn(user, loggedInUser);

		Form<UserModel> form = Form.form(UserModel.class).bindFromRequest();
		if (form.hasErrors()) {
			return showEditUserAfterError(studyList, loggedInUser, form, loggedInUser,
					Http.Status.BAD_REQUEST);
		}
		// Update user in database
		// Do not update 'email' since it's the ID and should stay
		// unaltered. For the password we have an extra form.
		DynamicForm requestData = Form.form().bindFromRequest();
		String name = requestData.get(UserModel.NAME);
		userDao.updateName(user, name);
		return redirect(controllers.gui.routes.Users.profile(email));
	}

	/**
	 * Shows view to change the password of a user.
	 */
	@Transactional
	public Result changePassword(String email) throws JatosGuiException {
		Logger.info(CLASS_NAME + ".changePassword: " + "email " + email + ", "
				+ "logged-in user's email " + session(Users.SESSION_EMAIL));
		UserModel user = userService.retrieveUser(email);
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		userService.checkUserLoggedIn(user, loggedInUser);

		Form<UserModel> form = Form.form(UserModel.class).fill(user);
		String breadcrumbs = Breadcrumbs.generateForUser(user,
				Breadcrumbs.CHANGE_PASSWORD);
		return ok(views.html.gui.user.changePassword.render(studyList,
				loggedInUser, breadcrumbs, form));
	}

	/**
	 * Handles post request of change password form.
	 */
	@Transactional
	public Result submitChangedPassword(String email) throws Exception {
		Logger.info(CLASS_NAME + ".submitChangedPassword: " + "email " + email
				+ ", " + "logged-in user's email "
				+ session(Users.SESSION_EMAIL));
		UserModel user = userService.retrieveUser(email);
		Form<UserModel> form = Form.form(UserModel.class).fill(user);
		UserModel loggedInUser = userService.retrieveLoggedInUser();
		List<StudyModel> studyList = studyDao.findAllByUser(loggedInUser
				.getEmail());
		userService.checkUserLoggedIn(user, loggedInUser);

		DynamicForm requestData = Form.form().bindFromRequest();
		String newPassword = requestData.get(UserModel.NEW_PASSWORD);
		String newPasswordRepeat = requestData.get(UserModel.PASSWORD_REPEAT);
		String oldPasswordHash = userService.getHashMDFive(requestData
				.get(UserModel.OLD_PASSWORD));
		List<ValidationError> errorList = userService.validateChangePassword(
				user, newPassword, newPasswordRepeat, oldPasswordHash);
		if (!errorList.isEmpty()) {
			return showChangePasswordAfterError(studyList, loggedInUser, form,
					errorList, Http.Status.BAD_REQUEST, loggedInUser);
		}
		// Update password hash in DB
		String newPasswordHash = userService.getHashMDFive(newPassword);
		user.setPasswordHash(newPasswordHash);
		userDao.update(user);
		return redirect(controllers.gui.routes.Users.profile(email));
	}

}