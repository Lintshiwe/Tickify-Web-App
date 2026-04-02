package za.ac.tut.application.user;

import java.sql.SQLException;
import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.UserDAO;

public class UserAccountService implements BaseService<UserDAO> {

	private final UserDAO userDAO = new UserDAO();

	@Override
	public UserDAO repo() {
		return userDAO;
	}

	public boolean isClientRole(String role) {
		return userDAO.isClientRole(role);
	}

	public ClientAccount findClientByIdentifier(String role, String identifier) throws SQLException {
		UserDAO.ClientAccount account = userDAO.findClientByIdentifier(role, identifier);
		return account == null ? null : ClientAccount.from(account);
	}

	public ClientAccount findClientByRoleAndId(String role, int userId) throws SQLException {
		UserDAO.ClientAccount account = userDAO.findClientByRoleAndId(role, userId);
		return account == null ? null : ClientAccount.from(account);
	}

	public boolean updateClientPassword(String role, int userId, String newPasswordHash) throws SQLException {
		return userDAO.updateClientPassword(role, userId, newPasswordHash);
	}

	public boolean isClientEmailVerified(String role, int userId) throws SQLException {
		return userDAO.isClientEmailVerified(role, userId);
	}

	public boolean verifyClientEmailAddress(String role, int userId, String expectedEmail) throws SQLException {
		return userDAO.verifyClientEmailAddress(role, userId, expectedEmail);
	}

	public boolean isUniqueConstraintViolation(SQLException ex) {
		return userDAO.isUniqueConstraintViolation(ex);
	}

	public static class ClientAccount {
		private final String role;
		private final int userId;
		private final String email;
		private final String passwordHash;

		private ClientAccount(String role, int userId, String email, String passwordHash) {
			this.role = role;
			this.userId = userId;
			this.email = email;
			this.passwordHash = passwordHash;
		}

		public static ClientAccount from(UserDAO.ClientAccount account) {
			return new ClientAccount(account.getRole(), account.getUserId(), account.getEmail(), account.getPasswordHash());
		}

		public String getRole() {
			return role;
		}

		public int getUserId() {
			return userId;
		}

		public String getEmail() {
			return email;
		}

		public String getPasswordHash() {
			return passwordHash;
		}
	}
}
