package za.ac.tut.application.attendee;

import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.AttendeeDAO;

public class AttendeeService implements BaseService<AttendeeDAO> {

	private final AttendeeDAO attendeeDAO = new AttendeeDAO();

	@Override
	public AttendeeDAO repo() {
		return attendeeDAO;
	}
}
