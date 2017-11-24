package com.ph.ibm.upload.upload.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.Message.RecipientType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import com.ph.ibm.bo.ResetPasswordBO;
import com.ph.ibm.model.Email;
import com.ph.ibm.model.Employee;
import com.ph.ibm.model.Role;
import com.ph.ibm.opum.exception.InvalidCSVException;
import com.ph.ibm.repository.EmployeeRepository;
import com.ph.ibm.repository.impl.EmployeeRepositoryImpl;
import com.ph.ibm.upload.Uploader;
import com.ph.ibm.util.OpumConstants;
import com.ph.ibm.util.UploaderUtils;
import com.ph.ibm.validation.Validator;
import com.ph.ibm.validation.impl.EmployeeValidator;

/**
 * Class implementation for uploading list of user administrator
 *
 * @author <a HREF="teodorj@ph.ibm.com">Joemarie Teodoro</a>
 * @author <a HREF="dacanam@ph.ibm.com">Marjay Dacanay</a>
 */
public class AdminListUploader implements Uploader {

    /**
     * EmployeeRepository is a Data Access Object which contain methods to add, register, login, view, validate field/s
     * stored in employee table - opum database
     */
    private EmployeeRepository employeeRepository = new EmployeeRepositoryImpl();

    /**
     * Validation contain methods to validate field such as employee name, employee id, project name, email address
     */
    private Validator<Employee> employeeValidator = new EmployeeValidator( employeeRepository );

    /**
     * Logger is used to document the execution of the system and logs the corresponding log level such as INFO, WARN,
     * ERROR
     */
    private Logger logger = Logger.getLogger( AdminListUploader.class );

    /**
     * This method is used when Super Administrator uploads the list of Admin Users
     *
     * @param rawData Data from the CSV file
     * @param uriInfo uri information
     * @return @throws Exception exception
     * @see com.ph.ibm.upload.Uploader#upload(java.lang.String, javax.ws.rs.core.UriInfo)
     */
    @Override
    public Response upload( String rawData, UriInfo uriInfo ) throws Exception {

        Map<String, List<String>> rows = UploaderUtils.populateList( rawData );
        if( rows.isEmpty() ){
            return UploaderUtils.invalidCsvResponseBuilder( uriInfo, null, OpumConstants.EMPTY_CSV );
        }
        List<Employee> validatedEmployee = new ArrayList<Employee>();
        String currentEmployeeID = null;
        List<String> recipientList = new ArrayList<String>();

        try{
            for( List<String> row : rows.values() ){
                Employee validateEmployee = new Employee();
                validateEmployee = validateEmployee( row );
                currentEmployeeID = validateEmployee.getEmployeeId();
                validatedEmployee.add( validateEmployee );
                recipientList.add( validateEmployee.getIntranetId() );
            }

            employeeRepository.saveOrUpdate( validatedEmployee, Role.SYS_ADMIN );
            logger.info( OpumConstants.SUCCESSFULLY_UPLOADED_FILE );
        }
        catch( InvalidCSVException e ){
            logger.error( e.getError() );
            return UploaderUtils.invalidCsvResponseBuilder( uriInfo, e.getObject(), e.getError() );
        }
        catch(

        SQLException e ){
            logger.error( "SQL Exception due to " + e.getMessage() );
            e.printStackTrace();
            return Response.status( 406 ).entity( OpumConstants.SQL_ERROR ).build();
        }

        logger.info( OpumConstants.SUCCESSFULLY_UPLOADED_FILE );
        sendEmailsToListOfRecepientsToChangePasswords( recipientList );
        logger.info( OpumConstants.SUCCESSFULLY_EMAILED_LIST_OF_EMAIL_ADDRESS );

        return Response.status( Status.OK ).entity( "CSV Uploaded Successfully!" ).build();
    }

    /**
     * Method to email list of addresses from the list uploaded by sys_admin/admin
     * 
     * @param lstRecipients list of recipients
     * @throws IOException exception
     */
    public void sendEmailsToListOfRecepientsToChangePasswords( List<String> lstRecipients ) throws IOException {
        ResetPasswordBO resetPasswordBO = new ResetPasswordBO();
        Email email = new Email();
        email.setRecipientAddresses( lstRecipients );
        email.setSenderAddress( "onlinepumsender@gmail.com" );
        email.setRecipientType( RecipientType.TO.toString() );
        email.setSubject( OpumConstants.EMAIL_SUBJECT );
        email.setText( OpumConstants.EMAIL_GREETING + "\n\n" + OpumConstants.EMAIL_BODY + "\n\n%s" );
        resetPasswordBO.emailResetPasswordLink( email );
    }

    /**
     * This method is used to validate uploaded list of Users/Employees
     *
     * @param uriInfo uri information
     * @param row represents row in csv file
     * @return Employee employee object
     * @throws Exception exception
     */
    private Employee validateEmployee( List<String> row )
        throws InvalidCSVException, SQLException, Exception {
        if( row == null || row.isEmpty() ){
            throw new InvalidCSVException( null, OpumConstants.INVALID_CSV );
        }
        checkRowIntegrity( row );
        Employee employee = null;
        employee = new Employee();
        employee.setEmployeeSerial( row.get( 0 ) );
        employee.setFullName( row.get( 1 ) );
        employee.setIntranetId( row.get( 2 ) );
        employee.setRollInDate( row.get( 3 ) );
        employee.setRollOffDate( row.get( 4 ) );
        employeeValidator.validate( employee );
        return employee;
    }

    /**
     * Checks basic row validation like row item must not be empty.
     * 
     * @param row
     * @param employee
     * @return boolean
     * @throws InvalidCSVException when row value is not valid
     */
    private void checkRowIntegrity( List<String> row ) throws InvalidCSVException {
        if( row.isEmpty() || row.size() != 5 || row.get( 0 ).isEmpty() || row.get( 1 ).isEmpty() ||
            row.get( 2 ).isEmpty() || row.get( 3 ).isEmpty() || row.get( 4 ).isEmpty() ){
            throw new InvalidCSVException( null, "CSV contents should not be empty." );
        }
    }

}
