package com.ph.ibm.validation.impl;

import static com.ph.ibm.util.ValidationUtils.isValueEmpty;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.ph.ibm.model.Employee;
import com.ph.ibm.opum.exception.InvalidCSVException;
import com.ph.ibm.repository.EmployeeRepository;
import com.ph.ibm.util.BluePagesUtils;
import com.ph.ibm.util.OpumConstants;
import com.ph.ibm.util.ValidationUtils;
import com.ph.ibm.validation.Validator;

/**
 * Class implementation for validation for admin employee
 *
 * @author <a HREF="teodorj@ph.ibm.com">Joemarie Teodoro</a>
 * @author <a HREF="dacanam@ph.ibm.com">Marjay Dacanay</a>
 */
public class AdminEmployeeValidator implements Validator<Employee> {

    private EmployeeRepository employeeRepository;

    private static Logger logger = Logger.getLogger( AdminEmployeeValidator.class );

    public AdminEmployeeValidator( EmployeeRepository employeeRepository ) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public boolean validate( Employee employee ) throws InvalidCSVException, SQLException {

        boolean isValid = !isEmployeeValueEmpty( employee ) && 
                        isValidEmployeeSerial( employee ) &&
                        isEmployeeIdExisting( employee ) &&
                        isValidEmployeeName( employee ) &&
                        isValidEmailAddress( employee ) &&
                        isEmployeeEmailExisting( employee ) && 
                        ValidationUtils.isValidDate( employee, employee.getRollInDate() ) &&
                        ValidationUtils.isValidDate( employee, employee.getRollOffDate() ) &&
                        ValidationUtils.isValidDateRange( employee, employee.getRollInDate(), employee.getRollOffDate() );
        return isValid;
    }

    /**
     * Checks for empty values in Serial, Name, Email, Rollin/off Date
     *
     * @param employee employee object
     * @return
     * @throws InvalidCSVException
     */
    protected boolean isEmployeeValueEmpty( Employee employee ) throws InvalidCSVException {
        if( isValueEmpty( employee.getEmployeeSerial() ) || isValueEmpty( employee.getFullName() ) ||
            isValueEmpty( employee.getIntranetId() ) || isValueEmpty( employee.getRollInDate() ) ||
            isValueEmpty( employee.getRollOffDate() ) ){
            logger.info( ValidationUtils.CAUSE_OF_ERROR + " " + OpumConstants.EMPTY_CSV_ERROR );
            throw new InvalidCSVException( employee, OpumConstants.EMPTY_CSV_ERROR );
        }
        return false;
    }

    /**
     * This method validate the format of employee email address
     *
     * @param employeeEmail
     * @return true if email address matches the regular expression pattern
     * @throws InvalidCSVException
     */
    protected boolean isValidEmailAddress( Employee employee ) throws InvalidCSVException {
        return ValidationUtils.regexValidator( employee, employee.getIntranetId(), ValidationUtils.VALID_EMAIL_REGEX,
            OpumConstants.INVALID_EMAIL_ADDRESS );
    }

    /**
     * This method validate the format of employee id
     *
     * @param employeeId
     * @return true if employee id matches the regular expression pattern
     * @throws InvalidCSVException
     */
    protected boolean isValidEmployeeSerial( Employee employee ) throws InvalidCSVException {
        return ValidationUtils.regexValidator( employee, employee.getEmployeeSerial(),
            ValidationUtils.VALID_SERIAL_REGEX, OpumConstants.INVALID_EMPLOYEE_ID );
    }

    /**
     * Validate if employee email exists in database
     *
     * @param employee
     * @return
     * @throws SQLException
     * @throws InvalidCSVException
     */
    protected boolean isEmployeeEmailExisting( Employee employee ) throws SQLException, InvalidCSVException {
          boolean isExisting = BluePagesUtils.isEmployeeIntranetFound( employee.getEmployeeSerial(), employee.getIntranetId()  );
        if( !isExisting ){
            logger.info( ValidationUtils.CAUSE_OF_ERROR + OpumConstants.EMPLOYEE_EMAIL_NOT_FOUND );
            throw new InvalidCSVException( employee, OpumConstants.EMPLOYEE_EMAIL_NOT_FOUND );
        }
        return true;
    }

    /**
     * Validate if employee id number exist in database
     *
     * @param employee
     * @return
     * @throws SQLException
     * @throws InvalidCSVException
     */
    protected boolean isEmployeeIdExisting( Employee employee ) throws SQLException, InvalidCSVException {
        boolean isExisting = BluePagesUtils.isEmployeeSerialFound( employee.getEmployeeSerial() );
        if( !isExisting ){
            logger.info( ValidationUtils.CAUSE_OF_ERROR + OpumConstants.EMPLOYEE_SERIAL_DOES_NOT_EXIST_IBM );
            throw new InvalidCSVException( employee, OpumConstants.EMPLOYEE_SERIAL_DOES_NOT_EXIST_IBM );
        }
        return isExisting;
    }

    /**
     * This method validate the format of employee name
     *
     * @param employeeName
     * @return true if name matches the regular expression pattern
     * @throws InvalidCSVException
     */
    protected boolean isValidEmployeeName( Employee employee ) throws InvalidCSVException {
        return ValidationUtils.regexValidator( employee, employee.getFullName(), ValidationUtils.VALID_NAME_REGEX,
            OpumConstants.INVALID_NAME );
    }


}