package edu.fau.recsys.grading;

/** Registration with a username that already exists. */
public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("The username '" + username + "' is already taken.");
	}
}
