package za.ac.tut.application.media;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.AdvertDAO;

public class AdvertService implements BaseService<AdvertDAO> {

	private final AdvertDAO advertDAO = new AdvertDAO();

	@Override
	public AdvertDAO repo() {
		return advertDAO;
	}
}
