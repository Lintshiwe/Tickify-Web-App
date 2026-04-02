package za.ac.tut.application.presenter;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.TertiaryPresenterDAO;

public class TertiaryPresenterService implements BaseService<TertiaryPresenterDAO> {

	private final TertiaryPresenterDAO tertiaryPresenterDAO = new TertiaryPresenterDAO();

	@Override
	public TertiaryPresenterDAO repo() {
		return tertiaryPresenterDAO;
	}
}
