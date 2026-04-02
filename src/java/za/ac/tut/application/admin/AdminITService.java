package za.ac.tut.application.admin;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.AdminITDAO;

public class AdminITService implements BaseService<AdminITDAO> {

	private final AdminITDAO adminITDAO = new AdminITDAO();

	@Override
	public AdminITDAO repo() {
		return adminITDAO;
	}
}
