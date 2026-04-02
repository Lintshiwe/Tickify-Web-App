package za.ac.tut.application.scanner;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import za.ac.tut.application.common.BaseService;
import za.ac.tut.databaseManagement.ScannerDAO;

public class ScannerService implements BaseService<ScannerDAO> {

	private final ScannerDAO scannerDAO = new ScannerDAO();

	@Override
	public ScannerDAO repo() {
		return scannerDAO;
	}

	public ScanResult validateAndLog(String code, int venueGuardId, boolean consumeOnSuccess) throws SQLException {
		ScannerDAO.ScanResult result = scannerDAO.validateAndLog(code, venueGuardId, consumeOnSuccess);
		return new ScanResult(result.isValid(), result.getMessage(), result.getTicketId(),
				result.getEventName(), result.getScannedAt(), result.getAuthenticityStatus());
	}

	public List<Map<String, Object>> getAttendeeListForGuardEvent(int guardId, int maxRows) throws SQLException {
		return scannerDAO.getAttendeeListForGuardEvent(guardId, maxRows);
	}

	public static class ScanResult {
		private final boolean valid;
		private final String message;
		private final Integer ticketId;
		private final String eventName;
		private final String scannedAt;
		private final String authenticityStatus;

		public ScanResult(boolean valid, String message, Integer ticketId,
				String eventName, String scannedAt, String authenticityStatus) {
			this.valid = valid;
			this.message = message;
			this.ticketId = ticketId;
			this.eventName = eventName;
			this.scannedAt = scannedAt;
			this.authenticityStatus = authenticityStatus;
		}

		public boolean isValid() {
			return valid;
		}

		public String getMessage() {
			return message;
		}

		public Integer getTicketId() {
			return ticketId;
		}

		public String getEventName() {
			return eventName;
		}

		public String getScannedAt() {
			return scannedAt;
		}

		public String getAuthenticityStatus() {
			return authenticityStatus;
		}
	}
}
