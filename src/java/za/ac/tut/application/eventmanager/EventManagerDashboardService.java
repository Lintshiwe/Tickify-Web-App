package za.ac.tut.application.eventmanager;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.EventManagerDashboardDAO;

public class EventManagerDashboardService implements BaseService<EventManagerDashboardDAO> {

	private final EventManagerDashboardDAO eventManagerDashboardDAO = new EventManagerDashboardDAO();

	@Override
	public EventManagerDashboardDAO repo() {
		return eventManagerDashboardDAO;
	}
}
