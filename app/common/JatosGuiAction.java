package common;

import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Http;
import play.mvc.SimpleResult;
import exceptions.JatosGuiException;

/**
 * For all actions in a controller that is annotated with JatosGuiAction catch
 * {@link JatosGuiException} and return the SimpleResult stored in the exception (e.g.
 * display an error page).
 * 
 * @author Kristian Lange
 */
public class JatosGuiAction extends play.mvc.Action.Simple {

	public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
		Promise<SimpleResult> call;
		try {
			call = delegate.call(ctx);
		} catch (JatosGuiException e) {
			Logger.info("JatosGuiException: " + e.getMessage());
			SimpleResult result = e.getSimpleResult();
			call = Promise.<SimpleResult> pure(result);
		}
		return call;
	}

}
