package za.ac.tut.application.registration;

public class RegistrationRequest {

    private String role;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String rawPassword;
    private String phoneNumber;
    private String biography;
    private String clientType;
    private String attendeeInstitution;
    private String presenterInstitution;
    private String studentNumber;
    private String idPassportNumber;
    private String dateOfBirth;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRawPassword() {
        return rawPassword;
    }

    public void setRawPassword(String rawPassword) {
        this.rawPassword = rawPassword;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getAttendeeInstitution() {
        return attendeeInstitution;
    }

    public void setAttendeeInstitution(String attendeeInstitution) {
        this.attendeeInstitution = attendeeInstitution;
    }

    public String getPresenterInstitution() {
        return presenterInstitution;
    }

    public void setPresenterInstitution(String presenterInstitution) {
        this.presenterInstitution = presenterInstitution;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getIdPassportNumber() {
        return idPassportNumber;
    }

    public void setIdPassportNumber(String idPassportNumber) {
        this.idPassportNumber = idPassportNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
