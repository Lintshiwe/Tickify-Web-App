package za.ac.tut.application.media;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.EventDAO;

public class EventMediaService implements BaseService<EventDAO> {

	private final EventDAO eventDAO = new EventDAO();

	@Override
	public EventDAO repo() {
		return eventDAO;
	}
}
